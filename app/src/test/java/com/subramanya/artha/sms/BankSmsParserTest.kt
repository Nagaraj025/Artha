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

    @Test
    fun `extracts a UPI VPA merchant from a 'from' clause, truncated at the at-sign`() {
        val body = "Received Rs.500.00 in your Kotak Bank AC X7286 from 8299260887singh@ybl on 06-07-26.UPI Ref:697502248275."
        val result = BankSmsParser.parse("VM-KOTAKB-S", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(SmsDirection.CREDIT, result.direction)
        assertEquals(500.0, result.amount, 0.001)
        assertEquals("8299260887singh", result.merchant)
    }

    @Test
    fun `preserves internal dots in a VPA username while still truncating at the at-sign`() {
        val body = "Received Rs.800.00 in your Kotak Bank AC X7286 from harshita.5395@wahdfcbank on 05-07-26.UPI Ref:125799986378."
        val result = BankSmsParser.parse("VM-KOTAKB-S", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals("harshita.5395", result.merchant)
    }

    @Test
    fun `extracts a named counterparty from a semicolon-delimited credited clause, not the SMS-BLOCK footer phone number`() {
        val body = "ICICI Bank Acct XX607 debited for Rs 1.00 on 06-Jul-26; NAGARAJ MALEKOP credited. " +
            "UPI:809737170158. Call 18002662 for dispute. SMS BLOCK 607 to 9215676766."
        val result = BankSmsParser.parse("ICICIB", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(SmsDirection.DEBIT, result.direction)
        assertEquals(1.0, result.amount, 0.001)
        assertEquals("607", result.accountHint)
        assertEquals("NAGARAJ MALEKOP", result.merchant)
    }

    @Test
    fun `does not treat 'A-slash-c' as a merchant when there is no other named counterparty`() {
        val body = "Rs.500.00 debited from A/c XX1234 on 06-07-26"
        val result = BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(null, result.merchant)
    }

    @Test
    fun `captures a multi-word payer name from an SBI-style credit transfer`() {
        val body = "Dear SBI UPI User, your A/c X1234 credited by Rs.500.00 on 05Jul26 transfer from RAMESH KUMAR Ref No 512345678901."
        val result = BankSmsParser.parse("SBIINB", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(SmsDirection.CREDIT, result.direction)
        assertEquals("RAMESH KUMAR", result.merchant)
    }

    @Test
    fun `extracts the payer name from a slash-delimited UPI reference blob`() {
        val body = "Rs.800.00 credited to your A/c XX7286 from UPI/402216758243/PHONEPE/Payment on 05-07-26."
        val result = BankSmsParser.parse("KOTAKB", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals("PHONEPE", result.merchant)
    }

    @Test
    fun `extracts the payee VPA username from a 'To' clause, not the sender's own bank`() {
        val body = "Sent Rs.500.00 From HDFC Bank A/C x1234 To harish@okhdfcbank On 06-07-26. Not you? Call 18002586161."
        val result = BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(SmsDirection.DEBIT, result.direction)
        assertEquals("harish", result.merchant)
    }

    @Test
    fun `returns null merchant for a card-hold notice with a filler semicolon clause`() {
        val body = "Rs.5000.00 blocked on your HDFC Card XX34; the amount will be debited within 3 working days."
        val result = BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(null, result.merchant)
    }

    @Test
    fun `does not fabricate a merchant from the 'to' inside a word like Auto`() {
        val body = "Rs.499.00 debited for Netflix Auto Pay on 06-07-26. Avl Bal Rs.2000.00."
        val result = BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(null, result.merchant)
    }

    @Test
    fun `returns null rather than a bare phone-number VPA as the merchant`() {
        val body = "Received Rs.1.00 in your Kotak Bank AC X7286 from 9876543210@ybl on 06-07-26.UPI Ref:500747965294."
        val result = BankSmsParser.parse("KOTAKB", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(SmsDirection.CREDIT, result.direction)
        assertEquals(null, result.merchant)
    }

    @Test
    fun `prefers the real UPI counterparty over a 'Call X to report' footer imperative`() {
        val body = "Rs.500.00 credited to your Kotak Bank A/c X7286 from ramesh.kumar@oksbi on 06-07-26. " +
            "Not you? Call 18002099191 to report."
        val result = BankSmsParser.parse("KOTAKB", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals("ramesh.kumar", result.merchant)
    }

    @Test
    fun `captures a hyphenated merchant name in a semicolon credited clause`() {
        val body = "ICICI Bank Acct XX607 debited for Rs 300.00 on 06-Jul-26; PVR-INOX credited. " +
            "UPI:809737170158. Call 18002662 for dispute. SMS BLOCK 607 to 9215676766."
        val result = BankSmsParser.parse("ICICIB", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals("PVR-INOX", result.merchant)
    }

    @Test
    fun `captures a digit-leading merchant name in a semicolon credited clause`() {
        val body = "ICICI Bank Acct XX607 debited for Rs 50.00 on 06-Jul-26; 3M INDIA LTD credited. " +
            "UPI:809737170158. Call 18002662 for dispute. SMS BLOCK 607 to 9215676766."
        val result = BankSmsParser.parse("ICICIB", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals("3M INDIA LTD", result.merchant)
    }

    @Test
    fun `captures a multi-word NEFT payer name from a 'from' clause without truncation`() {
        val body = "Rs.5000.00 credited to your Kotak Bank A/c X7286 from RAJESH KUMAR on 06-07-26 via NEFT."
        val result = BankSmsParser.parse("KOTAKB", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals("RAJESH KUMAR", result.merchant)
    }

    @Test
    fun `extracts the card hint from 'ending with' phrasing`() {
        val body = "Rs.339.00 spent on your SBI Credit Card ending with 0440 at Blinkit " +
            "on 06-07-26 via UPI (Ref No. 655317701697). Trxn. not done by you?"
        val result = BankSmsParser.parse("SBICRD", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(SmsDirection.DEBIT, result.direction)
        assertEquals(339.0, result.amount, 0.001)
        assertEquals("0440", result.accountHint)
        assertEquals("Blinkit", result.merchant)
    }

    @Test
    fun `extracts the account hint from 'ending in' phrasing`() {
        val body = "Rs.1200.00 debited from your Account ending in 5678 on 06-07-26."
        val result = BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals("5678", result.accountHint)
    }

    @Test
    fun `does not misread the transaction amount as account hint when no account number is present`() {
        val body = "Your account debited for Rs.500 towards SIP mandate."
        val result = BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(SmsDirection.DEBIT, result.direction)
        assertEquals(500.0, result.amount, 0.001)
        assertNull(result.accountHint)
    }

    @Test
    fun `does not misread an IMPS reference number as account hint`() {
        val body = "Rs.5000 credited to your account. IMPS Ref no 123456 from RAMESH."
        val result = BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(SmsDirection.CREDIT, result.direction)
        assertNull(result.accountHint)
    }

    @Test
    fun `does not misread the available limit as account hint`() {
        val body = "Rs.500 spent on Card. Avl Lmt Rs.45000."
        val result = BankSmsParser.parse("SBICRD", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(SmsDirection.DEBIT, result.direction)
        assertEquals(500.0, result.amount, 0.001)
        assertNull(result.accountHint)
    }
}
