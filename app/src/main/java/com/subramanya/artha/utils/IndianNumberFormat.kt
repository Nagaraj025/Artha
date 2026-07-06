package com.subramanya.artha.utils

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Indian-style number formatting. Groups in 2,2,3 (e.g. 1,00,000 not 100,000) and
 * always prefixes the INR symbol — `Rs.` / `INR` are forbidden per CLAUDE.md.
 *
 * Whole rupees are shown without trailing `.00`; non-whole amounts keep two
 * decimal places. The sign moves outside the symbol: `-₹1,234.50`.
 *
 * Examples:
 *   100         → ₹100
 *   1_000       → ₹1,000
 *   100_000     → ₹1,00,000
 *   12_34_567.5 → ₹12,34,567.50
 */
object IndianNumberFormat {

    private const val INR_SYMBOL: String = "₹"

    // HANDOFF §6.5 — the negative sign is an en-dash, not a hyphen-minus.
    // The editorial typography (Instrument Serif numerals) reads sloppy with
    // a hyphen, so every "-₹X" rendered through this object becomes "–₹X".
    private const val NEG_SIGN: String = "–"

    /** Default display: hides `.00` on whole rupees, shows two decimals otherwise. */
    fun format(amount: Double): String = format(amount, alwaysShowDecimals = false)

    /** Use when you need consistent column widths regardless of whether amount is whole. */
    fun formatWithDecimals(amount: Double): String = format(amount, alwaysShowDecimals = true)

    /**
     * Compact short form for hero cards & summaries — abbreviates with L/Cr above
     * 1 lakh, otherwise behaves like [format]. Always shows ₹ prefix and respects
     * sign. Examples:
     *   1_000        → ₹1,000
     *   1_00_000     → ₹1 L
     *   2_50_000     → ₹2.5 L
     *   1_00_00_000  → ₹1 Cr
     *   12_34_56_789 → ₹12 Cr
     */
    fun formatCompact(amount: Double): String {
        if (kotlin.math.abs(amount) < 100_000.0) return format(amount)
        val isNegative = amount < 0.0
        val abs = kotlin.math.abs(amount)
        val (value, suffix) = when {
            abs >= 1_00_00_000.0 -> abs / 1_00_00_000.0 to "Cr"
            else -> abs / 1_00_000.0 to "L"
        }
        val rendered = if (value >= 10.0) {
            "%.0f".format(value)
        } else {
            "%.1f".format(value).trimEnd('0').trimEnd('.')
        }
        return (if (isNegative) NEG_SIGN else "") + INR_SYMBOL + rendered + " " + suffix
    }

    private fun format(amount: Double, alwaysShowDecimals: Boolean): String {
        val isNegative = amount < 0.0
        val abs = abs(amount)
        val cents = (abs * 100.0).roundToLong()
        val whole = cents / 100L
        val fraction = (cents % 100L).toInt()

        val groupedWhole = groupIndian(whole)
        val tail = when {
            alwaysShowDecimals || fraction != 0 -> ".%02d".format(fraction)
            else -> ""
        }
        val signPrefix = if (isNegative) NEG_SIGN else ""
        return "$signPrefix$INR_SYMBOL$groupedWhole$tail"
    }

    /** Public for callers that want only the grouped integer part (e.g., chips/tabs). */
    fun groupIndian(value: Long): String {
        if (value < 0L) return NEG_SIGN + groupIndian(-value)
        val digits = value.toString()
        if (digits.length <= 3) return digits

        val lastThree = digits.substring(digits.length - 3)
        val remaining = digits.substring(0, digits.length - 3)
        // Walk the remaining digits in pairs from the right and join with commas.
        val grouped = StringBuilder()
        var i = remaining.length
        while (i > 2) {
            grouped.insert(0, "," + remaining.substring(i - 2, i))
            i -= 2
        }
        grouped.insert(0, remaining.substring(0, i))
        return "$grouped,$lastThree"
    }
}
