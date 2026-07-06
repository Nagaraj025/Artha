package com.subramanya.artha.ui.insurance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.repository.InsuranceRepository
import com.subramanya.artha.data.repository.InvestmentRepository
import com.subramanya.artha.domain.model.Insurance
import com.subramanya.artha.domain.model.Investment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InsuranceDetailUiState(
    val insurance: Insurance? = null,
    val linkedInvestment: Investment? = null,
    val showDeleteConfirm: Boolean = false,
)

class InsuranceDetailViewModel(
    private val insuranceId: String,
    private val insuranceRepository: InsuranceRepository,
    private val investmentRepository: InvestmentRepository,
) : ViewModel() {

    private val showDeleteConfirm = MutableStateFlow(false)
    private val linkedInvestmentFlow = MutableStateFlow<Investment?>(null)

    init {
        // Poll once on init — the linked investment is set at insurance-creation time
        // and rarely changes during a detail view's lifetime. A cheap one-shot fetch
        // avoids combining a third Flow when 99% of the time it's null.
        viewModelScope.launch {
            linkedInvestmentFlow.update {
                investmentRepository.findByLinkedInsurance(insuranceId)
            }
        }
    }

    val state: StateFlow<InsuranceDetailUiState> = combine(
        insuranceRepository.observeById(insuranceId),
        showDeleteConfirm,
        linkedInvestmentFlow,
    ) { insurance, deleteConfirm, linked ->
        InsuranceDetailUiState(
            insurance = insurance,
            linkedInvestment = linked,
            showDeleteConfirm = deleteConfirm,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsuranceDetailUiState())

    fun requestDelete() {
        if (state.value.insurance != null) showDeleteConfirm.update { true }
    }
    fun dismissDeleteConfirm() = showDeleteConfirm.update { false }

    fun confirmDelete(onDeleted: () -> Unit) {
        val current = state.value.insurance ?: return
        viewModelScope.launch {
            // InsuranceRepository.delete unlinks any linked investment so it isn't orphaned.
            insuranceRepository.delete(current)
            showDeleteConfirm.update { false }
            onDeleted()
        }
    }

    fun archive(onArchived: () -> Unit) {
        val current = state.value.insurance ?: return
        viewModelScope.launch {
            insuranceRepository.archive(current)
            onArchived()
        }
    }

    fun restore(onRestored: () -> Unit) {
        val current = state.value.insurance ?: return
        viewModelScope.launch {
            insuranceRepository.restore(current)
            onRestored()
        }
    }
}

class InsuranceDetailViewModelFactory(
    private val insuranceId: String,
    private val insuranceRepository: InsuranceRepository,
    private val investmentRepository: InvestmentRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(InsuranceDetailViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return InsuranceDetailViewModel(
            insuranceId,
            insuranceRepository,
            investmentRepository,
        ) as T
    }
}
