package com.subramanya.artha.data.entity.enums

import org.junit.Assert.assertEquals
import org.junit.Test

class ValuationModeTest {
    @Test fun `deposit types default to DERIVED`() {
        listOf(
            InvestmentType.FD,
            InvestmentType.RD,
            InvestmentType.PPF,
            InvestmentType.EPF,
            InvestmentType.BONDS,
        ).forEach {
            assertEquals("$it should be DERIVED", ValuationMode.DERIVED, it.defaultValuationMode())
        }
    }

    @Test fun `market types default to MARKET`() {
        listOf(
            InvestmentType.SIP,
            InvestmentType.MUTUAL_FUND,
            InvestmentType.EQUITY,
            InvestmentType.GOLD_PHYSICAL,
            InvestmentType.GOLD_DIGITAL,
            InvestmentType.NPS,
            InvestmentType.ULIP,
            InvestmentType.OTHER,
        ).forEach {
            assertEquals("$it should be MARKET", ValuationMode.MARKET, it.defaultValuationMode())
        }
    }
}
