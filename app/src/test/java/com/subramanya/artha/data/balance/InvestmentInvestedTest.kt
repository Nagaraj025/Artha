package com.subramanya.artha.data.balance

import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class InvestmentInvestedTest {

    private val investmentX = "inv-X"
    private val investmentY = "inv-Y"
    private val accountA = "acct-A"

    @Test fun `no transactions yields zero invested`() {
        val result = BalanceCalculator.computeInvestmentInvested(investmentX, emptyList())
        assertEquals(0.0, result, EPS)
    }

    @Test fun `single INVESTMENT_BUY to this investment is fully counted`() {
        val txns = listOf(buy(from = accountA, to = investmentX, amount = 10_000.0))
        val result = BalanceCalculator.computeInvestmentInvested(investmentX, txns)
        assertEquals(10_000.0, result, EPS)
    }

    @Test fun `INVESTMENT_SELL from this investment reduces invested`() {
        val txns = listOf(
            buy(from = accountA, to = investmentX, amount = 10_000.0),
            sell(from = investmentX, to = accountA, amount = 3_000.0),
        )
        val result = BalanceCalculator.computeInvestmentInvested(investmentX, txns)
        assertEquals(7_000.0, result, EPS)
    }

    @Test fun `unrelated investment is ignored`() {
        val txns = listOf(
            buy(from = accountA, to = investmentX, amount = 10_000.0),
            buy(from = accountA, to = investmentY, amount = 5_000.0),
            sell(from = investmentY, to = accountA, amount = 1_000.0),
        )
        // investment X only saw a single 10k buy
        assertEquals(10_000.0, BalanceCalculator.computeInvestmentInvested(investmentX, txns), EPS)
        // investment Y: 5000 in, 1000 out
        assertEquals(4_000.0, BalanceCalculator.computeInvestmentInvested(investmentY, txns), EPS)
    }

    @Test fun `cumulative SIP-style buys sum correctly`() {
        val txns = (1..12).map { month ->
            buy(from = accountA, to = investmentX, amount = 5_000.0, idx = month)
        }
        val result = BalanceCalculator.computeInvestmentInvested(investmentX, txns)
        assertEquals(60_000.0, result, EPS)
    }

    @Test fun `expense or transfer on the source account does not affect investment invested`() {
        val txns = listOf(
            buy(from = accountA, to = investmentX, amount = 10_000.0),
            // ordinary spending unrelated to investments
            txn(TransactionType.EXPENSE, SourceKind.ACCOUNT, accountA, 500.0),
            txn(
                TransactionType.TRANSFER,
                SourceKind.ACCOUNT,
                accountA,
                1_000.0,
                destinationKind = SourceKind.ACCOUNT,
                destinationId = "acct-other",
            ),
        )
        val result = BalanceCalculator.computeInvestmentInvested(investmentX, txns)
        assertEquals(10_000.0, result, EPS)
    }

    // ---------- helpers ----------

    private fun buy(from: String, to: String, amount: Double, idx: Int? = null): TransactionEntity = txn(
        type = TransactionType.INVESTMENT_BUY,
        sourceKind = SourceKind.ACCOUNT,
        sourceId = from,
        amount = amount,
        destinationKind = SourceKind.INVESTMENT,
        destinationId = to,
        idOverride = idx?.let { "txn-buy-$it" },
    )

    private fun sell(from: String, to: String, amount: Double): TransactionEntity = txn(
        type = TransactionType.INVESTMENT_SELL,
        sourceKind = SourceKind.INVESTMENT,
        sourceId = from,
        amount = amount,
        destinationKind = SourceKind.ACCOUNT,
        destinationId = to,
    )

    private fun txn(
        type: TransactionType,
        sourceKind: SourceKind,
        sourceId: String,
        amount: Double,
        destinationKind: SourceKind? = null,
        destinationId: String? = null,
        idOverride: String? = null,
    ): TransactionEntity = TransactionEntity(
        id = idOverride ?: "txn-${idSeq++}",
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
