package com.subramanya.artha.domain.rules

import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Person
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.domain.model.TransactionRule
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Result of running the engine — modified transaction + side-signals the caller acts on. */
data class RuleEngineResult(
    val transaction: Transaction,
    /** Any rule asked to skip this transaction from the monthly-expense aggregator
     *  (e.g., the pre-seeded "Credit Card Payment" rule). Caller persists alongside. */
    val excludeFromExpenseTotal: Boolean,
    /** Any rule asked to short-circuit through the spouse-prompt dialog. The save flow
     *  in AddTransactionViewModel already implements PromptSpouse via the per-row
     *  spouse-detect path, so a rule asking for this is treated identically: pause the
     *  save and let the UI route through the dialog. */
    val askSpousePrompt: Boolean,
    /** Names of rules that matched and were applied. Used for the "Applied N rules"
     *  toast / debug hint on the Transaction Detail screen. */
    val appliedRuleNames: List<String>,
)

/**
 * Stateless rule engine. Given the active rules + the people referenced by the
 * transaction (needed to evaluate HasPersonRelation), produces a [RuleEngineResult].
 *
 * Apply ordering:
 *   1. Sort rules by priority ascending (lowest priority number runs first).
 *   2. For each matching rule, apply its actions in declaration order.
 *   3. Action collisions (e.g. two SetCategory rules) are last-write-wins.
 *   4. PromptSpouse + ExcludeFromExpenseTotal flags accumulate (any matching rule
 *      flips them to true; they never flip back).
 *
 * Pure — no I/O. Wire it from the save path of any transaction-mutating ViewModel.
 */
object RuleEngine {

    fun apply(
        candidate: Transaction,
        rules: List<TransactionRule>,
        knownPeople: List<Person>,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): RuleEngineResult {
        var current = candidate
        var exclude = false
        var promptSpouse = false
        val applied = mutableListOf<String>()

        val ordered = rules.filter { it.isActive }.sortedBy { it.priority }
        for (rule in ordered) {
            if (!matches(rule.conditions, current, knownPeople, timeZone)) continue
            applied += rule.name
            for (action in rule.actions.items) {
                when (action) {
                    is RuleAction.SetType ->
                        if (canRetype(current.type, action.type)) {
                            current = current.copy(type = action.type)
                        }
                    is RuleAction.SetCategory -> current = current.copy(
                        categoryId = action.categoryId,
                        subCategoryId = action.subCategoryId ?: current.subCategoryId,
                    )
                    is RuleAction.SetTaxSection -> current = current.copy(taxSection = action.section)
                    is RuleAction.AddTag -> {
                        if (action.tagId !in current.tagIds) {
                            current = current.copy(tagIds = current.tagIds + action.tagId)
                        }
                    }
                    is RuleAction.AddPerson -> {
                        if (action.personId !in current.peopleIds) {
                            current = current.copy(peopleIds = current.peopleIds + action.personId)
                        }
                    }
                    RuleAction.ExcludeFromExpenseTotal -> exclude = true
                    RuleAction.PromptSpouse -> promptSpouse = true
                }
            }
        }
        return RuleEngineResult(
            transaction = current,
            excludeFromExpenseTotal = exclude,
            askSpousePrompt = promptSpouse,
            appliedRuleNames = applied,
        )
    }

    // ---------- SetType safety guard ----------

    private enum class Direction { OUT, IN, TWO_LEG, NEUTRAL }

    /** The cashflow direction a type implies for the affected account/card. */
    private fun directionClass(type: TransactionType): Direction = when (type) {
        TransactionType.EXPENSE, TransactionType.LOAN_GIVEN, TransactionType.GIFT_SENT,
        TransactionType.INVESTMENT_BUY,
        -> Direction.OUT
        TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK,
        TransactionType.INTEREST, TransactionType.LOAN_RECEIVED, TransactionType.GIFT_RECEIVED,
        TransactionType.INVESTMENT_SELL,
        -> Direction.IN
        TransactionType.TRANSFER, TransactionType.CARD_PAYMENT -> Direction.TWO_LEG
        TransactionType.ADJUSTMENT -> Direction.NEUTRAL
    }

    /**
     * Whether a `SetType` rule may rewrite [current] → [target]. Keyword rules
     * (e.g. "LIC", "refund", "salary") are designed to refine an EXPENSE the user logged, so an
     * EXPENSE may be reclassified freely. But the same keyword must NEVER silently flip a
     * non-expense across the cashflow boundary — turning a real INCOME into INVESTMENT_BUY
     * (credit → debit) or a TRANSFER into a REFUND (dropping the destination leg) corrupts
     * balances by ~2× the amount. For any non-expense, only same-direction retyping is allowed.
     */
    private fun canRetype(current: TransactionType, target: TransactionType): Boolean =
        current == TransactionType.EXPENSE || directionClass(current) == directionClass(target)

    // ---------- condition matcher ----------

    private fun matches(conditions: RuleConditions, txn: Transaction, knownPeople: List<Person>, timeZone: TimeZone): Boolean {
        if (conditions.items.isEmpty()) return true
        val results = conditions.items.map { evaluate(it, txn, knownPeople, timeZone) }
        return when (conditions.logic) {
            ConditionLogic.ALL -> results.all { it }
            ConditionLogic.ANY -> results.any { it }
        }
    }

    private fun evaluate(condition: RuleCondition, txn: Transaction, knownPeople: List<Person>, timeZone: TimeZone): Boolean =
        when (condition) {
            is RuleCondition.DescriptionContains -> {
                if (condition.ignoreCase) {
                    txn.description.contains(condition.text, ignoreCase = true)
                } else {
                    txn.description.contains(condition.text)
                }
            }
            is RuleCondition.AmountCompare -> when (condition.op) {
                AmountOp.EQ -> txn.amount == condition.value
                AmountOp.GT -> txn.amount > condition.value
                AmountOp.LT -> txn.amount < condition.value
                AmountOp.GTE -> txn.amount >= condition.value
                AmountOp.LTE -> txn.amount <= condition.value
            }
            is RuleCondition.SourceIs -> {
                val kindMatch = txn.sourceType == condition.kind
                val idMatch = condition.id == null || txn.sourceId == condition.id
                kindMatch && idMatch
            }
            is RuleCondition.DestinationIs -> {
                val kindMatch = txn.destinationType == condition.kind
                val idMatch = condition.id == null || txn.destinationId == condition.id
                kindMatch && idMatch
            }
            is RuleCondition.PaymentAppIs -> txn.paymentApp == condition.app
            is RuleCondition.TypeIs -> txn.type == condition.type
            is RuleCondition.HasPersonRelation -> {
                val tagged = knownPeople.filter { it.id in txn.peopleIds }
                tagged.any { it.relation == condition.relation }
            }
            is RuleCondition.TimeOfDayBetween -> {
                val local = Instant.fromEpochMilliseconds(txn.date).toLocalDateTime(timeZone)
                val minuteOfDay = local.hour * 60 + local.minute
                if (condition.fromMinuteOfDay <= condition.toMinuteOfDay) {
                    minuteOfDay in condition.fromMinuteOfDay..condition.toMinuteOfDay
                } else {
                    // Wraps over midnight.
                    minuteOfDay >= condition.fromMinuteOfDay || minuteOfDay <= condition.toMinuteOfDay
                }
            }
        }
}
