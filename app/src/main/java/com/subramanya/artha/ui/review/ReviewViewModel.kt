package com.subramanya.artha.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.PendingTransactionRepository
import com.subramanya.artha.domain.model.PendingSmsTransaction
import com.subramanya.artha.sms.AccountMatch
import com.subramanya.artha.sms.AccountMatcher
import com.subramanya.artha.ui.transaction.FundsEndpoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One pending SMS-detected transaction, its rule-suggested category name, and the account/card
 * its last-digits hint resolved to (if exactly one matched). `hasUnmatchedHint` is true when the
 * SMS carried an account hint but nothing matched it — the Review card offers to add it.
 */
data class ReviewItem(
    val pending: PendingSmsTransaction,
    val suggestedCategoryName: String?,
    val matchedFunds: FundsEndpoint? = null,
    val hasUnmatchedHint: Boolean = false,
)

data class ReviewUiState(val items: List<ReviewItem> = emptyList())

/**
 * Backs the Review tab: the list of not-yet-actioned SMS-detected transactions. Tapping
 * a row hands the [PendingSmsTransaction] off to [com.subramanya.artha.ui.transaction.AddTransactionViewModel]
 * (see `applyPendingSmsPrefill`); swiping a row dismisses it directly via [dismiss].
 */
class ReviewViewModel(
    private val pendingTransactionRepository: PendingTransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
) : ViewModel() {

    val state: StateFlow<ReviewUiState> = combine(
        pendingTransactionRepository.observeAll(),
        accountRepository.observeActive(),
        cardRepository.observeActive(),
    ) { pendingList, accounts, cards ->
        ReviewUiState(
            items = pendingList.map { pending ->
                val categoryName = pending.suggestedCategoryId
                    ?.let { categoryRepository.getById(it)?.name }
                val match = AccountMatcher.match(pending.accountHint, accounts, cards)
                val matchedFunds = (match as? AccountMatch.Matched)?.funds
                val hasUnmatchedHint = !pending.accountHint.isNullOrBlank() && matchedFunds == null
                ReviewItem(
                    pending = pending,
                    suggestedCategoryName = categoryName,
                    matchedFunds = matchedFunds,
                    hasUnmatchedHint = hasUnmatchedHint,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    fun dismiss(id: String) {
        viewModelScope.launch { pendingTransactionRepository.dismiss(id) }
    }
}

class ReviewViewModelFactory(
    private val pendingTransactionRepository: PendingTransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ReviewViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return ReviewViewModel(
            pendingTransactionRepository,
            categoryRepository,
            accountRepository,
            cardRepository,
        ) as T
    }
}
