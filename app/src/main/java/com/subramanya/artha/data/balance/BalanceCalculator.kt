package com.subramanya.artha.data.balance

import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.data.entity.enums.ValuationMode

/**
 * Pure-Kotlin balance derivation. Account balances and credit-card outstandings are
 * NEVER stored — they are recomputed from the transaction log whenever the inputs change.
 *
 * Direction rules (mirror the source/destination convention documented on
 * TransactionEntity):
 *   - source = the affected account/card on the user's side.
 *   - destination = second affected side, only used for TRANSFER and CARD_PAYMENT in Phase 1.
 *   - The TransactionType decides whether money flows in or out of source/destination.
 *
 * For accounts:
 *   - EXPENSE, LOAN_GIVEN, GIFT_SENT, INVESTMENT_BUY        → source loses money
 *   - TRANSFER, CARD_PAYMENT                                 → source loses, destination gains
 *   - INCOME, REFUND, CASHBACK, INTEREST,
 *     LOAN_RECEIVED, GIFT_RECEIVED, INVESTMENT_SELL          → source gains money
 *   - ADJUSTMENT                                             → signed by amount (negative allowed)
 *
 * For credit cards (outstanding = how much the user owes):
 *   - EXPENSE, LOAN_GIVEN, GIFT_SENT charged TO the card    → outstanding increases
 *   - any "money in" type ON the card (INCOME, REFUND,
 *     CASHBACK, INTEREST, LOAN_RECEIVED, GIFT_RECEIVED,
 *     INVESTMENT_SELL)                                       → outstanding decreases
 *   - CARD_PAYMENT where destination = card                 → outstanding decreases (bill paid)
 *   - ADJUSTMENT                                             → signed by amount
 *
 * The credit set is the same MONEY_INTO_SOURCE used for accounts, so e.g. a refund posted
 * as a plain INCOME directly onto the card still pays it down (symmetric with accounts).
 */
object BalanceCalculator {

    private val MONEY_OUT_OF_SOURCE: Set<TransactionType> =
        setOf(
            TransactionType.EXPENSE,
            TransactionType.LOAN_GIVEN,
            TransactionType.GIFT_SENT,
            TransactionType.INVESTMENT_BUY,
            TransactionType.TRANSFER,
            TransactionType.CARD_PAYMENT,
        )

    private val MONEY_INTO_SOURCE: Set<TransactionType> =
        setOf(
            TransactionType.INCOME,
            TransactionType.REFUND,
            TransactionType.CASHBACK,
            TransactionType.INTEREST,
            TransactionType.LOAN_RECEIVED,
            TransactionType.GIFT_RECEIVED,
            TransactionType.INVESTMENT_SELL,
        )

    private val MONEY_INTO_DESTINATION: Set<TransactionType> =
        setOf(
            TransactionType.TRANSFER,
            TransactionType.CARD_PAYMENT,
        )

    /** Card charges: increase outstanding when applied as `source = CARD`. */
    private val CARD_CHARGES: Set<TransactionType> =
        setOf(
            TransactionType.EXPENSE,
            TransactionType.LOAN_GIVEN,
            TransactionType.GIFT_SENT,
        )

    // Card credits (outstanding decreases) reuse MONEY_INTO_SOURCE — any money landing on the
    // card pays it down, mirroring how those same types add to an account balance.

    /** invested principal + posted interest for one investment, returned together. */
    data class InvestmentTotals(val invested: Double, val interest: Double)

