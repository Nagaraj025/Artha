package com.subramanya.artha.domain.rules

import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.PersonRelation
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleSpecJsonTest {

    @Test fun `every condition variant round-trips through encode-decode`() {
        val original = RuleConditions(
            logic = ConditionLogic.ANY,
            items = listOf(
                RuleCondition.DescriptionContains("Mother Dairy", ignoreCase = false),
                RuleCondition.AmountCompare(AmountOp.GTE, 1_000.0),
                RuleCondition.SourceIs(SourceKind.ACCOUNT, "acct-1"),
                RuleCondition.DestinationIs(SourceKind.CARD, "card-2"),
                RuleCondition.PaymentAppIs(PaymentApp.CRED),
                RuleCondition.TypeIs(TransactionType.CARD_PAYMENT),
                RuleCondition.HasPersonRelation(PersonRelation.SPOUSE),
                RuleCondition.TimeOfDayBetween(540, 1080),
            ),
        )
        val raw = RuleSpecJson.encodeConditions(original)
        val decoded = RuleSpecJson.decodeConditions(raw)
        assertEquals(original, decoded)
    }

    @Test fun `every action variant round-trips through encode-decode`() {
        val original = RuleActions(
            items = listOf(
                RuleAction.SetType(TransactionType.INVESTMENT_BUY),
                RuleAction.SetCategory("cat_food_drink", "cat_food_drink_groceries"),
                RuleAction.SetCategory("cat_salary", null),
                RuleAction.SetTaxSection("80C"),
                RuleAction.AddTag("tag-1"),
                RuleAction.AddPerson("person-1"),
                RuleAction.ExcludeFromExpenseTotal,
                RuleAction.PromptSpouse,
            ),
        )
        val raw = RuleSpecJson.encodeActions(original)
        val decoded = RuleSpecJson.decodeActions(raw)
        assertEquals(original, decoded)
    }

    @Test fun `decoding garbage JSON yields empty RuleConditions, never throws`() {
        val empty = RuleSpecJson.decodeConditions("not-json-at-all")
        assertEquals(RuleConditions(), empty)
    }

    @Test fun `decoding garbage JSON yields empty RuleActions, never throws`() {
        val empty = RuleSpecJson.decodeActions("not-json")
        assertEquals(RuleActions(), empty)
    }

    @Test fun `unknown condition kind is skipped, valid neighbours survive`() {
        val mixed = """{"logic":"ALL","items":[
            {"kind":"DescriptionContains","text":"foo","ignoreCase":true},
            {"kind":"InventedKindThatDoesNotExist","whatever":1},
            {"kind":"TypeIs","type":"EXPENSE"}
        ]}
        """.trimIndent()
        val decoded = RuleSpecJson.decodeConditions(mixed)
        assertEquals(ConditionLogic.ALL, decoded.logic)
        assertEquals(2, decoded.items.size)
        assertEquals(RuleCondition.DescriptionContains("foo"), decoded.items[0])
        assertEquals(RuleCondition.TypeIs(TransactionType.EXPENSE), decoded.items[1])
    }
}
