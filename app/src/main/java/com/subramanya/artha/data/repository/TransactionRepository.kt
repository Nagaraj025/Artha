package com.subramanya.artha.data.repository

import com.subramanya.artha.data.dao.TransactionDao
import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.entity.TransactionPersonCrossRef
import com.subramanya.artha.data.entity.TransactionTagCrossRef
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun observeAll(): Flow<List<Transaction>> = hydrate(transactionDao.observeAll())

    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<Transaction>> =
        hydrate(transactionDao.observeBetween(startMillis, endMillis))

    fun observeForAccountOrCard(id: String): Flow<List<Transaction>> = hydrate(transactionDao.observeForSourceOrDestination(id))

    fun observeById(id: String): Flow<Transaction?> = transactionDao.observeById(id).map { it?.toDomain() }

    /**
     * Joins the entity stream with whole-table snapshots of the cross-ref tables so
     * each emitted domain Transaction carries its peopleIds / tagIds. Previously the
     * list flows returned `peopleIds = []` for every row — PeopleScreen's per-person
     * balance always showed ₹0 as a result, and any future per-tag report would have
     * been silently empty too. The two extra Flows are full-table reads but the
     * cross-ref tables are tiny (one row per (txn, person) edge), so the cost is
     * negligible compared to the bug it prevents.
     */
    private fun hydrate(source: Flow<List<TransactionEntity>>): Flow<List<Transaction>> = combine(
        source,
        transactionDao.observeAllPeopleLinks(),
        transactionDao.observeAllTagLinks(),
    ) { entities, peopleLinks, tagLinks ->
        val peopleByTxn: Map<String, List<String>> = peopleLinks
            .groupBy(TransactionPersonCrossRef::transactionId, TransactionPersonCrossRef::personId)
        val tagsByTxn: Map<String, List<String>> = tagLinks
            .groupBy(TransactionTagCrossRef::transactionId, TransactionTagCrossRef::tagId)
        entities.map { entity ->
            entity.toDomain(
                peopleIds = peopleByTxn[entity.id].orEmpty(),
                tagIds = tagsByTxn[entity.id].orEmpty(),
            )
        }
    }

    /** Hydrates peopleIds and tagIds from the cross-ref tables (single-row fetch). */
    suspend fun getById(id: String): Transaction? {
        val entity = transactionDao.getById(id) ?: return null
        val peopleIds = transactionDao.getPeopleIds(id)
        val tagIds = transactionDao.getTagIds(id)
        return entity.toDomain(peopleIds = peopleIds, tagIds = tagIds)
    }

    /**
     * Insert-or-update + replace the people/tag cross-refs atomically.
     * Use this from any flow that adds or edits a transaction.
     */
    suspend fun save(transaction: Transaction) {
        transactionDao.saveWithLinks(
            transaction = transaction.toEntity(),
            peopleIds = transaction.peopleIds,
            tagIds = transaction.tagIds,
        )
    }

    suspend fun delete(transaction: Transaction) = transactionDao.deleteTransaction(transaction.toEntity())

    suspend fun deleteByIds(ids: List<String>) = transactionDao.deleteByIds(ids)
}
