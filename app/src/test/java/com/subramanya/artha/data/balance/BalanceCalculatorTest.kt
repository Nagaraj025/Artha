package com.subramanya.artha.data.balance

import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceCalculatorTest {

    private val accountA = "acct-A"
    private val accountB = "acct-B"
    private val cardC = "card-C"

    // 1) Opening balance only, no transactions.
    @Test fun `account balance with no transactions equals opening balance`() {
        val balance = BalanceCalculator.computeAccountBalance(
            openingBalance = 50_000.0,
            accountId = accountA,
            transactions = emptyList(),
        )
        assertEquals(50_000.0, balance, EPS)
    }

    // 2) Expense reduces account balance.
    @Test fun `expense from account reduces balance`() {
        val txns = listOf(expense(accountA, 1_000.0))
        val balance = BalanceCalculator.computeAccountBalance(50_000.0, accountA, txns)
        assertEquals(49_000.0, balance, EPS)
    }

    // 3) Income increases account balance.
    @Test fun `income into account increases balance`() {
        val txns = listOf(income(accountA, 5_000.0))
        val balance = BalanceCalculator.computeAccountBalance(50_000.0, accountA, txns)
        assertEquals(55_000.0, balance, EPS)
    }

    // 4) Transfer-out: source account decreases.
    @Test fun `transfer out of account decreases source balance`() {
        val txns = listOf(transfer(from = accountA, to = accountB, amount = 1_000.0))
        val balance = BalanceCalculator.computeAccountBalance(50_000.0, accountA, txns)
        assertEquals(49_000.0, balance, EPS)
    }

    // 5) Transfer-in: destination account increases.
    @Test fun `transfer into account increases destination balance`() {
        val txns = listOf(transfer(from = accountA, to = accountB, amount = 1_000.0))
        val balance = BalanceCalculator.computeAccountBalance(0.0, accountB, txns)
        assertEquals(1_000.0, balance, EPS)
    }

    // 6) Cumulative across many transactions, mixed direction.
    @Test fun `cumulative balance across mixed transactions matches expected total`() {
        val txns = listOf(
            income(accountA, 5_000.0),
            expense(accountA, 200.0),
            expense(accountA, 100.0),
            transfer(from = accountA, to = accountB, amount = 1_500.0),
            refund(accountA, 50.0),
            // unrelated transaction on accountB — must not affect accountA
            expense(accountB, 800.0),
        )
        val balance = BalanceCalculator.computeAccountBalance(10_000.0, accountA, txns)
        // 10000 + 5000 - 200 - 100 - 1500 + 50 = 13250
        assertEquals(13_250.0, balance, EPS)
    }

    // 7) Credit card: expenses raise outstanding, CARD_PAYMENT to it lowers outstanding.
    @Test fun `card expenses raise outstanding and card_payment lowers it`() {
        val txns = listOf(
            cardExpense(cardC, 5_000.0),
            cardExpense(cardC, 2_000.0),
            // pay 3000 from accountA toward card C
            cardPayment(fromAccount = accountA, toCard = cardC, amount = 3_000.0),
        )
        val outstanding = BalanceCalculator.computeCardOutstanding(cardC, txns)
        // 5000 + 2000 - 3000 = 4000
        assertEquals(4_000.0, outstanding, EPS)

        // and the paying account drops by 3000 (no other txns touch it)
        val accountBalance = BalanceCalculator.computeAccountBalance(50_000.0, accountA, txns)
        assertEquals(47_000.0, accountBalance, EPS)
    }

    // 8) Refund and cashback on a card both reduce outstanding.
    @Test fun `refund and cashback on card reduce outstanding`() {
        val txns = listOf(
            cardExpense(cardC, 1_000.0),
            txn(type = TransactionType.REFUND, sourceKind = SourceKind.CARD, sourceId = cardC, amount = 200.0),
            txn(type = TransactionType.CASHBACK, sourceKind = SourceKind.CARD, sourceId = cardC, amount = 50.0),
        )
        val outstanding = BalanceCalculator.computeCardOutstanding(cardC, txns)
        // 1000 - 200 - 50 = 750
        assertEquals(750.0, outstanding, EPS)
    }

    // 9) Direct income posted onto a card reduces outstanding (the user's refund-as-income case).
    //    Purchase ₹601 on the card, then ₹401 comes back posted as INCOME directly to the card
    //    (not as a REFUND) — outstanding must drop to ₹200, not stay at ₹601.
    @Test fun `direct income posted to a card reduces outstanding`() {
        val txns = listOf(
            cardExpense(cardC, 601.0),
            txn(type = TransactionType.INCOME, sourceKind = SourceKind.CARD, sourceId = cardC, amount = 401.0),
        )
        val outstanding = BalanceCalculator.computeCardOutstanding(cardC, txns)
        assertEquals(200.0, outstanding, EPS)
    }

    // 10) Every "money in" type posted onto a card reduces its outstanding, symmetric with how
    //     those types increase an account balance.
    @Test fun `all money-in types posted to a card reduce outstanding`() {
        val moneyIn = listOf(
            TransactionType.INCOME,
            TransactionType.REFUND,
            TransactionType.CASHBACK,
            TransactionType.INTEREST,
            TransactionType.LOAN_RECEIVED,
            TransactionType.GIFT_RECEIVED,
            TransactionType.INVESTMENT_SELL,
        )
        moneyIn.forEach { type ->
            val txns = listOf(
                cardExpense(cardC, 1_000.0),
                txn(type = type, sourceKind = SourceKind.CARD, sourceId = cardC, amount = 300.0),
            )
            val outstanding = BalanceCalculator.computeCardOutstanding(cardC, txns)
            assertEquals("$type on a card should reduce outstanding", 700.0, outstanding, EPS)
        }
    }

    // ---------- helpers ----------

    private fun expense(accountId: String, amount: Double): TransactionEntity =
        txn(TransactionType.EXPENSE, SourceKind.ACCOUNT, accountId, amount)

    private fun income(accountId: String, amount: Double): TransactionEntity =
        txn(TransactionType.INCOME, SourceKind.ACCOUNT, accountId, amount)

    private fun refund(accountId: String, amount: Double): TransactionEntity =
        txn(TransactionType.REFUND, SourceKind.ACCOUNT, accountId, amount)

    private fun cardExpense(cardId: String, amount: Double): TransactionEntity =
        txn(TransactionType.EXPENSE, SourceKind.CARD, cardId, amount)

    private fun transfer(from: String, to: String, amount: Double): TransactionEntity = txn(
        type = TransactionType.TRANSFER,
        sourceKind = SourceKind.ACCOUNT,
        sourceId = from,
        amount = amount,
        destinationKind = SourceKind.ACCOUNT,
        destinationId = to,
    )

    private fun cardPayment(fromAccount: String, toCard: String, amount: Double): TransactionEntity = txn(
        type = TransactionType.CARD_PAYMENT,
        sourceKind = SourceKind.ACCOUNT,
        sourceId = fromAccount,
        amount = amount,
        destinationKind = SourceKind.CARD,
        destinationId = toCard,
    )

    private fun txn(
        type: TransactionType,
        sourceKind: SourceKind,
        sourceId: String,
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
