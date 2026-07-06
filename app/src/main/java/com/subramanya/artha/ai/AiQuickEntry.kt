package com.subramanya.artha.ai

import android.graphics.Bitmap
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.TransactionType

/** What the user wants to parse — any combination of these may be present. */
data class AiQuickEntryInput(val text: String = "", val photo: Bitmap? = null)

/** Per-field confidence so the UI can flag low-confidence ones in red. */
enum class Confidence { LOW, MEDIUM, HIGH }

data class AiField<T>(val value: T?, val confidence: Confidence)

/**
 * Structured parse result. Every field is optional + confidence-tagged so the
 * preview screen can surface what to verify before saving.
 *
 * `notes` carries any extra context the model produced (line items from a
 * receipt, the original utterance, etc.) so the user keeps that audit trail.
 */
data class AiQuickEntryParsed(
    val type: AiField<TransactionType>,
    val amount: AiField<Double>,
    val description: AiField<String>,
    /** Free-form category name — caller resolves to a Category.id by fuzzy match. */
    val categoryHint: AiField<String>,
    val paymentApp: AiField<PaymentApp>,
    val dateMillis: AiField<Long>,
    val place: AiField<String>,
    val notes: String? = null,
    val rawModelResponse: String? = null,
)

sealed interface AiQuickEntryResult {
    data class Success(val parsed: AiQuickEntryParsed) : AiQuickEntryResult
    data object NoApiKey : AiQuickEntryResult
    data class Error(val message: String) : AiQuickEntryResult
}

/** Reason a [AiQuickEntryParser.validateKey] call failed, so the UI can phrase the toast. */
sealed interface KeyValidationResult {
    data object Ok : KeyValidationResult

    /** Key was rejected outright (401/403 / "API key not valid" / similar). */
    data class Invalid(val message: String) : KeyValidationResult

    /** Network or transient failure — let the user retry rather than blocking save. */
    data class NetworkError(val message: String) : KeyValidationResult
}

/**
 * Backend-agnostic AI parser. Lets the rest of the app stay ignorant of which
 * model is wired up — swapping providers (Gemini → Claude, etc.) is a single
 * class swap, not a UI rewrite.
 */
interface AiQuickEntryParser {
    suspend fun parse(input: AiQuickEntryInput): AiQuickEntryResult

    /** Round-trips [candidate] against the live API so Settings can refuse to save
     *  a key the provider rejects. Default = "no validator wired" → treated as Ok. */
    suspend fun validateKey(candidate: String): KeyValidationResult = KeyValidationResult.Ok
}
