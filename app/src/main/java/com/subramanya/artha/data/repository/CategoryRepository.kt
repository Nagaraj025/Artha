package com.subramanya.artha.data.repository

import com.subramanya.artha.data.dao.CategoryDao
import com.subramanya.artha.data.entity.enums.CategoryType
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun observeAll(): Flow<List<Category>> = categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeByType(type: CategoryType): Flow<List<Category>> = categoryDao.observeByType(type).map { list -> list.map { it.toDomain() } }

    fun observeRoots(): Flow<List<Category>> = categoryDao.observeRoots().map { list -> list.map { it.toDomain() } }

    fun observeChildren(parentId: String): Flow<List<Category>> =
        categoryDao.observeChildren(parentId).map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): Category? = categoryDao.getById(id)?.toDomain()

    suspend fun usageCount(categoryId: String): Int = categoryDao.usageCount(categoryId)

    suspend fun upsert(category: Category) = categoryDao.upsert(category.toEntity())

    suspend fun update(category: Category) = categoryDao.update(category.toEntity())

    suspend fun delete(category: Category) = categoryDao.delete(category.toEntity())
}
