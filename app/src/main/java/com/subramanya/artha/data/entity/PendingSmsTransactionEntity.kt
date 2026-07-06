package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sms_transactions")
data class PendingSmsTransactionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "raw_sms_body")
    val rawSmsBody: String,
    val sender: String,
    @ColumnInfo(name = "received_at")
    val receivedAt: Long,
    /** Stored as the enum name ("DEBIT"/"CREDIT") — see [com.subramanya.artha.domain.model.SmsDirection]. */
    val direction: String,
    val amount: Double,
    @ColumnInfo(name = "account_hint")
    val accountHint: String?,
    val merchant: String?,
    @ColumnInfo(name = "suggested_category_id")
    val suggestedCategoryId: String?,
)
