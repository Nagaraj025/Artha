package com.subramanya.artha.sms

import com.subramanya.artha.domain.model.SmsDirection
import kotlin.math.abs

data class ParsedBankSms(
    val sender: String,
    val receivedAt: Long,
    val direction: SmsDirection,
    val amount: Double,
    val accountHint: String?,
    val merchant: String?,
)

/**
 * Generic keyword+regex heuristic for Indian bank debit/credit SMS — deliberately not a
 * per-bank catalogue (docs/plans/2026-07-03-sms-auto-detect-design.md, Decision 1). Broad
 * coverage over per-bank precision. Returns null for anything not confidently a bank
 * transaction alert (OTP, promo, unparseable amount).
 */
object BankSmsParser {

    private val EXCLUDE_KEYWORDS = listOf(
        "otp",
        "one time password",
        "offer",
        "cashback offer",
        "sale",
        "discount",
    )
    private val DEBIT_KEYWORDS = listOf("debited", "debit", "spent", "withdrawn", "sent")
    private val CREDIT_KEYWORDS = listOf("credited", "credit", "received")
    private val AMOUNT_REGEX = Regex("""(?:Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val ACCOUNT_REGEX = Regex(
        """(?:a/c|acct|account|card)[^\d]{0,10}(?:no\.?)?\s*[xX*]*(\d{3,6})""",
        RegexOption.IGNORE_CASE,
    )

    // Non-greedy capture stops at the nearest punctuation or common trailing-clause marker
    // (e.g. "Avl Bal ...") instead of swallowing the rest of the SMS. A leading pronoun/article
    // ("to your account", "at the branch") is excluded outright — null is preferable to garbage.
    private val MERCHANT_REGEX = Regex(
        """(?:at|to)\s+(?!your\b|the\b)([A-Za-z0-9 &.'-]{3,30}?)(?=[.,:]|\s+(?:avl|bal|on|dt|info)\b|$)""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(sender: String, body: String, receivedAt: Long): ParsedBankSms? {
        val lower = body.lowercase()
        if (EXCLUDE_KEYWORDS.any { lower.contains(it) }) return null

        // Word-boundary match, not a bare substring check: short generic words like "sent"
        // are also substrings of unrelated words ("consent", "represent"), which a plain
        // `contains` would wrongly treat as a debit keyword.
        val debitMatch = firstWordMatch(lower, DEBIT_KEYWORDS)
        val creditMatch = firstWordMatch(lower, CREDIT_KEYWORDS)
        val (direction, keywordIndex) = when {
            debitMatch != null -> SmsDirection.DEBIT to debitMatch
            creditMatch != null -> SmsDirection.CREDIT to creditMatch
            else -> return null
        }

        // Many bank templates state the running balance before the transaction amount
        // (e.g. "bal Rs.10,000.00. Rs.500 debited..."), so take every Rs./INR figure in the
        // body and prefer whichever one sits closest to the matched debit/credit keyword,
        // rather than always trusting the first figure that appears.
        val amountMatches = AMOUNT_REGEX.findAll(body).toList()
        if (amountMatches.isEmpty()) return null
        val amountMatch = amountMatches.minBy { abs(it.range.first - keywordIndex) }
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        val accountHint = ACCOUNT_REGEX.find(body)?.groupValues?.get(1)
        val merchant = MERCHANT_REGEX.find(body)?.groupValues?.get(1)?.trim()

        return ParsedBankSms(
            sender = sender,
            receivedAt = receivedAt,
            direction = direction,
            amount = amount,
            accountHint = accountHint,
            merchant = merchant,
        )
    }

    /** Index of the first whole-word match among [words] in [text], or null if none match. */
    private fun firstWordMatch(text: String, words: List<String>): Int? {
        for (word in words) {
            val match = Regex("""\b${Regex.escape(word)}\b""").find(text)
            if (match != null) return match.range.first
        }
        return null
    }
}
