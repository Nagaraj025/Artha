package com.subramanya.artha.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.PendingTransactionRepository
import com.subramanya.artha.domain.model.PendingSmsTransaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One pending SMS-detected transaction plus its rule-suggested category name, if any. */
data class ReviewItem(
    val pending: PendingSmsTransaction,
    val suggestedCategoryName: String?,
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
) : ViewModel() {

    val state: StateFlow<ReviewUiState> = pendingTransactionRepository.observeAll()
        .map { pendingList ->
            ReviewUiState(
                items = pendingList.map { pending ->
                    val categoryName = pending.suggestedCategoryId
                        ?.let { categoryRepository.getById(it)?.name }
                    ReviewItem(pending, categoryName)
                },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    fun dismiss(id: String) {
        viewModelScope.launch { pendingTransactionRepository.dismiss(id) }
    }
}

class ReviewViewModelFactory(
    private val pendingTransactionRepository: PendingTransactionRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ReviewViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return ReviewViewModel(pendingTransactionRepository, categoryRepository) as T
    }
}
