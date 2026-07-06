package com.subramanya.artha.data.mapper

import com.subramanya.artha.data.entity.PersonEntity
import com.subramanya.artha.domain.model.Person

fun PersonEntity.toDomain(): Person = Person(
    id = id,
    name = name,
    relation = relation,
    contact = contact,
    avatarUri = avatarUri,
    createdAt = createdAt,
)

fun Person.toEntity(): PersonEntity = PersonEntity(
    id = id,
    name = name,
    relation = relation,
    contact = contact,
    avatarUri = avatarUri,
    createdAt = createdAt,
)
