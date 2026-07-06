package com.subramanya.artha.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.R
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.CardWithBalance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CardsViewModel(private val cardRepository: CardRepository) : ViewModel() {

    private val view = MutableStateFlow(CardsView.ACTIVE)
    private val reorderMode = MutableStateFlow(false)

    private val message = MutableStateFlow<Int?>(null)
    val toastMessage: StateFlow<Int?> = message.asStateFlow()
    fun consumeToast() = message.update { null }

    val state: StateFlow<CardsUiState> = combine(
        cardRepository.observeActiveWithBalances(),
        cardRepository.observeArchived(),
        view,
        reorderMode,
    ) { active, archived, currentView, isReorder ->
        // Archived rows surface a frozen "0" outstanding — they're history, not live.
        val archivedWithBalance = archived.map { CardWithBalance(it, currentOutstanding = 0.0) }
        CardsUiState(
            view = currentView,
            isReorderMode = isReorder && currentView == CardsView.ACTIVE,
            activeCards = active,
            archivedCards = archivedWithBalance,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardsUiState())

    fun showActive() = view.update { CardsView.ACTIVE }
    fun showArchived() {
        view.update { CardsView.ARCHIVED }
        reorderMode.update { false }
    }

    fun enterReorderMode() {
        if (view.value == CardsView.ACTIVE) reorderMode.update { true }
    }
    fun exitReorderMode() = reorderMode.update { false }

    fun archive(card: Card) = viewModelScope.launch { cardRepository.archive(card) }
    fun restore(card: Card) = viewModelScope.launch { cardRepository.restore(card) }
    fun delete(card: Card) = viewModelScope.launch {
        // Archive instead of orphaning referenced transactions (mirrors the detail-screen guard).
        if (cardRepository.hasReferencingTransactions(card.id)) {
            cardRepository.archive(card)
            message.update { R.string.entity_delete_archived_instead }
        } else {
            cardRepository.delete(card)
        }
    }

    fun moveUp(card: Card) = moveBy(card, -1)
    fun moveDown(card: Card) = moveBy(card, +1)

    private fun moveBy(card: Card, delta: Int) {
        val snapshot = state.value.activeCards.map { it.card }
        val index = snapshot.indexOfFirst { it.id == card.id }
        if (index < 0) return
        val targetIndex = index + delta
        if (targetIndex !in snapshot.indices) return
        val a = snapshot[index]
        val b = snapshot[targetIndex]
        viewModelScope.launch {
            cardRepository.update(a.copy(displayOrder = b.displayOrder))
            cardRepository.update(b.copy(displayOrder = a.displayOrder))
        }
    }
}

class CardsViewModelFactory(private val cardRepository: CardRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CardsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return CardsViewModel(cardRepository) as T
    }
}
