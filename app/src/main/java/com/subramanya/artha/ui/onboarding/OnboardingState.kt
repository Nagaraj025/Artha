package com.subramanya.artha.ui.onboarding

import com.subramanya.artha.data.entity.enums.AccountType

/**
 * Local form state for the "add account" step. Strings hold the raw input so the user
 * can correct typos mid-entry — validation runs only on commit (Add another / Done).
 */
data class AccountDraft(
    val name: String = "",
    val type: AccountType = AccountType.SAVINGS,
    val institution: String = "",
    val openingBalanceText: String = "",
) {
    val isValid: Boolean
        get() = name.isNotBlank() && parsedBalance != null

    /** Blank → treated as 0.0 opening balance. Non-blank-but-non-numeric → null (invalid). */
    val parsedBalance: Double?
        get() = if (openingBalanceText.isBlank()) 0.0 else openingBalanceText.toDoubleOrNull()
}

/** Persisted (in-memory only) drafts already committed via "Add another". */
data class PendingAccount(val name: String, val type: AccountType, val institution: String?, val openingBalance: Double)

data class OnboardingUiState(
    val name: String = "",
    val accountDraft: AccountDraft = AccountDraft(),
    val pendingAccounts: List<PendingAccount> = emptyList(),
    val isSaving: Boolean = false,
    val savedAndReady: Boolean = false,
) {
    val canFinishOnboarding: Boolean
        get() = name.isNotBlank() && (pendingAccounts.isNotEmpty() || accountDraft.isValid)
}
