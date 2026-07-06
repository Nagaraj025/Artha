package com.subramanya.artha.data.repository

import com.subramanya.artha.data.dao.InsuranceDao
import com.subramanya.artha.data.dao.InvestmentDao
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.Insurance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InsuranceRepository(private val insuranceDao: InsuranceDao, private val investmentDao: InvestmentDao) {

    fun observeAll(): Flow<List<Insurance>> = insuranceDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActive(): Flow<List<Insurance>> = insuranceDao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeArchived(): Flow<List<Insurance>> = insuranceDao.observeArchived().map { list -> list.map { it.toDomain() } }

    fun observeById(id: String): Flow<Insurance?> = insuranceDao.observeById(id).map { it?.toDomain() }

    /** Active policies with next premium due on or before [cutoffMillis]. */
    fun observeDueWithin(cutoffMillis: Long): Flow<List<Insurance>> =
        insuranceDao.observeDueWithin(cutoffMillis).map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): Insurance? = insuranceDao.getById(id)?.toDomain()
    suspend fun upsert(insurance: Insurance) = insuranceDao.upsert(insurance.toEntity())
    suspend fun update(insurance: Insurance) = insuranceDao.update(insurance.toEntity())

    suspend fun archive(insurance: Insurance) = insuranceDao.update(insurance.toEntity().copy(isArchived = true))

    suspend fun restore(insurance: Insurance) = insuranceDao.update(insurance.toEntity().copy(isArchived = false))

    suspend fun delete(insurance: Insurance) {
        // Clear any linked investment's back-link first so it isn't orphaned, then delete.
        investmentDao.unlinkInsurance(insurance.id)
        insuranceDao.delete(insurance.toEntity())
    }
}
