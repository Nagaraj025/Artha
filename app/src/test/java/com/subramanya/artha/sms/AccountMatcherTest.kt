package com.subramanya.artha.sms

import com.subramanya.artha.data.entity.enums.AccountType
import com.subramanya.artha.data.entity.enums.CardNetwork
import com.subramanya.artha.data.entity.enums.CardType
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Card
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountMatcherTest {

    private fun account(id: String, name: String, last4: String?): Account = Account(
        id = id,
        name = name,
        type = AccountType.SAVINGS,
        institution = null,
        accountNumberLast4 = last4,
        openingBalance = 0.0,
        currency = "INR",
        icon = "account_balance",
        color = 0L,
        isArchived = false,
        displayOrder = 0,
        createdAt = 0L,
    )

    private fun card(id: String, name: String, last4: String?): Card = Card(
        id = id,
        name = name,
        type = CardType.CREDIT,
        issuer = null,
        network = CardNetwork.VISA,
        cardNumberLast4 = last4,
        creditLimit = 1000.0,
        statementDayOfMonth = 1,
        dueDayOfMonth = 15,
        linkedAccountId = null,
        icon = "credit_card",
        color = 0L,
        isArchived = false,
        displayOrder = 0,
        createdAt = 0L,
    )

    @Test
    fun `exact 4-digit match on an account returns Matched account endpoint`() {
        val result = AccountMatcher.match("1234", listOf(account("a1", "HDFC", "1234")), emptyList())
        assertTrue(result is AccountMatch.Matched)
        val funds = (result as AccountMatch.Matched).funds
        assertEquals(SourceKind.ACCOUNT, funds.kind)
        assertEquals("a1", funds.id)
        assertEquals("HDFC", funds.displayName)
    }

    @Test
    fun `suffix match works when the hint is longer than stored last4`() {
        // SMS hint "X7286" -> "7286"? No — hint is 6 digits here, stored is 4; stored is a suffix of hint.
        val result = AccountMatcher.match("567286", emptyList(), listOf(card("c1", "Amazon Pay", "7286")))
        assertTrue(result is AccountMatch.Matched)
        assertEquals("c1", (result as AccountMatch.Matched).funds.id)
        assertEquals(SourceKind.CARD, result.funds.kind)
        assertTrue(result.funds.isCreditCard)
    }

    @Test
    fun `suffix match works when stored last4 is longer than the hint`() {
        val result = AccountMatcher.match("234", listOf(account("a1", "SBI", "1234")), emptyList())
        assertTrue(result is AccountMatch.Matched)
        assertEquals("a1", (result as AccountMatch.Matched).funds.id)
    }

    @Test
    fun `null hint returns NoMatch`() {
        val result = AccountMatcher.match(null, listOf(account("a1", "SBI", "1234")), emptyList())
        assertEquals(AccountMatch.NoMatch, result)
    }

    @Test
    fun `blank hint returns NoMatch`() {
        val result = AccountMatcher.match("   ", listOf(account("a1", "SBI", "1234")), emptyList())
        assertEquals(AccountMatch.NoMatch, result)
    }

    @Test
    fun `no candidate with a matching suffix returns NoMatch`() {
        val result = AccountMatcher.match("9999", listOf(account("a1", "SBI", "1234")), emptyList())
        assertEquals(AccountMatch.NoMatch, result)
    }

    @Test
    fun `candidates with null or blank stored last4 never match`() {
        val result = AccountMatcher.match("1234", listOf(account("a1", "SBI", null), account("a2", "Axis", "")), emptyList())
        assertEquals(AccountMatch.NoMatch, result)
    }

    @Test
    fun `two accounts sharing the same suffix are ambiguous and return NoMatch`() {
        val result = AccountMatcher.match(
            "1234",
            listOf(account("a1", "SBI", "1234"), account("a2", "HDFC", "1234")),
            emptyList(),
        )
        assertEquals(AccountMatch.NoMatch, result)
    }

    @Test
    fun `an account and a card sharing the same suffix are ambiguous and return NoMatch`() {
        val result = AccountMatcher.match(
            "1234",
            listOf(account("a1", "SBI", "1234")),
            listOf(card("c1", "HDFC CC", "1234")),
        )
        assertEquals(AccountMatch.NoMatch, result)
    }
}
