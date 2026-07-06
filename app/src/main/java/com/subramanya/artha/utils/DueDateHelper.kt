package com.subramanya.artha.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/** Days remaining + the resolved next-due [date]. Negative `daysUntil` would mean overdue. */
data class DueInfo(val daysUntil: Int, val date: LocalDate)

/**
 * Given a card's preferred `dueDayOfMonth` (1..31), find the next upcoming due date.
 *
 * Handles February and 30-day months by clamping the day to the last day of the target
 * month — so a dueDay of 31 in April resolves to April 30, and in February it resolves
 * to Feb 28 or 29.
 *
 * Returns null when the input isn't a valid day-of-month, which lets the caller hide
 * the chip rather than displaying nonsense.
 */
fun computeNextDue(
    dueDayOfMonth: Int,
    now: Long = Clock.System.now().toEpochMilliseconds(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): DueInfo? {
    if (dueDayOfMonth !in 1..31) return null
    val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone).date

    val thisMonthTarget = clampToMonth(today.year, today.monthNumber, dueDayOfMonth)
    val target = if (today <= thisMonthTarget) {
        thisMonthTarget
    } else {
        val (ny, nm) = nextMonth(today.year, today.monthNumber)
        clampToMonth(ny, nm, dueDayOfMonth)
    }
    return DueInfo(daysUntil = today.daysUntil(target), date = target)
}

private fun clampToMonth(year: Int, month: Int, day: Int): LocalDate {
    val firstOfNext = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
    val lastDayOfMonth = firstOfNext.minus(1, DateTimeUnit.DAY).dayOfMonth
    return LocalDate(year, month, day.coerceAtMost(lastDayOfMonth))
}

private fun nextMonth(year: Int, month: Int): Pair<Int, Int> = if (month == 12) (year + 1) to 1 else year to (month + 1)
