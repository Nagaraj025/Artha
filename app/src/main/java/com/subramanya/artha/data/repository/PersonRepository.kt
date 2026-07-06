package com.subramanya.artha.data.repository

import com.subramanya.artha.data.dao.PersonDao
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.Person
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PersonRepository(private val personDao: PersonDao) {

    fun observeAll(): Flow<List<Person>> = personDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): Person? = personDao.getById(id)?.toDomain()

    suspend fun upsert(person: Person) = personDao.upsert(person.toEntity())

    suspend fun update(person: Person) = personDao.update(person.toEntity())

    suspend fun delete(person: Person) = personDao.delete(person.toEntity())
}