    /**
     * Balances for MANY accounts in a SINGLE pass over [transactions].
     *
     * Equivalent to calling [computeAccountBalance] once per account (pinned by
     * BalanceBatchTest) but O(transactions + accounts) instead of O(accounts × transactions).
     * This is the hot path: every account balance is recomputed whenever the transaction log
     * changes, so the live flows use this batch form to avoid re-scanning the whole log per
     * account. [openingBalanceById] supplies each account's opening balance and also defines
     * which accounts to compute (transactions referencing other ids are ignored).
     */
    fun computeAccountBalances(openingBalanceById: Map<String, Double>, transactions: List<TransactionEntity>): Map<String, Double> {
        val balances = HashMap(openingBalanceById)
        for (txn in transactions) {
            if (txn.sourceType == SourceKind.ACCOUNT) {
                val id = txn.sourceId
                val current = if (id != null) balances[id] else null
                if (id != null && current != null) {
                    balances[id] = when (txn.type) {
                        in MONEY_OUT_OF_SOURCE -> current - txn.amount
                        in MONEY_INTO_SOURCE -> current + txn.amount
                        TransactionType.ADJUSTMENT -> current + txn.amount
                        else -> current
                    }
                }
            }
            if (txn.destinationType == SourceKind.ACCOUNT && txn.type in MONEY_INTO_DESTINATION) {
                val id = txn.destinationId
                val current = if (id != null) balances[id] else null
                if (id != null && current != null) {
                    balances[id] = current + txn.amount
                }
            }
        }
        return balances
    }

    /**
     * Outstanding for MANY cards in a SINGLE pass. Equivalent to [computeCardOutstanding] per
     * card (pinned by BalanceBatchTest), O(transactions + cards). [cardIds] defines which cards
     * to compute; each starts at 0.0.
     */
    fun computeCardOutstandings(cardIds: Collection<String>, transactions: List<TransactionEntity>): Map<String, Double> {
        val outstanding = HashMap<String, Double>(cardIds.size)
        for (id in cardIds) outstanding[id] = 0.0
        for (txn in transactions) {
            if (txn.sourceType == SourceKind.CARD) {
                val id = txn.sourceId
                val current = if (id != null) outstanding[id] else null
                if (id != null && current != null) {
                    outstanding[id] = when (txn.type) {
                        in CARD_CHARGES -> current + txn.amount
                        in MONEY_INTO_SOURCE -> current - txn.amount
                        TransactionType.ADJUSTMENT -> current + txn.amount
                        else -> current
                    }
                }
            }
            if (txn.destinationType == SourceKind.CARD && txn.type == TransactionType.CARD_PAYMENT) {
                val id = txn.destinationId
                val current = if (id != null) outstanding[id] else null
                if (id != null && current != null) {
                    outstanding[id] = current - txn.amount
                }
            }
        }
        return outstanding
    }

    /**
     * invested (opening + buys − sells) and posted interest for MANY investments in a SINGLE
     * pass. Equivalent to [computeInvestmentInvested] + [computeInvestmentInterest] per
     * investment (pinned by BalanceBatchTest). [openingContributionById] supplies each
     * investment's opening principal and defines which investments to compute.
     */
    fun computeInvestmentTotals(
        openingContributionById: Map<String, Double>,
        transactions: List<TransactionEntity>,
    ): Map<String, InvestmentTotals> {
        val invested = HashMap(openingContributionById)
        val interest = HashMap<String, Double>(openingContributionById.size)
        for (id in openingContributionById.keys) interest[id] = 0.0
        for (txn in transactions) {
            when (txn.type) {
                TransactionType.INVESTMENT_BUY -> {
                    val id = txn.destinationId
                    val current = if (id != null && txn.destinationType == SourceKind.INVESTMENT) invested[id] else null
                    if (id != null && current != null) invested[id] = current + txn.amount
                }
                TransactionType.INVESTMENT_SELL -> {
                    val id = txn.sourceId
                    val current = if (id != null && txn.sourceType == SourceKind.INVESTMENT) invested[id] else null
                    if (id != null && current != null) invested[id] = current - txn.amount
                }
                TransactionType.INTEREST -> {
                    val id = txn.destinationId
                    val current = if (id != null && txn.destinationType == SourceKind.INVESTMENT) interest[id] else null
                    if (id != null && current != null) interest[id] = current + txn.amount
                }
                else -> Unit
            }
        }
        return openingContributionById.keys.associateWith { id ->
            InvestmentTotals(invested = invested.getValue(id), interest = interest.getValue(id))
        }
    }

