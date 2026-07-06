package com.subramanya.artha.data.mapper

import com.subramanya.artha.data.entity.PendingSmsTransactionEntity
import com.subramanya.artha.domain.model.PendingSmsTransaction
import com.subramanya.artha.domain.model.SmsDirection

fun PendingSmsTransactionEntity.toDomain(): PendingSmsTransaction = PendingSmsTransaction(
    id = id,
    rawSmsBody = rawSmsBody,
    sender = sender,
    receivedAt = receivedAt,
    direction = SmsDirection.valueOf(direction),
    amount = amount,
    accountHint = accountHint,
    merchant = merchant,
    suggestedCategoryId = suggestedCategoryId,
)

fun PendingSmsTransaction.toEntity(): PendingSmsTransactionEntity = PendingSmsTransactionEntity(
    id = id,
    rawSmsBody = rawSmsBody,
    sender = sender,
    receivedAt = receivedAt,
    direction = direction.name,
    amount = amount,
    accountHint = accountHint,
    merchant = merchant,
    suggestedCategoryId = suggestedCategoryId,
)
