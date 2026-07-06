package com.subramanya.artha.data.repository

import com.subramanya.artha.data.dao.GoalDao
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.Goal
import com.subramanya.artha.domain.model.GoalWithProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime

class GoalRepository(
    private val goalDao: GoalDao,
    private val accountRepository: AccountRepository,
    private val investmentRepository: InvestmentRepository,
) {
    fun observeAll(): Flow<List<Goal>> = goalDao.observeAll().map { list -> list.map { it.toDomain() } }

    /** Joins each goal with its computed current amount (sum of linked-account
     *  balances + linked-investment current values) and days-remaining. */
    fun observeAllWithProgress(): Flow<List<GoalWithProgress>> = combine(
        goalDao.observeAll(),
        accountRepository.observeActiveWithBalances(),
        investmentRepository.observeActive(),
        investmentRepository.observeValuesByInvestmentId(),
    ) { goals, accountsWithBal, investments, investmentValuesById ->
        val tz = TimeZone.currentSystemDefault()
        val today = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            .toLocalDateTime(tz).date
        goals.map { entity ->
            val g = entity.toDomain()
            val acctSum = accountsWithBal
                .filter { it.account.id in g.linkedAccountIds }
                .sumOf { it.currentBalance }
            // Linkage + active-only scope preserved: only active linked investments count,
            // but each contributes its COMPUTED value (DERIVED → contributions + interest).
            val invSum = investments
                .filter { it.id in g.linkedInvestmentIds }
                .sumOf { investmentValuesById[it.id] ?: it.currentValue }
            val current = acctSum + invSum
            val pct = if (g.targetAmount == 0.0) 0.0 else (current / g.targetAmount) * 100.0
            val daysLeft = g.targetDate?.let {
                val target = Instant.fromEpochMilliseconds(it).toLocalDateTime(tz).date
                today.daysUntil(target)
            }
            GoalWithProgress(goal = g, currentAmount = current, percentDone = pct, daysRemaining = daysLeft)
        }
    }

    suspend fun upsert(goal: Goal) = goalDao.upsert(goal.toEntity())
    suspend fun delete(goal: Goal) = goalDao.delete(goal.toEntity())
}
