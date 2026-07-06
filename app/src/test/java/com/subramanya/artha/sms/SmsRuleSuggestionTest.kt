package com.subramanya.artha.sms

import com.subramanya.artha.domain.model.SmsDirection
import com.subramanya.artha.domain.model.TransactionRule
import com.subramanya.artha.domain.rules.ConditionLogic
import com.subramanya.artha.domain.rules.RuleAction
import com.subramanya.artha.domain.rules.RuleActions
import com.subramanya.artha.domain.rules.RuleCondition
import com.subramanya.artha.domain.rules.RuleConditions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsRuleSuggestionTest {

    private val swiggyRule = TransactionRule(
        id = "rule-1",
        name = "Swiggy -> Food",
        conditions = RuleConditions(
            logic = ConditionLogic.ALL,
            items = listOf(RuleCondition.DescriptionContains(text = "SWIGGY", ignoreCase = true)),
        ),
        actions = RuleActions(items = listOf(RuleAction.SetCategory(categoryId = "cat-food"))),
        priority = 0,
        isActive = true,
        isSystem = false,
        createdAt = 0L,
    )

    @Test
    fun `suggests a category when a rule matches the merchant`() {
        val parsed = ParsedBankSms(
            sender = "HDFCBK",
            receivedAt = 1_700_000_000_000L,
            direction = SmsDirection.DEBIT,
            amount = 500.0,
            accountHint = "1234",
            merchant = "SWIGGY",
        )
        val result = suggestCategoryFor(parsed, rules = listOf(swiggyRule), people = emptyList())
        assertEquals("cat-food", result.transaction.categoryId)
    }

    @Test
    fun `leaves category null when no rule matches`() {
        val parsed = ParsedBankSms(
            sender = "HDFCBK",
            receivedAt = 1_700_000_000_000L,
            direction = SmsDirection.DEBIT,
            amount = 500.0,
            accountHint = "1234",
            merchant = "UNKNOWN MERCHANT",
        )
        val result = suggestCategoryFor(parsed, rules = listOf(swiggyRule), people = emptyList())
        assertNull(result.transaction.categoryId)
    }
}
