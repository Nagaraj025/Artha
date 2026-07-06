package com.subramanya.artha.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.repository.TransactionRuleRepository
import com.subramanya.artha.domain.model.TransactionRule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RulesUiState(val rules: List<TransactionRule> = emptyList())

class RulesViewModel(private val ruleRepository: TransactionRuleRepository) : ViewModel() {

    val state: StateFlow<RulesUiState> =
        ruleRepository.observeAll()
            .map { rules -> RulesUiState(rules = rules) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesUiState())

    fun toggleActive(rule: TransactionRule, active: Boolean) {
        viewModelScope.launch { ruleRepository.setActive(rule, active) }
    }

    fun delete(rule: TransactionRule) {
        viewModelScope.launch { ruleRepository.delete(rule) }
    }
}

class RulesViewModelFactory(private val ruleRepository: TransactionRuleRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RulesViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return RulesViewModel(ruleRepository) as T
    }
}
