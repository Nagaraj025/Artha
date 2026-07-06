package com.subramanya.artha.data.balance

import com.subramanya.artha.data.entity.enums.BudgetPeriod
import com.subramanya.artha.data.entity.enums.BudgetScope
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Budget
import com.subramanya.artha.domain.model.Transaction
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Period boundaries (inclusive start, exclusive end) for a given Budget. */
data class BudgetPeriodBounds(val startMillis: Long, val endMillis: Long, val daysRemaining: Int)

object BudgetCalculator {

    /**
     * Resolves the *current* period for a budget. For MONTHLY we use the calendar
     * month containing today (not the budget's startDate-relative month) so multiple
     * budgets reconcile against the same period boundaries. WEEKLY uses Mon-Sun.
     */
    fun currentPeriod(
        period: BudgetPeriod,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        now: Long = Clock.System.now().toEpochMilliseconds(),
    ): BudgetPeriodBounds {
        val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone).date
        return when (period) {
            BudgetPeriod.WEEKLY -> {
                // Monday = 1, Sunday = 7 — bring today back to Monday.
                val daysSinceMonday = today.dayOfWeek.isoDayNumber - 1
                val start = today.plus(-daysSinceMonday, DateTimeUnit.DAY)
                val end = start.plus(7, DateTimeUnit.DAY)
                bounds(start, end, today, timeZone)
            }
            BudgetPeriod.MONTHLY -> {
                val start = LocalDate(today.year, today.monthNumber, 1)
                val end = start.plus(1, DateTimeUnit.MONTH)
                bounds(start, end, today, timeZone)
            }
            BudgetPeriod.YEARLY -> {
                val start = LocalDate(today.year, 1, 1)
                val end = LocalDate(today.year + 1, 1, 1)
                bounds(start, end, today, timeZone)
            }
        }
    }

    /** Sum of EXPENSE transactions in [bounds] that match the budget's scope. */
    fun spentIn(budget: Budget, bounds: BudgetPeriodBounds, transactions: List<Transaction>): Double = transactions.asSequence()
        .filter { it.type == TransactionType.EXPENSE }
        .filter { it.date in bounds.startMillis until bounds.endMillis }
        .filter { txn ->
            when (budget.scope) {
                BudgetScope.OVERALL -> true
                BudgetScope.CATEGORY ->
                    budget.categoryId != null &&
                        (txn.categoryId == budget.categoryId || txn.subCategoryId == budget.categoryId)
            }
        }
        .sumOf { it.amount }

    private fun bounds(startDate: LocalDate, endExclusive: LocalDate, today: LocalDate, timeZone: TimeZone): BudgetPeriodBounds =
        BudgetPeriodBounds(
            startMillis = startDate.atStartOfDayIn(timeZone).toEpochMilliseconds(),
            endMillis = endExclusive.atStartOfDayIn(timeZone).toEpochMilliseconds(),
            daysRemaining = today.daysUntil(endExclusive).coerceAtLeast(0),
        )

    private val kotlinx.datetime.DayOfWeek.isoDayNumber: Int
        get() = when (this) {
            kotlinx.datetime.DayOfWeek.MONDAY -> 1
            kotlinx.datetime.DayOfWeek.TUESDAY -> 2
            kotlinx.datetime.DayOfWeek.WEDNESDAY -> 3
            kotlinx.datetime.DayOfWeek.THURSDAY -> 4
            kotlinx.datetime.DayOfWeek.FRIDAY -> 5
            kotlinx.datetime.DayOfWeek.SATURDAY -> 6
            kotlinx.datetime.DayOfWeek.SUNDAY -> 7
            else -> 1
        }
}
