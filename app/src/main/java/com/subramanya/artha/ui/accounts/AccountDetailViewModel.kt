package com.subramanya.artha.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.balance.BalanceCalculator
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class AccountDetailUiState(
    val account: Account? = null,
    val currentBalance: Double = 0.0,
    val totalIn: Double = 0.0,
    val totalOut: Double = 0.0,
    val transactions: List<Transaction> = emptyList(),
    /** End-of-day balance for the last [CHART_DAYS] days, oldest → newest. */
    val chartPoints: List<Double> = emptyList(),
    val showArchiveConfirm: Boolean = false,
    val showDeleteConfirm: Boolean = false,
)

class AccountDetailViewModel(
    private val accountId: String,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val showArchiveConfirm = MutableStateFlow(false)
    private val showDeleteConfirm = MutableStateFlow(false)

    val state: StateFlow<AccountDetailUiState> = combine(
        accountRepository.observeById(accountId),
        transactionRepository.observeForAccountOrCard(accountId),
        showArchiveConfirm.asStateFlow(),
        showDeleteConfirm.asStateFlow(),
    ) { account, transactions, archiveConfirm, deleteConfirm ->
        if (account == null) {
            return@combine AccountDetailUiState(
                showArchiveConfirm = archiveConfirm,
                showDeleteConfirm = deleteConfirm,
            )
        }
        val entities = transactions.map { it.toEntity() }
        val balance = BalanceCalculator.computeAccountBalance(
            openingBalance = account.openingBalance,
            accountId = accountId,
            transactions = entities,
        )
        val (inSum, outSum) = totalsFor(account, transactions)
        val chart = chartPointsLast30Days(account, entities)
        AccountDetailUiState(
            account = account,
            currentBalance = balance,
            totalIn = inSum,
            totalOut = outSum,
            transactions = transactions,
            chartPoints = chart,
            showArchiveConfirm = archiveConfirm,
            showDeleteConfirm = deleteConfirm,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountDetailUiState())

    fun requestArchive() {
        if (state.value.account != null) showArchiveConfirm.update { true }
    }

    fun dismissArchiveConfirm() = showArchiveConfirm.update { false }

    fun confirmArchive(onArchived: () -> Unit) {
        val current = state.value.account ?: return
        viewModelScope.launch {
            accountRepository.archive(current)
            showArchiveConfirm.update { false }
            onArchived()
        }
    }

    fun restore(onRestored: () -> Unit) {
        val current = state.value.account ?: return
        viewModelScope.launch {
            accountRepository.restore(current)
            onRestored()
        }
    }

    fun requestDelete() {
        if (state.value.account != null) showDeleteConfirm.update { true }
    }

    fun dismissDeleteConfirm() = showDeleteConfirm.update { false }

    fun confirmDelete(onDeleted: () -> Unit) {
        val current = state.value.account ?: return
        // Never hard-delete an account that still has transactions — doing so would orphan
        // them (dangling source/destination ids that still distort reports and balances).
        // The UI routes the user to Archive instead; this is the defensive backstop.
        if (state.value.transactions.isNotEmpty()) {
            showDeleteConfirm.update { false }
            return
        }
        viewModelScope.launch {
            accountRepository.delete(current)
            showDeleteConfirm.update { false }
            onDeleted()
        }
    }

    // ---------- pure helpers ----------

    /**
     * "Total in" and "Total out" are lifetime totals for this account from the
     * transaction log. Transfers in/out count: this account either gained or lost
     * money, so it belongs in the totals even though MonthlyAggregator excludes
     * transfers from the global P&L view.
     */
    private fun totalsFor(account: Account, txns: List<Transaction>): Pair<Double, Double> {
        var income = 0.0
        var outgo = 0.0
        for (txn in txns) {
            val sourceMatches = txn.sourceType == SourceKind.ACCOUNT && txn.sourceId == account.id
            val destMatches = txn.destinationType == SourceKind.ACCOUNT && txn.destinationId == account.id
            if (sourceMatches) {
                when (txn.type) {
                    in MONEY_INTO_SOURCE -> income += txn.amount
                    in MONEY_OUT_OF_SOURCE -> outgo += txn.amount
                    TransactionType.ADJUSTMENT -> if (txn.amount >= 0) income += txn.amount else outgo += -txn.amount
                    else -> Unit
                }
            }
            if (destMatches && txn.type in MONEY_INTO_DESTINATION) {
                income += txn.amount
            }
        }
        return income to outgo
    }

    /**
     * End-of-day balance for each of the last [CHART_DAYS] days, oldest first. Reuses
     * [BalanceCalculator.computeAccountBalance] so chart math can never drift from the
     * canonical balance rules. O(days * txns) — fine for Phase 1 transaction volumes.
     */
    private fun chartPointsLast30Days(account: Account, entities: List<com.subramanya.artha.data.entity.TransactionEntity>): List<Double> {
        val today = Instant.fromEpochMilliseconds(clock()).toLocalDateTime(timeZone).date
        val out = ArrayList<Double>(CHART_DAYS)
        for (daysAgo in (CHART_DAYS - 1) downTo 0) {
            val day = today.minus(daysAgo, DateTimeUnit.DAY)
            // Inclusive end-of-day: start of next day minus 1 ms.
            val nextDayStart = day.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds()
            val cutoff = nextDayStart - 1
            val upTo = entities.filter { it.date <= cutoff }
            out.add(BalanceCalculator.computeAccountBalance(account.openingBalance, account.id, upTo))
        }
        // Hide flat-zero charts — they say nothing useful and waste the slot.
        if (account.openingBalance == 0.0 && out.all { it == 0.0 }) return emptyList()
        return out
    }

    private companion object {
        const val CHART_DAYS: Int = 30

        val MONEY_OUT_OF_SOURCE: Set<TransactionType> = setOf(
            TransactionType.EXPENSE,
            TransactionType.LOAN_GIVEN,
            TransactionType.GIFT_SENT,
            TransactionType.INVESTMENT_BUY,
            TransactionType.TRANSFER,
            TransactionType.CARD_PAYMENT,
        )
        val MONEY_INTO_SOURCE: Set<TransactionType> = setOf(
            TransactionType.INCOME,
            TransactionType.REFUND,
            TransactionType.CASHBACK,
            TransactionType.INTEREST,
            TransactionType.LOAN_RECEIVED,
            TransactionType.GIFT_RECEIVED,
            TransactionType.INVESTMENT_SELL,
        )
        val MONEY_INTO_DESTINATION: Set<TransactionType> = setOf(
            TransactionType.TRANSFER,
            TransactionType.CARD_PAYMENT,
        )
    }
}

class AccountDetailViewModelFactory(
    private val accountId: String,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AccountDetailViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return AccountDetailViewModel(accountId, accountRepository, transactionRepository) as T
    }
}
