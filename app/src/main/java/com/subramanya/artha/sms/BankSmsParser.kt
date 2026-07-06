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

    // The gap between the anchor ("a/c"/"account"/"card"/…) and the masked digits bridges ONLY
    // genuine account-number phrasing tokens — "ending", "with"/"in", "no."/"no", mask chars
    // (x/X/*) and whitespace — never arbitrary text. This captures real references like
    // "Card ending with 0440" / "A/c XX1234" while rejecting a later unrelated number when NO
    // masked account is present: e.g. "account debited for Rs.500…", "account. IMPS Ref no 123456"
    // and "Card. Avl Lmt Rs.45000" all fail at the anchor (next token isn't a bridge word) → null.
    private val ACCOUNT_REGEX = Regex(
        """(?:a/c|acct|account|card)(?:\s+ending)?(?:\s+(?:with|in))?\s*(?:no\.?)?\s*[xX*]*(\d{3,6})""",
        RegexOption.IGNORE_CASE,
    )

    // "; NAME credited/debited" (ICICI-style). First char and body allow digits, '&', '-' so
    // names like "3M INDIA LTD", "PVR-INOX", "H&M" survive. A letter must be present and no
    // sentence-fragment filler word may appear (guards against "; the amount will be debited").
    private val NAMED_PARTY_REGEX = Regex(
        """;\s*([A-Za-z0-9][A-Za-z0-9 &.'-]{2,40}?)\s+(?:credited|debited)\b""",
        RegexOption.IGNORE_CASE,
    )

    // A UPI VPA anywhere in the body, e.g. "harshita.5395@wahdfcbank" or "harish@okhdfcbank".
    // The username (before '@') is the counterparty id regardless of whether the clause said
    // "from" (credit) or "to" (debit). Tried before the from/at-to clause strategies so a real
    // VPA counterparty always wins over a footer imperative ("...to report") or the sender's
    // own bank name.
    private val VPA_REGEX = Regex("""([A-Za-z0-9][A-Za-z0-9._-]{1,40})@[A-Za-z]{2,}""")

    // "from <payer>" up to a trailing boundary word / punctuation. Captures a multi-word human
    // name ("RAMESH KUMAR") or a slash-delimited UPI ref blob ("UPI/<ref>/<NAME>/Payment") whole,
    // for refineFromClause() to post-process.
    private val FROM_CLAUSE_REGEX = Regex(
        """\bfrom\s+(.+?)(?=\s+(?:on|at|to|ref|via|dt|dated|info|not|avl|bal)\b|[.,;]|$)""",
        RegexOption.IGNORE_CASE,
    )

    // "at <merchant>" / "to <merchant>". A \b before (at|to) stops it matching the "to" inside a
    // word like "Auto". Tried LAST — the from-clause strategy already ran, so a footer "...to
    // report." only surfaces here when nothing better exists, and its all-digit/short results are
    // still guarded by the caller.
    private val MERCHANT_REGEX = Regex(
        """\b(?:at|to)\s+(?!your\b|the\b)([A-Za-z0-9 &.'-]{3,30}?)(?=[.,:]|\s+(?:avl|bal|on|dt|info)\b|$)""",
        RegexOption.IGNORE_CASE,
    )

    // First-word fillers on a "from <X>" clause that mean the user's own side, not a counterparty.
    private val FROM_STOPWORDS = setOf("a/c", "acct", "account", "your", "the", "wallet")

    // Noise segments inside a "UPI/<ref>/<NAME>/<type>" blob — never the counterparty name.
    private val UPI_NOISE_SEGMENTS = setOf("upi", "neft", "imps", "rtgs", "payment", "transfer", "p2a", "p2m")

    // Words that reveal a "; ... credited" capture is a sentence fragment, not a real name.
    private val NAMED_PARTY_FILLERS = setOf(
        "the", "a", "an", "your", "will", "be", "been", "has", "have", "was", "is",
        "amount", "account", "transaction", "of", "for",
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
        val merchant = extractMerchant(body)

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

    /**
     * Extracts the counterparty/merchant name, trying four strategies in priority order and
     * returning the first confident result:
     *  1. [NAMED_PARTY_REGEX] — "; NAME credited/debited" (rejects sentence-fragment fillers).
     *  2. [VPA_REGEX] — a UPI VPA anywhere; its username half is the counterparty (rejects an
     *     all-digit, i.e. phone-number, VPA username, which is not a usable name).
     *  3. [FROM_CLAUSE_REGEX] + [refineFromClause] — "from <payer>" (multi-word name or a
     *     slash-delimited UPI ref blob).
     *  4. [MERCHANT_REGEX] — "at X" / "to X", rejecting an all-digit capture (e.g. a phone number
     *     from a trailing "SMS BLOCK ... to <phone>" footer).
     */
    private fun extractMerchant(body: String): String? {
        NAMED_PARTY_REGEX.find(body)?.groupValues?.get(1)?.trim()?.let { candidate ->
            if (candidate.any(Char::isLetter) &&
                candidate.split(' ').none { it.lowercase() in NAMED_PARTY_FILLERS }
            ) {
                return candidate
            }
        }

        VPA_REGEX.find(body)?.groupValues?.get(1)?.trim()?.let { username ->
            if (username.any(Char::isLetter)) return username
        }

        FROM_CLAUSE_REGEX.find(body)?.groupValues?.get(1)?.trim()?.let { clause ->
            refineFromClause(clause)?.let { return it }
        }

        MERCHANT_REGEX.find(body)?.groupValues?.get(1)?.trim()?.let { candidate ->
            if (candidate.any(Char::isLetter)) return candidate
        }

        return null
    }

    /** Post-processes a raw "from <clause>" capture into a counterparty name, or null if it is
     *  the user's own side (A/c, wallet), an already-VPA-handled clause, or otherwise not a name. */
    private fun refineFromClause(clause: String): String? {
        // A VPA here was already handled by VPA_REGEX (strategy 2); an all-digit VPA username was
        // intentionally dropped there, so don't resurrect the raw "<digits>@handle" blob.
        if ('@' in clause) return null
        // Reject the user's own side up front: "A/c XX1234" has first word "a/c", a stopword.
        // This MUST run before the slash branch below, because "A/c" itself contains a '/' and
        // would otherwise be mis-split into ["A", "c XX1234"] and yield a bogus "A" merchant.
        val firstWord = clause.split(' ').firstOrNull()?.lowercase()
        if (firstWord in FROM_STOPWORDS) return null
        // Slash-delimited UPI ref: first letter-bearing, non-noise segment is the name.
        if ('/' in clause) {
            return clause.split('/')
                .map { it.trim() }
                .firstOrNull { it.any(Char::isLetter) && it.lowercase() !in UPI_NOISE_SEGMENTS }
        }
        if (clause.any(Char::isLetter)) return clause
        return null
    }
}
