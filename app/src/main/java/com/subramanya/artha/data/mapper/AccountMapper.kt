package com.subramanya.artha.data.mapper

import com.subramanya.artha.data.entity.AccountEntity
import com.subramanya.artha.domain.model.Account

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    type = type,
    institution = institution,
    accountNumberLast4 = accountNumberLast4,
    openingBalance = openingBalance,
    currency = currency,
    icon = icon,
    color = color,
    isArchived = isArchived,
    displayOrder = displayOrder,
    createdAt = createdAt,
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    type = type,
    institution = institution,
    accountNumberLast4 = accountNumberLast4,
    openingBalance = openingBalance,
    currency = currency,
    icon = icon,
    color = color,
    isArchived = isArchived,
    displayOrder = displayOrder,
    createdAt = createdAt,
)
