package com.subramanya.artha.data.repository

import com.subramanya.artha.data.balance.BalanceCalculator
import com.subramanya.artha.data.dao.AccountDao
import com.subramanya.artha.data.dao.TransactionDao
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.AccountWithBalance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

class AccountRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val scope: CoroutineScope,
) {

    fun observeAll(): Flow<List<Account>> = accountDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActive(): Flow<List<Account>> = accountDao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeArchived(): Flow<List<Account>> = accountDao.observeArchived().map { list -> list.map { it.toDomain() } }

    fun observeById(id: String): Flow<Account?> = accountDao.observeById(id).map { it?.toDomain() }

    fun observeCurrentBalance(accountId: String): Flow<Double> =
        combine(accountDao.observeById(accountId), transactionDao.observeAll()) { account, txns ->
            if (account == null) {
                0.0
            } else {
                BalanceCalculator.computeAccountBalance(account.openingBalance, accountId, txns)
            }
        }.flowOn(Dispatchers.Default).distinctUntilChanged()

    // Shared across all consumers (dashboard, reports, goals) so the single-pass balance compute
    // runs ONCE per change, not once per screen. WhileSubscribed → idle when nothing observes.
    private val activeWithBalances: Flow<List<AccountWithBalance>> =
        combine(accountDao.observeActive(), transactionDao.observeAll()) { accounts, txns ->
            // Single pass over the whole log for ALL accounts (O(txns + accounts)) instead of
            // one full scan per account, and off the main thread via flowOn below.
            val balances = BalanceCalculator.computeAccountBalances(
                accounts.associate { it.id to it.openingBalance },
                txns,
            )
            accounts.map { entity ->
                AccountWithBalance(
                    account = entity.toDomain(),
                    currentBalance = balances.getValue(entity.id),
                )
            }
        }.flowOn(Dispatchers.Default)
            .distinctUntilChanged()
            .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    fun observeActiveWithBalances(): Flow<List<AccountWithBalance>> = activeWithBalances

    suspend fun getById(id: String): Account? = accountDao.getById(id)?.toDomain()

    suspend fun upsert(account: Account) = accountDao.upsert(account.toEntity())

    suspend fun upsertAll(accounts: List<Account>) = accountDao.upsertAll(accounts.map(Account::toEntity))

    suspend fun update(account: Account) = accountDao.update(account.toEntity())

    suspend fun archive(account: Account) = accountDao.update(account.toEntity().copy(isArchived = true))

    suspend fun restore(account: Account) = accountDao.update(account.toEntity().copy(isArchived = false))

    /** True if any transaction references this account (source or destination) — a hard delete
     *  would orphan them, so callers must archive instead. */
    suspend fun hasReferencingTransactions(id: String): Boolean = transactionDao.countReferencing(id) > 0

    suspend fun delete(account: Account) = accountDao.delete(account.toEntity())
}
