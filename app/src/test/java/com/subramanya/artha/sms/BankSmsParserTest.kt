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

    @Test
    fun `merchant name stops at the sentence boundary, not the whole trailing clause`() {
        val body = "Rs.500.00 debited from A/c XX1234 on 03-07-26 at SWIGGY. Avl Bal Rs.10,000.00"
        val result = BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals("SWIGGY", result.merchant)
    }

    @Test
    fun `picks the transaction amount over an earlier balance figure in the body`() {
        val body = "A/c XX1234 bal Rs.10,000.00. Rs.500 debited on 03-07-26 at SWIGGY"
        val result = BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(500.0, result.amount, 0.001)
    }

    @Test
    fun `recognizes 'sent' as a debit keyword`() {
        val body = "Rs.200.00 sent from A/c XX1234 to SWIGGY on 06-07-26"
        val result = BankSmsParser.parse("ICICIB", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(SmsDirection.DEBIT, result.direction)
        assertEquals(200.0, result.amount, 0.001)
    }

    @Test
    fun `recognizes 'received' as a credit keyword`() {
        val body = "Rs.1,000.00 received in A/c XX1234 from RAVI on 06-07-26"
        val result = BankSmsParser.parse("ICICIB", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(SmsDirection.CREDIT, result.direction)
        assertEquals(1000.0, result.amount, 0.001)
    }

    @Test
    fun `does not false-positive on 'sent' as a substring of an unrelated word`() {
        val body = "Please give your consent for the KYC update process to continue using UPI worth Rs.500"
        assertNull(BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L))
    }
}
