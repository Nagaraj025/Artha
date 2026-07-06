package com.subramanya.artha.data.repository

import com.subramanya.artha.data.balance.BalanceCalculator
import com.subramanya.artha.data.dao.InvestmentDao
import com.subramanya.artha.data.dao.TransactionDao
import com.subramanya.artha.data.entity.InvestmentEntity
import com.subramanya.artha.data.entity.enums.ValuationMode
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.Investment
import com.subramanya.artha.domain.model.InvestmentWithMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

class InvestmentRepository(
    private val investmentDao: InvestmentDao,
    private val transactionDao: TransactionDao,
    private val scope: CoroutineScope,
) {

    fun observeAll(): Flow<List<Investment>> = investmentDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActive(): Flow<List<Investment>> = investmentDao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeArchived(): Flow<List<Investment>> = investmentDao.observeArchived().map { list -> list.map { it.toDomain() } }

    fun observeById(id: String): Flow<Investment?> = investmentDao.observeById(id).map { it?.toDomain() }

    private val activeWithMetrics: Flow<List<InvestmentWithMetrics>> =
        combine(investmentDao.observeActive(), transactionDao.observeAll()) { investments, txns ->
            // One pass over the log computes invested + interest for ALL investments.
            val totals = BalanceCalculator.computeInvestmentTotals(
                investments.associate { it.id to it.openingContribution },
                txns,
            )
            investments.map { entity -> metricsFor(entity, totals.getValue(entity.id)) }
        }.flowOn(Dispatchers.Default)
            .distinctUntilChanged()
            .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    fun observeActiveWithMetrics(): Flow<List<InvestmentWithMetrics>> = activeWithMetrics

    /**
     * Map of EVERY investment id (active AND archived) → its computed per-mode value
     * (MARKET → manual currentValue, DERIVED → contributions + posted interest).
     *
     * Single source of truth for "what an investment is worth" so aggregate consumers
     * (net worth, goals, reports) never re-implement the formula or sum the stale raw
     * `currentValue`. Covers all investments because consumers differ in scope —
     * dashboard/reports want active-only, goals filter by linked ids, search shows all —
     * so each consumer filters the map by the ids it cares about.
     */
    private val valuesByInvestmentId: Flow<Map<String, Double>> =
        combine(investmentDao.observeAll(), transactionDao.observeAll()) { investments, txns ->
            val totals = BalanceCalculator.computeInvestmentTotals(
                investments.associate { it.id to it.openingContribution },
                txns,
            )
            investments.associate { entity ->
                entity.id to valueFor(entity, totals.getValue(entity.id))
            }
        }.flowOn(Dispatchers.Default)
            .distinctUntilChanged()
            // Collected by dashboard, reports, goals AND search — share so the per-mode value map
            // is computed once per change instead of four times.
            .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    fun observeValuesByInvestmentId(): Flow<Map<String, Double>> = valuesByInvestmentId

    /**
     * The one place the per-mode value formula is applied, now from pre-computed [totals]
     * (so the transaction log is scanned once, not once per investment). Mirrors
     * [BalanceCalculator.computeInvestmentValue]'s MARKET/DERIVED branches.
     */
    private fun valueFor(entity: InvestmentEntity, totals: BalanceCalculator.InvestmentTotals): Double = when (entity.valuationMode) {
        ValuationMode.MARKET -> entity.currentValue
        ValuationMode.DERIVED -> totals.invested + totals.interest
    }

    /** Full metric bundle for one entity, from its pre-computed [totals]. */
    private fun metricsFor(entity: InvestmentEntity, totals: BalanceCalculator.InvestmentTotals): InvestmentWithMetrics {
        val invested = totals.invested
        val value = valueFor(entity, totals)
        val gain = value - invested
        val pct = if (invested == 0.0) Double.NaN else (gain / invested) * 100.0
        return InvestmentWithMetrics(
            investment = entity.toDomain(),
            investedAmount = invested,
            value = value,
            absoluteGain = gain,
            percentGain = pct,
        )
    }

    fun observeInvested(id: String): Flow<Double> = combine(investmentDao.observeById(id), transactionDao.observeAll()) { entity, txns ->
        BalanceCalculator.computeInvestmentInvested(id, txns, entity?.openingContribution ?: 0.0)
    }.flowOn(Dispatchers.Default).distinctUntilChanged()

    suspend fun getById(id: String): Investment? = investmentDao.getById(id)?.toDomain()
    suspend fun findByLinkedInsurance(insuranceId: String): Investment? = investmentDao.findByLinkedInsurance(insuranceId)?.toDomain()

    suspend fun upsert(investment: Investment) = investmentDao.upsert(investment.toEntity())
    suspend fun update(investment: Investment) = investmentDao.update(investment.toEntity())

    suspend fun archive(investment: Investment) = investmentDao.update(investment.toEntity().copy(isArchived = true))

    suspend fun restore(investment: Investment) = investmentDao.update(investment.toEntity().copy(isArchived = false))

    /** True if any transaction references this investment (source or destination) — a hard delete
     *  would orphan them, so callers must archive instead. */
    suspend fun hasReferencingTransactions(id: String): Boolean = transactionDao.countReferencing(id) > 0

    suspend fun delete(investment: Investment) = investmentDao.delete(investment.toEntity())
}
