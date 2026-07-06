package com.subramanya.artha.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.balance.BalanceCalculator
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class CardDetailUiState(
    val card: Card? = null,
    val currentOutstanding: Double = 0.0,
    val availableLimit: Double? = null,
    val utilizationFraction: Float? = null,
    val transactions: List<Transaction> = emptyList(),
    val chartPoints: List<Double> = emptyList(),
    val showArchiveConfirm: Boolean = false,
    val showDeleteConfirm: Boolean = false,
)

class CardDetailViewModel(
    private val cardId: String,
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val showArchiveConfirm = MutableStateFlow(false)
    private val showDeleteConfirm = MutableStateFlow(false)

    val state: StateFlow<CardDetailUiState> = combine(
        cardRepository.observeById(cardId),
        transactionRepository.observeForAccountOrCard(cardId),
        showArchiveConfirm.asStateFlow(),
        showDeleteConfirm.asStateFlow(),
    ) { card, transactions, archiveConfirm, deleteConfirm ->
        if (card == null) {
            return@combine CardDetailUiState(
                showArchiveConfirm = archiveConfirm,
                showDeleteConfirm = deleteConfirm,
            )
        }
        val entities = transactions.map { it.toEntity() }
        val outstanding = BalanceCalculator.computeCardOutstanding(cardId, entities)
        val available = card.creditLimit?.let { (it - outstanding).coerceAtLeast(0.0) }
        val utilization = card.creditLimit
            ?.takeIf { it > 0.0 }
            ?.let { (outstanding / it).coerceIn(0.0, 1.0).toFloat() }
        val chart = chartPointsLast30Days(cardId, entities)

        CardDetailUiState(
            card = card,
            currentOutstanding = outstanding,
            availableLimit = available,
            utilizationFraction = utilization,
            transactions = transactions,
            chartPoints = chart,
            showArchiveConfirm = archiveConfirm,
            showDeleteConfirm = deleteConfirm,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardDetailUiState())

    fun requestArchive() {
        if (state.value.card != null) showArchiveConfirm.update { true }
    }
    fun dismissArchiveConfirm() = showArchiveConfirm.update { false }
    fun confirmArchive(onArchived: () -> Unit) {
        val current = state.value.card ?: return
        viewModelScope.launch {
            cardRepository.archive(current)
            showArchiveConfirm.update { false }
            onArchived()
        }
    }

    fun restore(onRestored: () -> Unit) {
        val current = state.value.card ?: return
        viewModelScope.launch {
            cardRepository.restore(current)
            onRestored()
        }
    }

    fun requestDelete() {
        if (state.value.card != null) showDeleteConfirm.update { true }
    }
    fun dismissDeleteConfirm() = showDeleteConfirm.update { false }
    fun confirmDelete(onDeleted: () -> Unit) {
        val current = state.value.card ?: return
        // Never hard-delete a card that still has transactions — it would orphan them. The UI
        // routes to Archive instead; this is the defensive backstop.
        if (state.value.transactions.isNotEmpty()) {
            showDeleteConfirm.update { false }
            return
        }
        viewModelScope.launch {
            cardRepository.delete(current)
            showDeleteConfirm.update { false }
            onDeleted()
        }
    }

    /**
     * End-of-day outstanding for each of the last [CHART_DAYS] days, oldest first.
     * Mirrors the account chart approach: reuse [BalanceCalculator.computeCardOutstanding]
     * so the chart cannot drift from canonical balance rules.
     */
    private fun chartPointsLast30Days(cardId: String, entities: List<com.subramanya.artha.data.entity.TransactionEntity>): List<Double> {
        val today = Instant.fromEpochMilliseconds(clock()).toLocalDateTime(timeZone).date
        val out = ArrayList<Double>(CHART_DAYS)
        for (daysAgo in (CHART_DAYS - 1) downTo 0) {
            val day = today.minus(daysAgo, DateTimeUnit.DAY)
            val nextDayStart = day.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds()
            val cutoff = nextDayStart - 1
            val upTo = entities.filter { it.date <= cutoff }
            out.add(BalanceCalculator.computeCardOutstanding(cardId, upTo))
        }
        if (out.all { it == 0.0 }) return emptyList()
        return out
    }

    private companion object {
        const val CHART_DAYS: Int = 30
    }
}

class CardDetailViewModelFactory(
    private val cardId: String,
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CardDetailViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return CardDetailViewModel(cardId, cardRepository, transactionRepository) as T
    }
}
