package com.subramanya.artha.domain.rules

import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.PersonRelation
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Person
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.domain.model.TransactionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    @Test fun `empty rules list returns transaction unchanged with no flags`() {
        val txn = expense(amount = 100.0, description = "Random shop")
        val result = RuleEngine.apply(txn, emptyList(), emptyList())
        assertEquals(txn, result.transaction)
        assertFalse(result.excludeFromExpenseTotal)
        assertFalse(result.askSpousePrompt)
        assertTrue(result.appliedRuleNames.isEmpty())
    }

    @Test fun `description-contains rule rewrites category`() {
        val txn = expense(description = "Swiggy order #4321")
        val rule = rule(
            name = "Swiggy = food",
            conditions = RuleConditions(items = listOf(RuleCondition.DescriptionContains("swiggy"))),
            actions = RuleActions(
                items = listOf(RuleAction.SetCategory("cat_food_drink", "cat_food_drink_food_delivery")),
            ),
        )
        val result = RuleEngine.apply(txn, listOf(rule), emptyList())
        assertEquals("cat_food_drink", result.transaction.categoryId)
        assertEquals("cat_food_drink_food_delivery", result.transaction.subCategoryId)
        assertEquals(listOf("Swiggy = food"), result.appliedRuleNames)
    }

    @Test fun `ALL combinator requires every condition to match`() {
        val txn = expense(description = "Swiggy lunch", amount = 100.0)
        val rule = rule(
            name = "Big Swiggy",
            conditions = RuleConditions(
                logic = ConditionLogic.ALL,
                items = listOf(
                    RuleCondition.DescriptionContains("swiggy"),
                    RuleCondition.AmountCompare(AmountOp.GTE, 500.0),
                ),
            ),
            actions = RuleActions(items = listOf(RuleAction.AddTag("tag-big-order"))),
        )
        val result = RuleEngine.apply(txn, listOf(rule), emptyList())
        // Amount fails the >= 500 condition.
        assertTrue(result.appliedRuleNames.isEmpty())
        assertFalse("tag-big-order" in result.transaction.tagIds)
    }

    @Test fun `ANY combinator passes when any single condition matches`() {
        val txn = expense(description = "Random merchant", amount = 999.0)
        val rule = rule(
            name = "Flag big or food",
            conditions = RuleConditions(
                logic = ConditionLogic.ANY,
                items = listOf(
                    RuleCondition.DescriptionContains("swiggy"),
                    RuleCondition.AmountCompare(AmountOp.GTE, 500.0),
                ),
            ),
            actions = RuleActions(items = listOf(RuleAction.AddTag("tag-flag"))),
        )
        val result = RuleEngine.apply(txn, listOf(rule), emptyList())
        assertTrue("tag-flag" in result.transaction.tagIds)
    }

    @Test fun `inactive rule is skipped`() {
        val txn = expense(description = "Swiggy", amount = 0.0)
        val rule = rule(
            name = "off",
            isActive = false,
            conditions = RuleConditions(items = listOf(RuleCondition.DescriptionContains("swiggy"))),
            actions = RuleActions(items = listOf(RuleAction.AddTag("tag-x"))),
        )
        val result = RuleEngine.apply(txn, listOf(rule), emptyList())
        assertTrue(result.appliedRuleNames.isEmpty())
    }

    @Test fun `priority ordering runs lower number first and later wins for SetType`() {
        val txn = expense(description = "test", amount = 0.0)
        val firstWriter = rule(
            name = "priority 1",
            priority = 1,
            conditions = RuleConditions(items = listOf(RuleCondition.DescriptionContains("test"))),
            actions = RuleActions(items = listOf(RuleAction.SetType(TransactionType.REFUND))),
        )
        val laterWinner = rule(
            name = "priority 5",
            priority = 5,
            conditions = RuleConditions(items = listOf(RuleCondition.DescriptionContains("test"))),
            actions = RuleActions(items = listOf(RuleAction.SetType(TransactionType.CASHBACK))),
        )
        // Pass rules in reverse priority order to ensure sorting works.
        val result = RuleEngine.apply(txn, listOf(laterWinner, firstWriter), emptyList())
        // priority 1 runs first, then priority 5 — last write wins.
        assertEquals(TransactionType.CASHBACK, result.transaction.type)
        assertEquals(listOf("priority 1", "priority 5"), result.appliedRuleNames)
    }

    @Test fun `HasPersonRelation matches when transaction has a person with that relation`() {
        val spouse = Person(
            id = "p-spouse",
            name = "Spouse",
            relation = PersonRelation.SPOUSE,
            contact = null,
            avatarUri = null,
            createdAt = 0,
        )
        val txn = expense().copy(peopleIds = listOf("p-spouse"))
        val rule = rule(
            name = "Spouse → prompt",
            conditions = RuleConditions(
                logic = ConditionLogic.ALL,
                items = listOf(
                    RuleCondition.HasPersonRelation(PersonRelation.SPOUSE),
                    RuleCondition.TypeIs(TransactionType.EXPENSE),
                ),
            ),
            actions = RuleActions(items = listOf(RuleAction.PromptSpouse)),
        )
        val result = RuleEngine.apply(txn, listOf(rule), listOf(spouse))
        assertTrue(result.askSpousePrompt)
    }

    @Test fun `ExcludeFromExpenseTotal signal accumulates`() {
        val txn = expense().copy(type = TransactionType.CARD_PAYMENT)
        val rule = rule(
            name = "CC Payment",
            conditions = RuleConditions(items = listOf(RuleCondition.TypeIs(TransactionType.CARD_PAYMENT))),
            actions = RuleActions(items = listOf(RuleAction.ExcludeFromExpenseTotal)),
        )
        val result = RuleEngine.apply(txn, listOf(rule), emptyList())
        assertTrue(result.excludeFromExpenseTotal)
    }

    @Test fun `PaymentAppIs matches CRED txn`() {
        val txn = expense(description = "Some cred rewards").copy(paymentApp = PaymentApp.CRED)
        val rule = rule(
            name = "CRED cashback",
            conditions = RuleConditions(items = listOf(RuleCondition.PaymentAppIs(PaymentApp.CRED))),
            actions = RuleActions(items = listOf(RuleAction.SetType(TransactionType.CASHBACK))),
        )
        val result = RuleEngine.apply(txn, listOf(rule), emptyList())
        assertEquals(TransactionType.CASHBACK, result.transaction.type)
    }

    @Test fun `AddTag idempotent — second matching rule does not duplicate`() {
        val txn = expense().copy(tagIds = listOf("tag-x"))
        val rule = rule(
            name = "Re-add tag",
            conditions = RuleConditions(items = listOf(RuleCondition.TypeIs(TransactionType.EXPENSE))),
            actions = RuleActions(items = listOf(RuleAction.AddTag("tag-x"))),
        )
        val result = RuleEngine.apply(txn, listOf(rule), emptyList())
        assertEquals(1, result.transaction.tagIds.count { it == "tag-x" })
    }

    @Test fun `SetType cannot flip an income into an outflow`() {
        // The real bug: a keyword rule (e.g. "LIC") fires on an actual INCOME (an LIC maturity
        // payout) and rewrites it to INVESTMENT_BUY — flipping the account from credit to debit.
        val income = expense(description = "LIC maturity payout").copy(type = TransactionType.INCOME)
        val rule = rule(
            name = "LIC = investment",
            conditions = RuleConditions(items = listOf(RuleCondition.DescriptionContains("lic"))),
            actions = RuleActions(items = listOf(RuleAction.SetType(TransactionType.INVESTMENT_BUY))),
        )
        val result = RuleEngine.apply(income, listOf(rule), emptyList())
        assertEquals(TransactionType.INCOME, result.transaction.type)
    }

    @Test fun `SetType cannot flip a transfer into a refund`() {
        val transfer = expense(description = "refund settlement").copy(
            type = TransactionType.TRANSFER,
            destinationType = SourceKind.ACCOUNT,
            destinationId = "acct-b",
        )
        val rule = rule(
            name = "refund",
            conditions = RuleConditions(items = listOf(RuleCondition.DescriptionContains("refund"))),
            actions = RuleActions(items = listOf(RuleAction.SetType(TransactionType.REFUND))),
        )
        val result = RuleEngine.apply(transfer, listOf(rule), emptyList())
        assertEquals(TransactionType.TRANSFER, result.transaction.type)
    }

    @Test fun `SetType still reclassifies an expense freely (intended behaviour preserved)`() {
        val txn = expense(description = "ELSS SIP")
        val rule = rule(
            name = "ELSS = investment",
            conditions = RuleConditions(items = listOf(RuleCondition.DescriptionContains("elss"))),
            actions = RuleActions(items = listOf(RuleAction.SetType(TransactionType.INVESTMENT_BUY))),
        )
        val result = RuleEngine.apply(txn, listOf(rule), emptyList())
        assertEquals(TransactionType.INVESTMENT_BUY, result.transaction.type)
    }

    @Test fun `SetType allows same-direction retype on a non-expense`() {
        // INCOME -> INTEREST are both inflows; reclassifying within a direction is safe.
        val income = expense(description = "FD interest credit").copy(type = TransactionType.INCOME)
        val rule = rule(
            name = "interest",
            conditions = RuleConditions(items = listOf(RuleCondition.DescriptionContains("interest"))),
            actions = RuleActions(items = listOf(RuleAction.SetType(TransactionType.INTEREST))),
        )
        val result = RuleEngine.apply(income, listOf(rule), emptyList())
        assertEquals(TransactionType.INTEREST, result.transaction.type)
    }

    // ---------- helpers ----------

    private fun expense(amount: Double = 100.0, description: String = "test"): Transaction = Transaction(
        id = "txn-1",
        type = TransactionType.EXPENSE,
        amount = amount,
        currency = "INR",
        date = 0L,
        description = description,
        categoryId = null,
        subCategoryId = null,
        sourceType = SourceKind.ACCOUNT,
        sourceId = "acct-a",
        destinationType = null,
        destinationId = null,
        paymentApp = PaymentApp.OTHER,
        place = null,
        latitude = null,
        longitude = null,
        peopleIds = emptyList(),
        tagIds = emptyList(),
        receiptUri = null,
        notes = null,
        taxSection = null,
        recurringRuleId = null,
        isSplit = false,
        splitGroupId = null,
        source = TransactionSource.MANUAL,
        createdAt = 0,
        updatedAt = 0,
    )

    private fun rule(
        name: String,
        priority: Int = 10,
        isActive: Boolean = true,
        conditions: RuleConditions,
        actions: RuleActions,
    ): TransactionRule = TransactionRule(
        id = "rule-${name.hashCode()}",
        name = name,
        conditions = conditions,
        actions = actions,
        priority = priority,
        isActive = isActive,
        isSystem = false,
        createdAt = 0,
    )
}
