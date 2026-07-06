package com.subramanya.artha.data.repository

import com.subramanya.artha.data.dao.RecurringRuleDao
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.RecurringRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecurringRuleRepository(private val recurringRuleDao: RecurringRuleDao) {
    fun observeAll(): Flow<List<RecurringRule>> = recurringRuleDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun dueBy(cutoffMillis: Long): List<RecurringRule> = recurringRuleDao.dueBy(cutoffMillis).map { it.toDomain() }

    suspend fun upsert(rule: RecurringRule) = recurringRuleDao.upsert(rule.toEntity())
    suspend fun delete(rule: RecurringRule) = recurringRuleDao.delete(rule.toEntity())
}
