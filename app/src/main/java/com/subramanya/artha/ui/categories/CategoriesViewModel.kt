package com.subramanya.artha.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.entity.enums.CategoryType
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.domain.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages the Categories tree. The visible type is user-toggleable so the tree
 * never has to render all 4 types at once. Expand/collapse state is preserved per
 * VM lifetime — leaving and re-entering this screen forgets state, which is fine
 * for Phase 1 (and matches the "fresh look every time" mental model).
 */
class CategoriesViewModel(private val categoryRepository: CategoryRepository) : ViewModel() {

    private val selectedType = MutableStateFlow(CategoryType.EXPENSE)
    private val expanded = MutableStateFlow<Set<String>>(emptySet())

    val state: StateFlow<CategoriesUiState> = combine(
        categoryRepository.observeAll(),
        selectedType,
        expanded,
    ) { all, type, expandedIds ->
        val typed = all.filter { it.type == type }
        val parents = typed.filter { it.parentId == null }.sortedBy { it.displayOrder }
        val childrenByParent = typed
            .filter { it.parentId != null }
            .groupBy { it.parentId!! }
            .mapValues { (_, list) -> list.sortedBy { it.displayOrder } }
        CategoriesUiState(
            type = type,
            parents = parents,
            childrenByParent = childrenByParent,
            expandedParentIds = expandedIds,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun onTypeSelected(type: CategoryType) {
        selectedType.update { type }
        expanded.update { emptySet() } // collapse when switching tabs
    }

    fun toggleExpanded(parentId: String) {
        expanded.update { current ->
            if (parentId in current) current - parentId else current + parentId
        }
    }

    suspend fun usageCount(id: String): Int = categoryRepository.usageCount(id)

    fun upsert(category: Category) {
        viewModelScope.launch { categoryRepository.upsert(category) }
    }

    fun delete(category: Category) {
        viewModelScope.launch { categoryRepository.delete(category) }
    }
}

class CategoriesViewModelFactory(private val categoryRepository: CategoryRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CategoriesViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return CategoriesViewModel(categoryRepository) as T
    }
}
