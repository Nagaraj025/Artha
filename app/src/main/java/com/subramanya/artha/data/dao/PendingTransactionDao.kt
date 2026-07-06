package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.subramanya.artha.data.entity.PendingSmsTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {
    @Query("SELECT * FROM pending_sms_transactions ORDER BY received_at DESC")
    fun observeAll(): Flow<List<PendingSmsTransactionEntity>>

    @Query("SELECT COUNT(*) FROM pending_sms_transactions")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingSmsTransactionEntity)

    @Query("DELETE FROM pending_sms_transactions WHERE id = :id")
    suspend fun deleteById(id: String)
}
