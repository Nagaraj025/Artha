package com.subramanya.artha.data.mapper

import com.subramanya.artha.data.entity.CardEntity
import com.subramanya.artha.domain.model.Card

fun CardEntity.toDomain(): Card = Card(
    id = id,
    name = name,
    type = type,
    issuer = issuer,
    network = network,
    cardNumberLast4 = cardNumberLast4,
    creditLimit = creditLimit,
    statementDayOfMonth = statementDayOfMonth,
    dueDayOfMonth = dueDayOfMonth,
    linkedAccountId = linkedAccountId,
    icon = icon,
    color = color,
    isArchived = isArchived,
    displayOrder = displayOrder,
    createdAt = createdAt,
)

fun Card.toEntity(): CardEntity = CardEntity(
    id = id,
    name = name,
    type = type,
    issuer = issuer,
    network = network,
    cardNumberLast4 = cardNumberLast4,
    creditLimit = creditLimit,
    statementDayOfMonth = statementDayOfMonth,
    dueDayOfMonth = dueDayOfMonth,
    linkedAccountId = linkedAccountId,
    icon = icon,
    color = color,
    isArchived = isArchived,
    displayOrder = displayOrder,
    createdAt = createdAt,
)
