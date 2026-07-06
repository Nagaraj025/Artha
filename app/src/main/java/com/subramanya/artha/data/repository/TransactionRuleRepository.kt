package com.subramanya.artha.data.repository

import com.subramanya.artha.data.dao.TransactionRuleDao
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.TransactionRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRuleRepository(private val ruleDao: TransactionRuleDao) {

    fun observeAll(): Flow<List<TransactionRule>> = ruleDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActive(): Flow<List<TransactionRule>> = ruleDao.observeActive().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): TransactionRule? = ruleDao.getById(id)?.toDomain()

    suspend fun upsert(rule: TransactionRule) = ruleDao.upsert(rule.toEntity())

    suspend fun setActive(rule: TransactionRule, active: Boolean) = ruleDao.update(rule.copy(isActive = active).toEntity())

    suspend fun delete(rule: TransactionRule) = ruleDao.delete(rule.toEntity())
}
