package com.subramanya.artha.data.repository

import com.subramanya.artha.data.dao.PendingTransactionDao
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.PendingSmsTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PendingTransactionRepository(private val dao: PendingTransactionDao) {

    fun observeAll(): Flow<List<PendingSmsTransaction>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun insert(pending: PendingSmsTransaction) = dao.insert(pending.toEntity())

    suspend fun dismiss(id: String) = dao.deleteById(id)
}
