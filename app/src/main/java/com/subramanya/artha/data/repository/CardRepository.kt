package com.subramanya.artha.data.repository

import com.subramanya.artha.data.balance.BalanceCalculator
import com.subramanya.artha.data.dao.CardDao
import com.subramanya.artha.data.dao.TransactionDao
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.CardWithBalance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

class CardRepository(private val cardDao: CardDao, private val transactionDao: TransactionDao, private val scope: CoroutineScope) {

    fun observeAll(): Flow<List<Card>> = cardDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActive(): Flow<List<Card>> = cardDao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeArchived(): Flow<List<Card>> = cardDao.observeArchived().map { list -> list.map { it.toDomain() } }

    fun observeById(id: String): Flow<Card?> = cardDao.observeById(id).map { it?.toDomain() }

    fun observeCurrentOutstanding(cardId: String): Flow<Double> = transactionDao.observeAll().map { txns ->
        BalanceCalculator.computeCardOutstanding(cardId, txns)
    }.flowOn(Dispatchers.Default).distinctUntilChanged()

    // Shared across consumers (dashboard, reports) so the single-pass compute runs once per change.
    private val activeWithBalances: Flow<List<CardWithBalance>> =
        combine(cardDao.observeActive(), transactionDao.observeAll()) { cards, txns ->
            // One pass over the log for ALL cards (O(txns + cards)), off the main thread.
            val outstanding = BalanceCalculator.computeCardOutstandings(cards.map { it.id }, txns)
            cards.map { entity ->
                CardWithBalance(
                    card = entity.toDomain(),
                    currentOutstanding = outstanding.getValue(entity.id),
                )
            }
        }.flowOn(Dispatchers.Default)
            .distinctUntilChanged()
            .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    fun observeActiveWithBalances(): Flow<List<CardWithBalance>> = activeWithBalances

    suspend fun getById(id: String): Card? = cardDao.getById(id)?.toDomain()

    suspend fun upsert(card: Card) = cardDao.upsert(card.toEntity())

    suspend fun update(card: Card) = cardDao.update(card.toEntity())

    suspend fun archive(card: Card) = cardDao.update(card.toEntity().copy(isArchived = true))

    suspend fun restore(card: Card) = cardDao.update(card.toEntity().copy(isArchived = false))

    /** True if any transaction references this card (source or destination) — a hard delete
     *  would orphan them, so callers must archive instead. */
    suspend fun hasReferencingTransactions(id: String): Boolean = transactionDao.countReferencing(id) > 0

    suspend fun delete(card: Card) = cardDao.delete(card.toEntity())
}
