package com.subramanya.artha.data.balance

import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The batch balance functions must produce EXACTLY the same numbers as the per-entity
 * functions — they exist only to compute every entity in a single pass over the
 * transaction log (O(txns + entities)) instead of one full scan per entity
 * (O(entities × txns)). These tests pin that equivalence so the optimisation can never
 * silently drift from the proven per-entity math.
 */
class BalanceBatchTest {

    private val acctA = "acct-A"
    private val acctB = "acct-B"
    private val cardC = "card-C"
    private val cardD = "card-D"
    private val invX = "inv-X"
    private val invY = "inv-Y"

    private val mixedTxns: List<TransactionEntity> = listOf(
        txn(TransactionType.INCOME, SourceKind.ACCOUNT, acctA, 5_000.0),
        txn(TransactionType.EXPENSE, SourceKind.ACCOUNT, acctA, 200.0),
        txn(TransactionType.ADJUSTMENT, SourceKind.ACCOUNT, acctA, -50.0),
        // transfer A -> B (A loses, B gains)
        txn(TransactionType.TRANSFER, SourceKind.ACCOUNT, acctA, 1_000.0, SourceKind.ACCOUNT, acctB),
        // card spend + a direct income credit onto the card + a bill payment from A
        txn(TransactionType.EXPENSE, SourceKind.CARD, cardC, 601.0),
        txn(TransactionType.INCOME, SourceKind.CARD, cardC, 401.0),
        txn(TransactionType.CARD_PAYMENT, SourceKind.ACCOUNT, acctA, 200.0, SourceKind.CARD, cardC),
        txn(TransactionType.CASHBACK, SourceKind.CARD, cardD, 30.0),
        // investments: buys, a sell, and interest
        txn(TransactionType.INVESTMENT_BUY, SourceKind.ACCOUNT, acctA, 5_000.0, SourceKind.INVESTMENT, invX),
        txn(TransactionType.INVESTMENT_SELL, SourceKind.INVESTMENT, invX, 1_000.0),
        txn(TransactionType.INTEREST, SourceKind.EXTERNAL, null, 400.0, SourceKind.INVESTMENT, invX),
        txn(TransactionType.INVESTMENT_BUY, SourceKind.ACCOUNT, acctB, 2_000.0, SourceKind.INVESTMENT, invY),
        // an unrelated transaction on an account we don't track — must be ignored
        txn(TransactionType.EXPENSE, SourceKind.ACCOUNT, "acct-untracked", 999.0),
    )

    @Test fun `computeAccountBalances matches per-account computeAccountBalance`() {
        val opening = mapOf(acctA to 10_000.0, acctB to 0.0)
        val batch = BalanceCalculator.computeAccountBalances(opening, mixedTxns)
        opening.forEach { (id, open) ->
            assertEquals(
                "account $id",
                BalanceCalculator.computeAccountBalance(open, id, mixedTxns),
                batch.getValue(id),
                EPS,
            )
        }
    }

    @Test fun `computeCardOutstandings matches per-card computeCardOutstanding`() {
        val ids = listOf(cardC, cardD)
        val batch = BalanceCalculator.computeCardOutstandings(ids, mixedTxns)
        ids.forEach { id ->
            assertEquals(
                "card $id",
                BalanceCalculator.computeCardOutstanding(id, mixedTxns),
                batch.getValue(id),
                EPS,
            )
        }
    }

    @Test fun `computeInvestmentTotals matches per-investment invested and interest`() {
        val opening = mapOf(invX to 60_000.0, invY to 0.0)
        val batch = BalanceCalculator.computeInvestmentTotals(opening, mixedTxns)
        opening.forEach { (id, open) ->
            assertEquals(
                "invested $id",
                BalanceCalculator.computeInvestmentInvested(id, mixedTxns, open),
                batch.getValue(id).invested,
                EPS,
            )
            assertEquals(
                "interest $id",
                BalanceCalculator.computeInvestmentInterest(id, mixedTxns),
                batch.getValue(id).interest,
                EPS,
            )
        }
    }

    // ---------- helpers ----------

    private fun txn(
        type: TransactionType,
        sourceKind: SourceKind,
        sourceId: String?,
        amount: Double,
        destinationKind: SourceKind? = null,
        destinationId: String? = null,
    ): TransactionEntity = TransactionEntity(
        id = "txn-${idSeq++}",
        type = type,
        amount = amount,
        currency = "INR",
        date = 0L,
        description = "test",
        categoryId = null,
        subCategoryId = null,
        sourceType = sourceKind,
        sourceId = sourceId,
        destinationType = destinationKind,
        destinationId = destinationId,
        paymentApp = PaymentApp.OTHER,
        place = null,
        latitude = null,
        longitude = null,
        receiptUri = null,
        notes = null,
        taxSection = null,
        recurringRuleId = null,
        isSplit = false,
        splitGroupId = null,
        source = TransactionSource.MANUAL,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private companion object {
        private const val EPS: Double = 1e-9
        private var idSeq: Int = 0
    }
}
