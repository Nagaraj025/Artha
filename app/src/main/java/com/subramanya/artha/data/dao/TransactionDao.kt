package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.entity.TransactionPersonCrossRef
import com.subramanya.artha.data.entity.TransactionTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, created_at DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE date BETWEEN :startMillis AND :endMillis " +
            "ORDER BY date DESC, created_at DESC",
    )
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE source_id = :id OR destination_id = :id " +
            "ORDER BY date DESC, created_at DESC",
    )
    fun observeForSourceOrDestination(id: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    /** How many transactions reference [id] as their source or destination. Used to block a
     *  hard-delete of an account/card/investment that would orphan those transactions. */
    @Query("SELECT COUNT(*) FROM transactions WHERE source_id = :id OR destination_id = :id")
    suspend fun countReferencing(id: String): Int

    @Query("SELECT person_id FROM transaction_people WHERE transaction_id = :transactionId")
    suspend fun getPeopleIds(transactionId: String): List<String>

    @Query("SELECT tag_id FROM transaction_tags WHERE transaction_id = :transactionId")
    suspend fun getTagIds(transactionId: String): List<String>

    /** Whole-table snapshots used by Repository.observeAll/observeBetween to hydrate
     *  peopleIds + tagIds onto domain Transactions in a single round-trip. Without
     *  these, list flows return Transactions with peopleIds = [] and downstream
     *  consumers (PeopleScreen, future reports) silently see zero links. */
    @Query("SELECT * FROM transaction_people")
    fun observeAllPeopleLinks(): Flow<List<TransactionPersonCrossRef>>

    @Query("SELECT * FROM transaction_tags")
    fun observeAllTagLinks(): Flow<List<TransactionTagCrossRef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    /** Bulk insert used by the bank-statement importer. IGNORE so re-running with
     *  the same deterministic IDs is a no-op rather than overwriting user edits. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeopleLinks(links: List<TransactionPersonCrossRef>)

    @Query("DELETE FROM transaction_people WHERE transaction_id = :transactionId")
    suspend fun clearPeopleLinks(transactionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTagLinks(links: List<TransactionTagCrossRef>)

    @Query("DELETE FROM transaction_tags WHERE transaction_id = :transactionId")
    suspend fun clearTagLinks(transactionId: String)

    @Transaction
    suspend fun saveWithLinks(transaction: TransactionEntity, peopleIds: List<String>, tagIds: List<String>) {
        insertTransaction(transaction)
        clearPeopleLinks(transaction.id)
        clearTagLinks(transaction.id)
        if (peopleIds.isNotEmpty()) {
            insertPeopleLinks(peopleIds.map { TransactionPersonCrossRef(transaction.id, it) })
        }
        if (tagIds.isNotEmpty()) {
            insertTagLinks(tagIds.map { TransactionTagCrossRef(transaction.id, it) })
        }
    }
}
