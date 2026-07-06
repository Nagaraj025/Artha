package com.subramanya.artha.data.db.seed

import com.subramanya.artha.data.entity.TransactionRuleEntity
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.PersonRelation
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.rules.ConditionLogic
import com.subramanya.artha.domain.rules.RuleAction
import com.subramanya.artha.domain.rules.RuleActions
import com.subramanya.artha.domain.rules.RuleCondition
import com.subramanya.artha.domain.rules.RuleConditions
import com.subramanya.artha.domain.rules.RuleSpecJson

/**
 * The 10 default rules from PRD §10. Each has a stable id so the seeder is idempotent
 * (INSERT OR IGNORE keeps user edits). `isSystem = true` means the user can disable or
 * tweak but not delete — same convention as system categories.
 *
 * Rule #6 (spouse) uses [RuleAction.PromptSpouse] which the engine routes to the
 * existing spouse-prompt dialog instead of mutating the transaction silently.
 */
internal object SeedRules {

    private const val NOW = 0L // placeholder; createdAt is rewritten on insert with real ts

    fun all(): List<TransactionRuleEntity> {
        val rows = mutableListOf<TransactionRuleEntity>()

        rows += rule(
            id = "rule_credit_card_payment",
            name = "Credit Card Payment",
            priority = 10,
            conditions = RuleConditions(
                logic = ConditionLogic.ALL,
                items = listOf(RuleCondition.TypeIs(TransactionType.CARD_PAYMENT)),
            ),
            actions = RuleActions(items = listOf(RuleAction.ExcludeFromExpenseTotal)),
        )

        rows += rule(
            id = "rule_lic_premium_is_investment",
            name = "LIC Premium is Investment",
            priority = 20,
            conditions = RuleConditions(
                logic = ConditionLogic.ALL,
                items = listOf(RuleCondition.DescriptionContains("LIC")),
            ),
            actions = RuleActions(
                items = listOf(
                    RuleAction.SetType(TransactionType.INVESTMENT_BUY),
                    RuleAction.SetTaxSection("80C"),
                ),
            ),
        )

        rows += rule(
            id = "rule_elss_sip_is_investment",
            name = "ELSS SIP is Investment",
            priority = 25,
            conditions = RuleConditions(
                logic = ConditionLogic.ANY,
                items = listOf(
                    RuleCondition.DescriptionContains("ELSS"),
                    RuleCondition.DescriptionContains("Groww"),
                ),
            ),
            actions = RuleActions(
                items = listOf(
                    RuleAction.SetType(TransactionType.INVESTMENT_BUY),
                    RuleAction.SetTaxSection("80C"),
                ),
            ),
        )

        rows += rule(
            id = "rule_health_insurance_is_80d",
            name = "Health Insurance is 80D",
            priority = 30,
            conditions = RuleConditions(
                logic = ConditionLogic.ALL,
                items = listOf(RuleCondition.DescriptionContains("health insurance")),
            ),
            actions = RuleActions(items = listOf(RuleAction.SetTaxSection("80D"))),
        )

        rows += rule(
            id = "rule_salary_credit",
            name = "Salary Credit",
            priority = 40,
            conditions = RuleConditions(
                logic = ConditionLogic.ANY,
                items = listOf(
                    RuleCondition.DescriptionContains("salary"),
                    RuleCondition.DescriptionContains("SAL CR"),
                ),
            ),
            actions = RuleActions(
                items = listOf(
                    RuleAction.SetType(TransactionType.INCOME),
                    RuleAction.SetCategory(categoryId = "cat_salary", subCategoryId = "cat_salary_base"),
                ),
            ),
        )

        rows += rule(
            id = "rule_money_to_spouse_prompt",
            name = "Money to Spouse — prompt user",
            priority = 50,
            conditions = RuleConditions(
                logic = ConditionLogic.ALL,
                items = listOf(
                    RuleCondition.HasPersonRelation(PersonRelation.SPOUSE),
                    RuleCondition.TypeIs(TransactionType.EXPENSE),
                ),
            ),
            actions = RuleActions(items = listOf(RuleAction.PromptSpouse)),
        )

        rows += rule(
            id = "rule_money_to_parents",
            name = "Money to Parents = Family Expense",
            priority = 60,
            conditions = RuleConditions(
                logic = ConditionLogic.ALL,
                items = listOf(RuleCondition.HasPersonRelation(PersonRelation.PARENT)),
            ),
            actions = RuleActions(
                items = listOf(
                    RuleAction.SetCategory(
                        categoryId = "cat_family",
                        subCategoryId = "cat_family_money_to_parents",
                    ),
                ),
            ),
        )

        rows += rule(
            id = "rule_upi_refund",
            name = "UPI Refund",
            priority = 70,
            conditions = RuleConditions(
                logic = ConditionLogic.ANY,
                items = listOf(
                    RuleCondition.DescriptionContains("refund"),
                    RuleCondition.DescriptionContains("REVERSAL"),
                ),
            ),
            actions = RuleActions(items = listOf(RuleAction.SetType(TransactionType.REFUND))),
        )

        rows += rule(
            id = "rule_cashback",
            name = "Cashback",
            priority = 80,
            conditions = RuleConditions(
                logic = ConditionLogic.ANY,
                items = listOf(
                    RuleCondition.DescriptionContains("cashback"),
                    RuleCondition.PaymentAppIs(PaymentApp.CRED),
                ),
            ),
            actions = RuleActions(
                items = listOf(
                    RuleAction.SetType(TransactionType.CASHBACK),
                    RuleAction.SetCategory(
                        categoryId = "cat_cashback_rewards",
                        subCategoryId = "cat_cashback_rewards_card_cashback",
                    ),
                ),
            ),
        )

        rows += rule(
            id = "rule_temple_donation",
            name = "Temple Donation",
            priority = 90,
            conditions = RuleConditions(
                logic = ConditionLogic.ANY,
                items = listOf(
                    RuleCondition.DescriptionContains("temple"),
                    RuleCondition.DescriptionContains("devasthanam"),
                    RuleCondition.DescriptionContains("hundi"),
                ),
            ),
            actions = RuleActions(
                items = listOf(
                    RuleAction.SetCategory(
                        categoryId = "cat_religious_spiritual",
                        subCategoryId = "cat_religious_spiritual_temple_donations",
                    ),
                ),
            ),
        )

        return rows
    }

    private fun rule(id: String, name: String, priority: Int, conditions: RuleConditions, actions: RuleActions): TransactionRuleEntity =
        TransactionRuleEntity(
            id = id,
            name = name,
            conditionsJson = RuleSpecJson.encodeConditions(conditions),
            actionsJson = RuleSpecJson.encodeActions(actions),
            priority = priority,
            isActive = true,
            isSystem = true,
            createdAt = NOW,
        )
}
