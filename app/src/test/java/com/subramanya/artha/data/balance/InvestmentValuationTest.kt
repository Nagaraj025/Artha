package com.subramanya.artha.data.balance

import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.data.entity.enums.ValuationMode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the per-type investment valuation logic added in the investment-valuation
 * redesign: an opening contribution seed on `computeInvestmentInvested`, summed
 * INTEREST credits, and the per-mode `computeInvestmentValue` (DERIVED vs MARKET).
 */
class InvestmentValuationTest {

    private val investmentX = "inv-X"
    private val accountA = "acct-A"

    // invested now includes openingContribution
    @Test fun `invested includes opening contribution`() {
        val txns = listOf(buy(accountA, investmentX, 5_000.0))
        assertEquals(
            65_000.0,
            BalanceCalculator.computeInvestmentInvested(investmentX, txns, openingContribution = 60_000.0),
            EPS,
        )
    }

    @Test fun `interest credited to the investment is summed`() {
        val txns = listOf(interest(to = investmentX, amount = 400.0))
        assertEquals(400.0, BalanceCalculator.computeInvestmentInterest(investmentX, txns), EPS)
    }

    @Test fun `derived value is opening plus contributions plus interest`() {
        val txns = listOf(buy(accountA, investmentX, 5_000.0), interest(investmentX, 400.0))
        val v = BalanceCalculator.computeInvestmentValue(
            mode = ValuationMode.DERIVED,
            currentValue = 0.0,
            openingContribution = 60_000.0,
            investmentId = investmentX,
            transactions = txns,
        )
        assertEquals(65_400.0, v, EPS)
    }

    @Test fun `market value is the manual current value regardless of contributions`() {
        val txns = listOf(buy(accountA, investmentX, 5_000.0))
        val v = BalanceCalculator.computeInvestmentValue(
            mode = ValuationMode.MARKET,
            currentValue = 90_000.0,
            openingContribution = 0.0,
            investmentId = investmentX,
            transactions = txns,
        )
        assertEquals(90_000.0, v, EPS)
    }

    @Test fun `interest does not count as invested`() {
        val txns = listOf(buy(accountA, investmentX, 5_000.0), interest(investmentX, 400.0))
        assertEquals(5_000.0, BalanceCalculator.computeInvestmentInvested(investmentX, txns), EPS)
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

    /** An INTEREST credit posted INTO an investment (source = EXTERNAL, the bank/scheme). */
    private fun interest(to: String, amount: Double): TransactionEntity = txn(
        type = TransactionType.INTEREST,
        sourceKind = SourceKind.EXTERNAL,
        sourceId = "external",
        amount = amount,
        destinationKind = SourceKind.INVESTMENT,
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
