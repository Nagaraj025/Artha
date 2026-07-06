package com.subramanya.artha.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class IndianNumberFormatTest {

    @Test fun `whole rupees hide the cents tail`() {
        assertEquals("₹100", IndianNumberFormat.format(100.0))
        assertEquals("₹1,000", IndianNumberFormat.format(1_000.0))
    }

    @Test fun `lakh transitions to two-two-three grouping`() {
        assertEquals("₹1,00,000", IndianNumberFormat.format(1_00_000.0))
        assertEquals("₹10,00,000", IndianNumberFormat.format(10_00_000.0))
        assertEquals("₹1,00,00,000", IndianNumberFormat.format(1_00_00_000.0))
    }

    @Test fun `non-whole amounts show two decimals`() {
        assertEquals("₹12,34,567.50", IndianNumberFormat.format(12_34_567.5))
        assertEquals("₹1,234.99", IndianNumberFormat.format(1234.99))
        assertEquals("₹54.25", IndianNumberFormat.format(54.25))
    }

    @Test fun `negative amounts use an en-dash outside the symbol (HANDOFF §6_5)`() {
        // Use an explicit unicode literal so this stays robust against editors
        // that try to "auto-fix" en-dashes back to hyphens.
        val enDash = "–"
        assertEquals("$enDash₹1,234", IndianNumberFormat.format(-1234.0))
        assertEquals("$enDash₹50.25", IndianNumberFormat.format(-50.25))
    }

    @Test fun `formatWithDecimals always shows two decimal places`() {
        assertEquals("₹100.00", IndianNumberFormat.formatWithDecimals(100.0))
        assertEquals("₹1,000.00", IndianNumberFormat.formatWithDecimals(1_000.0))
    }
}