    fun computeAccountBalance(openingBalance: Double, accountId: String, transactions: List<TransactionEntity>): Double {
        var balance = openingBalance
        for (txn in transactions) {
            // money leaving this account (source side)
            if (txn.sourceType == SourceKind.ACCOUNT && txn.sourceId == accountId) {
                when (txn.type) {
                    in MONEY_OUT_OF_SOURCE -> balance -= txn.amount
                    in MONEY_INTO_SOURCE -> balance += txn.amount
                    TransactionType.ADJUSTMENT -> balance += txn.amount
                    else -> Unit
                }
            }
            // money arriving in this account (destination side: transfer/card_payment)
            if (txn.destinationType == SourceKind.ACCOUNT && txn.destinationId == accountId) {
                if (txn.type in MONEY_INTO_DESTINATION) {
                    balance += txn.amount
                }
            }
        }
        return balance
    }

    /**
     * Sum of money the user has put into a specific investment. Computed from the
     * transaction log so the user never has to maintain a running total manually.
     *
     * Rules:
     *   - `openingContribution` seeds the running total (principal already in the
     *     investment before the first logged transaction — e.g. a migrated balance).
     *   - INVESTMENT_BUY with destination = this investment → adds to invested
     *   - INVESTMENT_SELL with source = this investment       → subtracts from invested
     *
     * Note: posted INTEREST is NOT counted here (it is growth, not contributed
     * principal) — see [computeInvestmentInterest].
     *
     * Returns can be negative if the user has sold more than they bought (rare;
     * indicates partial profit-taking past the original principal).
     */
    fun computeInvestmentInvested(investmentId: String, transactions: List<TransactionEntity>, openingContribution: Double = 0.0): Double {
        var invested = openingContribution
        for (txn in transactions) {
            when (txn.type) {
                TransactionType.INVESTMENT_BUY -> {
                    if (txn.destinationType == SourceKind.INVESTMENT && txn.destinationId == investmentId) {
                        invested += txn.amount
                    }
                }
                TransactionType.INVESTMENT_SELL -> {
                    if (txn.sourceType == SourceKind.INVESTMENT && txn.sourceId == investmentId) {
                        invested -= txn.amount
                    }
                }
                else -> Unit
            }
        }
        return invested
    }

    /** Interest credited INTO this investment (compounding deposits). */
    fun computeInvestmentInterest(investmentId: String, transactions: List<TransactionEntity>): Double {
        var interest = 0.0
        for (txn in transactions) {
            if (txn.type == TransactionType.INTEREST &&
                txn.destinationType == SourceKind.INVESTMENT &&
                txn.destinationId == investmentId
            ) {
                interest += txn.amount
            }
        }
        return interest
    }

    /**
     * Displayed value of an investment, per its valuation mode. Pass all params
     * regardless of mode; each mode reads only the subset it needs:
     *   - MARKET  → returns the manually-entered [currentValue] (the others are ignored).
     *   - DERIVED → contributions (opening + buys − sells) + posted interest
     *               (ignores [currentValue]).
     */
    fun computeInvestmentValue(
        mode: ValuationMode,
        currentValue: Double,
        openingContribution: Double,
        investmentId: String,
        transactions: List<TransactionEntity>,
    ): Double = when (mode) {
        ValuationMode.MARKET -> currentValue
        ValuationMode.DERIVED ->
            computeInvestmentInvested(investmentId, transactions, openingContribution) +
                computeInvestmentInterest(investmentId, transactions)
    }

    fun computeCardOutstanding(cardId: String, transactions: List<TransactionEntity>): Double {
        var outstanding = 0.0
        for (txn in transactions) {
            // charges and credits applied directly to the card (source side)
            if (txn.sourceType == SourceKind.CARD && txn.sourceId == cardId) {
                when (txn.type) {
                    in CARD_CHARGES -> outstanding += txn.amount
                    in MONEY_INTO_SOURCE -> outstanding -= txn.amount
                    TransactionType.ADJUSTMENT -> outstanding += txn.amount
                    else -> Unit
                }
            }
            // bill payment: destination = this card → outstanding decreases
            if (txn.destinationType == SourceKind.CARD &&
                txn.destinationId == cardId &&
                txn.type == TransactionType.CARD_PAYMENT
            ) {
                outstanding -= txn.amount
            }
        }
        return outstanding
    }
}
