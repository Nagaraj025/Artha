package com.subramanya.artha.domain.model

enum class SmsDirection { DEBIT, CREDIT }

data class PendingSmsTransaction(
    val id: String,
    val rawSmsBody: String,
    val sender: String,
    val receivedAt: Long,
    val direction: SmsDirection,
    val amount: Double,
    val accountHint: String?,
    val merchant: String?,
    val suggestedCategoryId: String?,
)
