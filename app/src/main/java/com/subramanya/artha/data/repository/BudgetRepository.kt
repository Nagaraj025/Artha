package com.subramanya.artha.data.repository

import com.subramanya.artha.data.balance.BudgetCalculator
import com.subramanya.artha.data.dao.BudgetDao
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.Budget
import com.subramanya.artha.domain.model.BudgetWithProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class BudgetRepository(private val budgetDao: BudgetDao, private val transactionRepository: TransactionRepository) {
    fun observeAll(): Flow<List<Budget>> = budgetDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActiveWithProgress(): Flow<List<BudgetWithProgress>> =
        combine(budgetDao.observeActive(), transactionRepository.observeAll()) { budgets, txns ->
            budgets.map { entity ->
                val budget = entity.toDomain()
                val bounds = BudgetCalculator.currentPeriod(budget.period)
                BudgetWithProgress(
                    budget = budget,
                    spent = BudgetCalculator.spentIn(budget, bounds, txns),
                    daysRemainingInPeriod = bounds.daysRemaining,
                )
            }
        }

    suspend fun upsert(budget: Budget) = budgetDao.upsert(budget.toEntity())
    suspend fun delete(budget: Budget) = budgetDao.delete(budget.toEntity())
}
