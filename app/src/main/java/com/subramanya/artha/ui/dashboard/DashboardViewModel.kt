package com.subramanya.artha.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.balance.MonthlyAggregator
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.InsuranceRepository
import com.subramanya.artha.data.repository.InvestmentRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.utils.TimeRange
import com.subramanya.artha.utils.thisCalendarMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class DashboardViewModel(
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
    private val investmentRepository: InvestmentRepository,
    private val insuranceRepository: InsuranceRepository,
) : ViewModel() {

    /** User-controlled chip on the Recent strip: Today / Week / Month. */
    private val recentRange = MutableStateFlow(TimeRange.TODAY)

    /** Cheap secondary bag — combined with the core flow inside [state] to keep the
     *  primary combine() under the 5-arg ceiling without nesting two layers. */
    private val phase2Bag = combine(
        investmentRepository.observeActive(),
        investmentRepository.observeValuesByInvestmentId(),
        insuranceRepository.observeDueWithin(weekFromNow()),
    ) { investments, valuesById, duePolicies ->
        Phase2Bag(
            // Active-only scope preserved: sum each active investment's COMPUTED value
            // (DERIVED rows reflect contributions + interest, not the stale currentValue).
            investmentTotalValue = investments.sumOf { valuesById[it.id] ?: it.currentValue },
            premiumsDueThisWeek = duePolicies,
        )
    }

    val state: StateFlow<DashboardUiState> = combine(
        combine(
            accountRepository.observeActiveWithBalances(),
            cardRepository.observeActiveWithBalances(),
            monthlyTotalsFlow(),
            recentTransactionsFlow(),
            recentRange,
        ) { accounts, cards, monthly, recent, range ->
            CoreBag(accounts, cards, monthly, recent, range)
        },
        combine(phase2Bag, transactionRepository.observeAll()) { p2, allTxns ->
            Phase2BagWithTxns(p2, allTxns)
        },
    ) { core, p2withTxns ->
        // Net position = sum of bank/cash balances minus credit-card outstanding
        // plus current investment value. Liquid + paper wealth on one number.
        val accountSum = core.accounts.sumOf { it.currentBalance }
        val cardOutstandingSum = core.cards.sumOf { it.currentOutstanding }
        val netPosition = accountSum - cardOutstandingSum + p2withTxns.bag.investmentTotalValue

        // 30-day daily-net spark: replay account+card balances from history.
        // Cheap approximation — assumes investments are flat in the window (we
        // don't track historical NAV). Good enough to show shape.
        val spark = buildNetPositionSpark(
            accounts = core.accounts,
            cards = core.cards,
            investmentValue = p2withTxns.bag.investmentTotalValue,
            transactions = p2withTxns.transactions,
            days = SPARK_DAYS,
        )
        val netChange = core.monthly.income - core.monthly.expense

        DashboardUiState(
            isLoading = false,
            netPosition = netPosition,
            accountCount = core.accounts.size,
            cardCount = core.cards.size,
            monthlyTotals = core.monthly,
            accounts = core.accounts,
            cards = core.cards,
            recentRange = core.range,
            recentTransactions = core.recent,
            premiumsDueThisWeek = p2withTxns.bag.premiumsDueThisWeek,
            investmentTotalValue = p2withTxns.bag.investmentTotalValue,
            netPositionSpark = spark,
            netChangeThisMonth = netChange,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    /** End-of-day net position for the last [days] days, oldest first. Reuses
     *  the existing BalanceCalculator math so the sparkline can never drift
     *  from the displayed Net Position number. */
    private fun buildNetPositionSpark(
        accounts: List<com.subramanya.artha.domain.model.AccountWithBalance>,
        cards: List<com.subramanya.artha.domain.model.CardWithBalance>,
        investmentValue: Double,
        transactions: List<com.subramanya.artha.domain.model.Transaction>,
        days: Int,
    ): List<Double> {
        if (accounts.isEmpty() && cards.isEmpty() && investmentValue == 0.0) return emptyList()
        val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
        val today = kotlinx.datetime.Instant.fromEpochMilliseconds(
            Clock.System.now().toEpochMilliseconds(),
        ).toLocalDateTime(tz).date
        val entities = transactions.map { it.toEntity() }
        val acctIds = accounts.map { it.account.id to it.account.openingBalance }
        val cardIds = cards.map { it.card.id }
        val points = ArrayList<Double>(days)
        for (daysAgo in (days - 1) downTo 0) {
            val day = today.minus(daysAgo, kotlinx.datetime.DateTimeUnit.DAY)
            val nextDayStart = day.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
                .atStartOfDayIn(tz).toEpochMilliseconds()
            val cutoff = nextDayStart - 1
            val upTo = entities.filter { it.date <= cutoff }
            val acctSum = acctIds.sumOf { (id, opening) ->
                com.subramanya.artha.data.balance.BalanceCalculator
                    .computeAccountBalance(opening, id, upTo)
            }
            val cardSum = cardIds.sumOf { id ->
                com.subramanya.artha.data.balance.BalanceCalculator
                    .computeCardOutstanding(id, upTo)
            }
            points.add(acctSum - cardSum + investmentValue)
        }
        // Hide a flat-zero line — it adds no signal.
        if (points.all { it == 0.0 }) return emptyList()
        return points
    }

    private data class CoreBag(
        val accounts: List<com.subramanya.artha.domain.model.AccountWithBalance>,
        val cards: List<com.subramanya.artha.domain.model.CardWithBalance>,
        val monthly: com.subramanya.artha.data.balance.MonthlyTotals,
        val recent: List<com.subramanya.artha.domain.model.Transaction>,
        val range: TimeRange,
    )
    private data class Phase2Bag(
        val investmentTotalValue: Double,
        val premiumsDueThisWeek: List<com.subramanya.artha.domain.model.Insurance>,
    )
    private data class Phase2BagWithTxns(val bag: Phase2Bag, val transactions: List<com.subramanya.artha.domain.model.Transaction>)

    private fun weekFromNow(): Long = Clock.System.now().toEpochMilliseconds() + WEEK_MILLIS

    fun onRecentRangeChanged(range: TimeRange) {
        recentRange.update { range }
    }

    private fun monthlyTotalsFlow() = transactionRepository.observeAll()
        .map { domainList ->
            val month = thisCalendarMonth()
            // Map back to entity so MonthlyAggregator can stay pure-data; the round-trip
            // is cheap and avoids leaking enum sets into the domain layer.
            val inMonth = domainList
                .filter { it.date in month.startMillis..month.endMillis }
                .map { it.toEntity() }
            MonthlyAggregator.aggregate(inMonth)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun recentTransactionsFlow() = recentRange.flatMapLatest { range ->
        val window = range.toRange()
        transactionRepository.observeBetween(window.startMillis, window.endMillis)
            .map { list -> list.take(RECENT_TRANSACTION_LIMIT) }
    }

    private companion object {
        private const val RECENT_TRANSACTION_LIMIT: Int = 10
        private const val WEEK_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
        private const val SPARK_DAYS: Int = 30
    }
}

class DashboardViewModelFactory(
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
    private val investmentRepository: InvestmentRepository,
    private val insuranceRepository: InsuranceRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return DashboardViewModel(
            accountRepository,
            cardRepository,
            transactionRepository,
            investmentRepository,
            insuranceRepository,
        ) as T
    }
}
