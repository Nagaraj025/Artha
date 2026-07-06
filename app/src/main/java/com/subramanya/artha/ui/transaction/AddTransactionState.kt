package com.subramanya.artha.ui.transaction

import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionType

/**
 * Top-level tabs the user picks between when adding a transaction.
 *
 * INVEST is the "I moved money from an account into an investment" path —
 * RD top-ups, FD principal, SIP, gold purchases, etc. It saves as a
 * TransactionType.INVESTMENT_BUY so the spending number on Dashboard /
 * Reports stays clean instead of bloating "expense" with savings.
 */
enum class TransactionTab { EXPENSE, INCOME, TRANSFER, INVEST }

/**
 * A source or destination selection in the From/To pickers. Keeps the display name
 * pre-resolved so the chip can render without re-querying. `isCreditCard` lets the
 * sheet auto-detect Transfer → CARD_PAYMENT without another lookup.
 */
data class FundsEndpoint(val kind: SourceKind, val id: String, val displayName: String, val isCreditCard: Boolean = false)

/**
 * Snapshot of what the spouse-prompt dialog needs to render. Non-null when the dialog
 * is showing; cleared when the user responds or cancels.
 */
data class SpousePromptInfo(val amount: Double, val personId: String, val personName: String)

data class AddTransactionUiState(
    val tab: TransactionTab = TransactionTab.EXPENSE,
    val amountText: String = "",
    val dateTimeMillis: Long = System.currentTimeMillis(),
    val source: FundsEndpoint? = null,
    val destination: FundsEndpoint? = null,
    val categoryId: String? = null,
    val categoryDisplay: String? = null,
    val subCategoryId: String? = null,
    val subCategoryDisplay: String? = null,
    val description: String = "",
    val paymentApp: PaymentApp = PaymentApp.OTHER,
    val peopleIds: Set<String> = emptySet(),
    val place: String = "",
    val tagIds: Set<String> = emptySet(),
    val receiptUri: String? = null,
    val notes: String = "",
    val showValidationErrors: Boolean = false,
    val isSaving: Boolean = false,
    val savedAndClose: Boolean = false,
    val pendingSpousePrompt: SpousePromptInfo? = null,
    /** True while editing an existing transaction — drives the sheet title + Save/Update label. */
    val isEditing: Boolean = false,
) {
    val parsedAmount: Double?
        get() = amountText.toDoubleOrNull()?.takeIf { it > 0.0 }

    /**
     * Auto-detect CARD_PAYMENT on the Transfer tab when destination is a credit card.
     * Income/Expense always map 1:1.
     */
    val effectiveType: TransactionType
        get() = when (tab) {
            TransactionTab.EXPENSE -> TransactionType.EXPENSE
            TransactionTab.INCOME -> TransactionType.INCOME
            TransactionTab.TRANSFER ->
                if (destination?.isCreditCard == true) {
                    TransactionType.CARD_PAYMENT
                } else {
                    TransactionType.TRANSFER
                }
            TransactionTab.INVEST -> TransactionType.INVESTMENT_BUY
        }

    /** Whether the Transfer→Card_Payment hint chip should be shown. */
    val showCardPaymentHint: Boolean
        get() = tab == TransactionTab.TRANSFER && destination?.isCreditCard == true

    /** True only when all required fields validate. Drives Save-button enablement. */
    val isValid: Boolean
        get() {
            if (parsedAmount == null) return false
            if (source == null) return false
            if (description.isBlank()) return false
            return when (tab) {
                TransactionTab.EXPENSE, TransactionTab.INCOME -> categoryId != null
                TransactionTab.TRANSFER, TransactionTab.INVEST ->
                    destination != null && (source.kind != destination.kind || source.id != destination.id)
            }
        }
}
