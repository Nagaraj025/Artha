package com.subramanya.artha.data.repository

import com.subramanya.artha.data.dao.TagDao
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepository(private val tagDao: TagDao) {

    fun observeAll(): Flow<List<Tag>> = tagDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): Tag? = tagDao.getById(id)?.toDomain()

    suspend fun usageCount(tagId: String): Int = tagDao.usageCount(tagId)

    suspend fun upsert(tag: Tag) = tagDao.upsert(tag.toEntity())

    suspend fun update(tag: Tag) = tagDao.update(tag.toEntity())

    suspend fun delete(tag: Tag) = tagDao.delete(tag.toEntity())
}
