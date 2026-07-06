package com.subramanya.artha.data.balance

import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthlyAggregatorTest {

    @Test fun `empty input returns zero totals`() {
        val result = MonthlyAggregator.aggregate(emptyList())
        assertEquals(0.0, result.income, EPS)
        assertEquals(0.0, result.expense, EPS)
    }

    @Test fun `expenses sum into expense total`() {
        val txns = listOf(txn(TransactionType.EXPENSE, 100.0), txn(TransactionType.EXPENSE, 250.50))
        val result = MonthlyAggregator.aggregate(txns)
        assertEquals(350.50, result.expense, EPS)
        assertEquals(0.0, result.income, EPS)
    }

    @Test fun `income and refunds sum into income total`() {
        val txns = listOf(
            txn(TransactionType.INCOME, 50_000.0),
            txn(TransactionType.REFUND, 200.0),
            txn(TransactionType.CASHBACK, 30.0),
            txn(TransactionType.INTEREST, 12.5),
        )
        val result = MonthlyAggregator.aggregate(txns)
        assertEquals(50_242.50, result.income, EPS)
        assertEquals(0.0, result.expense, EPS)
    }

    @Test fun `transfers and card payments are excluded from both totals`() {
        val txns = listOf(
            txn(TransactionType.TRANSFER, 1_000.0),
            txn(TransactionType.CARD_PAYMENT, 5_000.0),
            txn(TransactionType.ADJUSTMENT, 7.0),
        )
        val result = MonthlyAggregator.aggregate(txns)
        assertEquals(0.0, result.income, EPS)
        assertEquals(0.0, result.expense, EPS)
    }

    @Test fun `mixed bag produces correct income and expense buckets`() {
        val txns = listOf(
            txn(TransactionType.INCOME, 60_000.0),
            txn(TransactionType.EXPENSE, 200.0),
            txn(TransactionType.EXPENSE, 50.0),
            txn(TransactionType.LOAN_GIVEN, 1_000.0),
            txn(TransactionType.GIFT_RECEIVED, 500.0),
            txn(TransactionType.TRANSFER, 10_000.0), // excluded
        )
        val result = MonthlyAggregator.aggregate(txns)
        assertEquals(60_500.0, result.income, EPS)
        assertEquals(1_250.0, result.expense, EPS)
    }

    @Test fun `transactions flagged excluded-from-expense are skipped`() {
        val txns = listOf(
            txn(TransactionType.EXPENSE, 100.0),
            txn(TransactionType.EXPENSE, 5_000.0, excluded = true), // a rule excluded this one
            txn(TransactionType.INCOME, 200.0),
        )
        val result = MonthlyAggregator.aggregate(txns)
        assertEquals(100.0, result.expense, EPS) // the 5,000 excluded
        assertEquals(200.0, result.income, EPS)
    }

    private fun txn(type: TransactionType, amount: Double, excluded: Boolean = false): TransactionEntity = TransactionEntity(
        id = "txn-${idSeq++}",
        type = type,
        amount = amount,
        currency = "INR",
        date = 0L,
        description = "test",
        categoryId = null,
        subCategoryId = null,
        sourceType = SourceKind.ACCOUNT,
        sourceId = "acct",
        destinationType = null,
        destinationId = null,
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
        excludedFromExpenseTotal = excluded,
    )

    private companion object {
        private const val EPS: Double = 1e-9
        private var idSeq: Int = 0
    }
}
