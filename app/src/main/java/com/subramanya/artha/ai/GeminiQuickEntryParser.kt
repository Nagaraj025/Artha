package com.subramanya.artha.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.TransactionType
import kotlinx.datetime.Clock
import org.json.JSONObject

/**
 * Gemini-backed implementation of [AiQuickEntryParser].
 *
 * The API key comes from a [keyProvider] (a suspend lambda that reads the user's
 * stored preference) so the user can paste/rotate the key in Settings without
 * restarting the app. An empty key short-circuits to [AiQuickEntryResult.NoApiKey]
 * so the UI shows a friendly hint instead of crashing.
 */
class GeminiQuickEntryParser(private val keyProvider: suspend () -> String, private val modelName: String = "gemini-1.5-flash-latest") :
    AiQuickEntryParser {

    /** Build a model on demand — keeps the parser stateless w.r.t. the live key. */
    private fun modelFor(key: String): GenerativeModel? =
        key.takeIf { it.isNotBlank() }?.let { GenerativeModel(modelName = modelName, apiKey = it) }

    override suspend fun parse(input: AiQuickEntryInput): AiQuickEntryResult {
        val gen = modelFor(keyProvider()) ?: return AiQuickEntryResult.NoApiKey
        return runCatching {
            val prompt = buildPrompt(input.text)
            val payload: Content = if (input.photo != null) {
                content {
                    image(input.photo)
                    text(prompt)
                }
            } else {
                content { text(prompt) }
            }
            val response = gen.generateContent(payload)
            val raw = response.text.orEmpty()
            val json = extractJsonObject(raw) ?: throw IllegalStateException("Model didn't return JSON")
            AiQuickEntryResult.Success(decode(json, raw))
        }.getOrElse { AiQuickEntryResult.Error(it.message ?: "Gemini call failed") }
    }

    /**
     * Round-trips a tiny prompt against the Gemini endpoint so Settings can confirm
     * the key works before persisting it. Anything the SDK throws containing "API key"
     * is treated as an outright rejection — everything else as a recoverable network
     * blip so the user isn't blocked from saving in an unstable cell-data window.
     */
    override suspend fun validateKey(candidate: String): KeyValidationResult {
        val gen = modelFor(candidate) ?: return KeyValidationResult.Invalid("Key is empty")
        return runCatching {
            gen.generateContent(content { text("Reply with just: ok") })
            KeyValidationResult.Ok
        }.getOrElse { err ->
            val msg = err.message.orEmpty()
            val looksLikeAuth = listOf("API key", "API_KEY", "401", "403", "invalid", "INVALID_ARGUMENT", "PERMISSION_DENIED")
                .any { it in msg }
            if (looksLikeAuth) KeyValidationResult.Invalid(msg) else KeyValidationResult.NetworkError(msg)
        }
    }

    private fun buildPrompt(userText: String): String = """
        You are a finance-app assistant for a user in India. Parse the input into a single
        JSON object with these fields (all optional — omit any you can't determine):
          - type: one of EXPENSE, INCOME, TRANSFER
          - amount: number in INR rupees
          - description: short human-readable text
          - category: 1-2 word category (e.g., "Food", "Transport", "Bills")
          - paymentApp: one of GPAY, PHONEPE, PAYTM, CRED, BHIM, CARD_SWIPE, CASH, OTHER
          - dateText: relative date phrase like "today", "yesterday", "3 days ago"
          - place: location/merchant name if mentioned

        Reply with ONLY the JSON object, no prose, no markdown fences.
        User input: ${userText.ifBlank { "(see attached image)" }}
    """.trimIndent()

    private fun extractJsonObject(raw: String): JSONObject? {
        // Tolerate ```json fences and prose; isolate the first {...} block.
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { JSONObject(raw.substring(start, end + 1)) }.getOrNull()
    }

    private fun decode(json: JSONObject, raw: String): AiQuickEntryParsed {
        val typeStr = json.optString("type").takeIf { it.isNotBlank() }
        val amount = json.opt("amount")?.toString()?.toDoubleOrNull()
        val description = json.optString("description").takeIf { it.isNotBlank() }
        val category = json.optString("category").takeIf { it.isNotBlank() }
        val paymentAppStr = json.optString("paymentApp").takeIf { it.isNotBlank() }
        val dateText = json.optString("dateText").takeIf { it.isNotBlank() }
        val place = json.optString("place").takeIf { it.isNotBlank() }

        val type = runCatching { typeStr?.let { TransactionType.valueOf(it.uppercase()) } }.getOrNull()
        val app = runCatching { paymentAppStr?.let { PaymentApp.valueOf(it.uppercase()) } }
            .getOrNull() ?: PaymentApp.OTHER

        return AiQuickEntryParsed(
            type = AiField(type, type?.let { Confidence.HIGH } ?: Confidence.LOW),
            amount = AiField(amount, amount?.let { Confidence.HIGH } ?: Confidence.LOW),
            description = AiField(description, description?.let { Confidence.MEDIUM } ?: Confidence.LOW),
            categoryHint = AiField(category, category?.let { Confidence.MEDIUM } ?: Confidence.LOW),
            paymentApp = AiField(app, if (paymentAppStr != null) Confidence.HIGH else Confidence.LOW),
            dateMillis = AiField(
                dateText?.let(::resolveRelativeDate),
                dateText?.let { Confidence.MEDIUM } ?: Confidence.LOW,
            ),
            place = AiField(place, place?.let { Confidence.MEDIUM } ?: Confidence.LOW),
            rawModelResponse = raw,
        )
    }

    /** Resolves the relative phrase to an epoch-ms instant. Falls back to now. */
    private fun resolveRelativeDate(phrase: String): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        val daysAgo = when {
            "yesterday" in phrase.lowercase() -> 1
            "today" in phrase.lowercase() -> 0
            phrase.matches(Regex("""\d+\s*days?\s*ago""", RegexOption.IGNORE_CASE)) ->
                Regex("""\d+""").find(phrase)?.value?.toIntOrNull() ?: 0
            else -> 0
        }
        return now - daysAgo * 86_400_000L
    }
}
