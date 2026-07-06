package com.subramanya.artha.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sms_transactions")
data class PendingSmsTransactionEntity(
    @PrimaryKey
    val id: String,
    val rawSmsBody: String,
    val sender: String,
    val receivedAt: Long,
    /** Stored as the enum name ("DEBIT"/"CREDIT") — see [com.subramanya.artha.domain.model.SmsDirection]. */
    val direction: String,
    val amount: Double,
    val accountHint: String?,
    val merchant: String?,
    val suggestedCategoryId: String?,
)
