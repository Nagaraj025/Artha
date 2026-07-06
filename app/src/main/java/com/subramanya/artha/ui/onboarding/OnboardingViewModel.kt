package com.subramanya.artha.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.entity.enums.AccountType
import com.subramanya.artha.data.preferences.SettingsPreferences
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.domain.model.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class OnboardingViewModel(
    private val accountRepository: AccountRepository,
    private val settingsPreferences: SettingsPreferences,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun onNameChanged(value: String) {
        _state.update { it.copy(name = value) }
    }

    fun onAccountNameChanged(value: String) {
        _state.update { it.copy(accountDraft = it.accountDraft.copy(name = value)) }
    }

    fun onAccountTypeChanged(type: AccountType) {
        _state.update { it.copy(accountDraft = it.accountDraft.copy(type = type)) }
    }

    fun onAccountInstitutionChanged(value: String) {
        _state.update { it.copy(accountDraft = it.accountDraft.copy(institution = value)) }
    }

    fun onOpeningBalanceChanged(value: String) {
        // Allow only digits + a single decimal point. Other input is silently dropped
        // so the field rejects letters/symbols without showing an error toast.
        val sanitised = value.filterIndexed { index, c ->
            c.isDigit() || (c == '.' && value.indexOf('.') == index)
        }
        _state.update { it.copy(accountDraft = it.accountDraft.copy(openingBalanceText = sanitised)) }
    }

    /**
     * Commits the current draft (if valid) into the pending list and resets the form.
     * Returns true if the draft was committed; false if it was invalid.
     */
    fun stashCurrentAccount(): Boolean {
        val draft = _state.value.accountDraft
        if (!draft.isValid) return false
        _state.update {
            it.copy(
                pendingAccounts = it.pendingAccounts + draft.toPending(),
                accountDraft = AccountDraft(),
            )
        }
        return true
    }

    /**
     * Persists the userName and every queued account (including the current draft if
     * valid). On success, flips `savedAndReady` to signal the host to navigate.
     */
    fun finishOnboarding() {
        val snapshot = _state.value
        if (!snapshot.canFinishOnboarding || snapshot.isSaving) return

        val accountsToSave = buildList {
            addAll(snapshot.pendingAccounts)
            if (snapshot.accountDraft.isValid) {
                add(snapshot.accountDraft.toPending())
            }
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            settingsPreferences.setUserName(snapshot.name.trim())
            val now = clock()
            accountsToSave.forEachIndexed { index, pending ->
                accountRepository.upsert(pending.toDomain(displayOrder = index, createdAt = now))
            }
            _state.update { it.copy(isSaving = false, savedAndReady = true) }
        }
    }

    private fun AccountDraft.toPending(): PendingAccount = PendingAccount(
        name = name.trim(),
        type = type,
        institution = institution.trim().takeIf { it.isNotBlank() },
        openingBalance = parsedBalance ?: 0.0,
    )

    private fun PendingAccount.toDomain(displayOrder: Int, createdAt: Long): Account = Account(
        id = UUID.randomUUID().toString(),
        name = name,
        type = type,
        institution = institution,
        accountNumberLast4 = null,
        openingBalance = openingBalance,
        currency = "INR",
        icon = type.defaultIcon,
        color = type.defaultColor,
        isArchived = false,
        displayOrder = displayOrder,
        createdAt = createdAt,
    )

    private val AccountType.defaultIcon: String
        get() = when (this) {
            AccountType.SAVINGS -> "account_balance"
            AccountType.CURRENT -> "account_balance"
            AccountType.CASH -> "payments"
            AccountType.WALLET -> "account_balance_wallet"
        }

    private val AccountType.defaultColor: Long
        get() = when (this) {
            AccountType.SAVINGS -> 0xFF0F766E
            AccountType.CURRENT -> 0xFF4338CA
            AccountType.CASH -> 0xFF15803D
            AccountType.WALLET -> 0xFFB45309
        }
}

class OnboardingViewModelFactory(private val accountRepository: AccountRepository, private val settingsPreferences: SettingsPreferences) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return OnboardingViewModel(accountRepository, settingsPreferences) as T
    }
}
