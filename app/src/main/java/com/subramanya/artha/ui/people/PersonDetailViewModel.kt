package com.subramanya.artha.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.data.repository.PersonRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.domain.model.Person
import com.subramanya.artha.domain.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Snapshot for the Person Detail screen. The three figures mirror the per-person
 * ledger model the list-row chip already implies:
 *   - [netBalance]   positive = they owe you, negative = you owe them
 *   - [theyOweYou]   sum of EXPENSE / LOAN_GIVEN / GIFT_SENT tagged with them
 *   - [youOweThem]   sum of INCOME / LOAN_RECEIVED / GIFT_RECEIVED tagged with them
 *
 * [transactions] is the filtered list (newest first) — same source the row's
 * balance was computed from, so the user can trace any number on the hero.
 */
data class PersonDetailUiState(
    val person: Person? = null,
    val netBalance: Double = 0.0,
    val theyOweYou: Double = 0.0,
    val youOweThem: Double = 0.0,
    val transactions: List<Transaction> = emptyList(),
    val showDeleteConfirm: Boolean = false,
)

class PersonDetailViewModel(
    private val personId: String,
    private val personRepository: PersonRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    private val deleteConfirm = MutableStateFlow(false)

    val state: StateFlow<PersonDetailUiState> = combine(
        personRepository.observeAll(),
        transactionRepository.observeAll(),
        deleteConfirm,
    ) { people, txns, confirm ->
        val person = people.firstOrNull { it.id == personId }
        val mine = txns.filter { personId in it.peopleIds }
        var owesYou = 0.0
        var youOwe = 0.0
        for (t in mine) {
            when (t.type) {
                TransactionType.EXPENSE,
                TransactionType.LOAN_GIVEN,
                TransactionType.GIFT_SENT,
                -> owesYou += t.amount
                TransactionType.INCOME,
                TransactionType.LOAN_RECEIVED,
                TransactionType.GIFT_RECEIVED,
                -> youOwe += t.amount
                else -> Unit
            }
        }
        PersonDetailUiState(
            person = person,
            netBalance = owesYou - youOwe,
            theyOweYou = owesYou,
            youOweThem = youOwe,
            transactions = mine,
            showDeleteConfirm = confirm,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonDetailUiState())

    fun requestDelete() = deleteConfirm.update { true }
    fun dismissDeleteConfirm() = deleteConfirm.update { false }

    fun confirmDelete(onDeleted: () -> Unit) {
        val current = state.value.person ?: return
        viewModelScope.launch {
            personRepository.delete(current)
            deleteConfirm.update { false }
            onDeleted()
        }
    }

    /** Used by the inline edit sheet — caller passes the resolved person back. */
    fun upsert(person: Person) {
        viewModelScope.launch { personRepository.upsert(person) }
    }
}

class PersonDetailViewModelFactory(
    private val personId: String,
    private val personRepository: PersonRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(PersonDetailViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return PersonDetailViewModel(personId, personRepository, transactionRepository) as T
    }
}
