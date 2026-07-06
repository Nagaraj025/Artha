package com.subramanya.artha.sms

import com.subramanya.artha.domain.model.SmsDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BankSmsParserTest {

    @Test
    fun `parses a standard debit alert`() {
        val body = "Rs.500.00 debited from A/c XX1234 on 03-07-26 at SWIGGY. Avl Bal Rs.10,000.00"
        val result = BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(SmsDirection.DEBIT, result.direction)
        assertEquals(500.0, result.amount, 0.001)
        assertEquals("1234", result.accountHint)
    }

    @Test
    fun `parses a standard credit alert`() {
        val body = "INR 25,000.00 credited to your A/c XX5678 on 03-07-26. Info: SALARY"
        val result = BankSmsParser.parse("SBIINB", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(SmsDirection.CREDIT, result.direction)
        assertEquals(25000.0, result.amount, 0.001)
    }

    @Test
    fun `ignores an OTP message even if it mentions a debit`() {
        val body = "Your OTP for a debit card transaction of Rs.500 is 123456. Do not share it."
        assertNull(BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L))
    }

    @Test
    fun `ignores a promotional message`() {
        val body = "Get a cashback offer of Rs.100 on your next credit card spend! T&C apply."
        assertNull(BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L))
    }

    @Test
    fun `returns null when no amount can be parsed`() {
        val body = "Your account was debited for a transaction. Contact support for details."
        assertNull(BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L))
    }
}
