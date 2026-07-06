package com.subramanya.artha.domain.rules

import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionType

/**
 * The typed shape of a Rules Engine rule. Persisted as JSON on
 * [com.subramanya.artha.data.entity.TransactionRuleEntity] so the SQLite schema
 * never grows when we add a new condition / action — only the codec changes.
 *
 * Conditions combine with [logic] (ALL = AND, ANY = OR). Empty list always matches.
 *
 * Action ordering doesn't matter for the v1 actions because none of them collide
 * (setCategory + setTaxSection apply to different fields). The engine guarantees a
 * deterministic apply order regardless.
 */

enum class ConditionLogic { ALL, ANY }

enum class AmountOp { EQ, GT, LT, GTE, LTE }

sealed interface RuleCondition {
    data class DescriptionContains(val text: String, val ignoreCase: Boolean = true) : RuleCondition
    data class AmountCompare(val op: AmountOp, val value: Double) : RuleCondition
    data class SourceIs(val kind: SourceKind, val id: String?) : RuleCondition
    data class DestinationIs(val kind: SourceKind, val id: String?) : RuleCondition
    data class PaymentAppIs(val app: PaymentApp) : RuleCondition
    data class TypeIs(val type: TransactionType) : RuleCondition
    data class HasPersonRelation(val relation: com.subramanya.artha.data.entity.enums.PersonRelation) : RuleCondition
    data class TimeOfDayBetween(val fromMinuteOfDay: Int, val toMinuteOfDay: Int) : RuleCondition
}

data class RuleConditions(val logic: ConditionLogic = ConditionLogic.ALL, val items: List<RuleCondition> = emptyList())

sealed interface RuleAction {
    data class SetType(val type: TransactionType) : RuleAction
    data class SetCategory(val categoryId: String, val subCategoryId: String? = null) : RuleAction
    data class SetTaxSection(val section: String) : RuleAction
    data class AddTag(val tagId: String) : RuleAction
    data class AddPerson(val personId: String) : RuleAction

    /** Marks the transaction so the MonthlyTotals aggregator skips it (e.g., card payments). */
    data object ExcludeFromExpenseTotal : RuleAction

    /** Routes via the spouse-prompt dialog at save-time instead of auto-applying. */
    data object PromptSpouse : RuleAction
}

data class RuleActions(val items: List<RuleAction> = emptyList())
