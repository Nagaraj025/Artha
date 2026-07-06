package com.subramanya.artha.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.PersonRepository
import com.subramanya.artha.data.repository.TagRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class TransactionDetailUiState(
    val transaction: Transaction? = null,
    val sourceName: String? = null,
    val destinationName: String? = null,
    val categoryName: String? = null,
    val subCategoryName: String? = null,
    val peopleNames: List<String> = emptyList(),
    val tagNames: List<String> = emptyList(),
    val showDeleteConfirm: Boolean = false,
)

/**
 * Resolves all the IDs on a transaction (accounts/cards, category/subcategory, people,
 * tags) into display names by joining against the live repositories. Read-only —
 * editing goes through AddTransactionViewModel via `applyEditPrefill`.
 */
class TransactionDetailViewModel(
    private val transactionId: String,
    private val transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    cardRepository: CardRepository,
    categoryRepository: CategoryRepository,
    personRepository: PersonRepository,
    tagRepository: TagRepository,
) : ViewModel() {

    private val showDeleteConfirm = MutableStateFlow(false)

    val state: StateFlow<TransactionDetailUiState> = combine(
        combine(
            transactionRepository.observeById(transactionId),
            accountRepository.observeAll(),
            cardRepository.observeAll(),
        ) { txn, accounts, cards -> Triple(txn, accounts, cards) },
        combine(
            categoryRepository.observeAll(),
            personRepository.observeAll(),
            tagRepository.observeAll(),
        ) { categories, people, tags -> Triple(categories, people, tags) },
        showDeleteConfirm,
    ) { data, lookups, confirm ->
        val (txn, accounts, cards) = data
        val (categories, people, tags) = lookups
        if (txn == null) {
            return@combine TransactionDetailUiState(showDeleteConfirm = confirm)
        }

        // Hydrate peopleIds + tagIds from cross-refs (observeById uses entity flow, not
        // the cross-ref-joined variant). For Phase 1 we fetch once on construction.
        val hydratedTxn = if (txn.peopleIds.isEmpty() && txn.tagIds.isEmpty()) {
            transactionRepository.getById(transactionId) ?: txn
        } else {
            txn
        }

        TransactionDetailUiState(
            transaction = hydratedTxn,
            sourceName = resolveEndpointName(hydratedTxn.sourceType, hydratedTxn.sourceId, accounts, cards),
            destinationName = resolveEndpointName(hydratedTxn.destinationType, hydratedTxn.destinationId, accounts, cards),
            categoryName = categories.firstOrNull { it.id == hydratedTxn.categoryId }?.name,
            subCategoryName = categories.firstOrNull { it.id == hydratedTxn.subCategoryId }?.name,
            peopleNames = hydratedTxn.peopleIds.mapNotNull { id -> people.firstOrNull { it.id == id }?.name },
            tagNames = hydratedTxn.tagIds.mapNotNull { id -> tags.firstOrNull { it.id == id }?.name },
            showDeleteConfirm = confirm,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionDetailUiState())

    fun requestDelete() {
        if (state.value.transaction != null) showDeleteConfirm.update { true }
    }
    fun dismissDeleteConfirm() = showDeleteConfirm.update { false }
    fun confirmDelete(onDeleted: () -> Unit) {
        val current = state.value.transaction ?: return
        viewModelScope.launch {
            transactionRepository.delete(current)
            showDeleteConfirm.update { false }
            onDeleted()
        }
    }

    /**
     * Creates a copy of the current transaction with a fresh id + timestamps. The user
     * lands back on Transactions; the new row will appear at the top.
     */
    fun duplicate(onDuplicated: () -> Unit) {
        val current = state.value.transaction ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            transactionRepository.save(
                current.copy(
                    id = UUID.randomUUID().toString(),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            onDuplicated()
        }
    }

    private fun resolveEndpointName(
        kind: SourceKind?,
        id: String?,
        accounts: List<com.subramanya.artha.domain.model.Account>,
        cards: List<com.subramanya.artha.domain.model.Card>,
    ): String? {
        if (kind == null || id == null) return null
        return when (kind) {
            SourceKind.ACCOUNT -> accounts.firstOrNull { it.id == id }?.name
            SourceKind.CARD -> cards.firstOrNull { it.id == id }?.name
            SourceKind.CASH -> "Cash"
            SourceKind.INVESTMENT -> "Investment"
            SourceKind.EXTERNAL -> "External"
        }
    }
}

class TransactionDetailViewModelFactory(
    private val transactionId: String,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val personRepository: PersonRepository,
    private val tagRepository: TagRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TransactionDetailViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return TransactionDetailViewModel(
            transactionId,
            transactionRepository,
            accountRepository,
            cardRepository,
            categoryRepository,
            personRepository,
            tagRepository,
        ) as T
    }
}
