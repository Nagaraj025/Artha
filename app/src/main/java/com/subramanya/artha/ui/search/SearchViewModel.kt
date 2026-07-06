package com.subramanya.artha.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.InsuranceRepository
import com.subramanya.artha.data.repository.InvestmentRepository
import com.subramanya.artha.data.repository.PersonRepository
import com.subramanya.artha.data.repository.TagRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.Insurance
import com.subramanya.artha.domain.model.Investment
import com.subramanya.artha.domain.model.Person
import com.subramanya.artha.domain.model.Tag
import com.subramanya.artha.domain.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

/**
 * Result groups returned by global search. Each list is capped so the screen
 * never has to render hundreds of rows on a single-letter query.
 */
data class SearchResults(
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val cards: List<Card> = emptyList(),
    val people: List<Person> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val investments: List<Investment> = emptyList(),
    val insurances: List<Insurance> = emptyList(),
    /** Computed per-mode value keyed by investment id, for displaying live worth. */
    val investmentValuesById: Map<String, Double> = emptyMap(),
) {
    val isEmpty: Boolean = transactions.isEmpty() && accounts.isEmpty() && cards.isEmpty() &&
        people.isEmpty() && categories.isEmpty() && tags.isEmpty() &&
        investments.isEmpty() && insurances.isEmpty()

    val total: Int = transactions.size + accounts.size + cards.size + people.size +
        categories.size + tags.size + investments.size + insurances.size
}

data class SearchUiState(val query: String = "", val results: SearchResults = SearchResults(), val isEmpty: Boolean = true)

/**
 * Global search across the user's entire ledger. Lights up everything that
 * substring-matches the query (case-insensitive). For transactions we also
 * accept a numeric query like "1500" — those match the whole-rupee amount.
 *
 * Each repository is observed live, so a transaction added in another tab
 * shows up here without a manual refresh. Capping at [MAX_PER_GROUP] keeps
 * the screen responsive on a "the" or "₹" search.
 */
class SearchViewModel(
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    cardRepository: CardRepository,
    personRepository: PersonRepository,
    categoryRepository: CategoryRepository,
    tagRepository: TagRepository,
    investmentRepository: InvestmentRepository,
    insuranceRepository: InsuranceRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * Heavy outer combine — when nothing's searched yet, results are empty
     * and the inner repository scans don't fire. Wrapped lists are stored in
     * a holder data class because Flow.combine caps at 5 sources.
     */
    private data class Bag(
        val txns: List<Transaction>,
        val accounts: List<Account>,
        val cards: List<Card>,
        val people: List<Person>,
        val rest: Rest,
    )
    private data class Rest(
        val categories: List<Category>,
        val tags: List<Tag>,
        val investments: List<Investment>,
        val insurances: List<Insurance>,
        /** id → computed per-mode value, so a DERIVED row shows live worth not stale currentValue. */
        val investmentValuesById: Map<String, Double>,
    )

    // Fold the investments list with its computed-value map first, keeping the outer
    // combine inside the 5-arg cap. observeValuesByInvestmentId() covers ALL investments
    // (active + archived) — search shows both, so every result has a value.
    private val investmentsWithValues = combine(
        investmentRepository.observeAll(),
        investmentRepository.observeValuesByInvestmentId(),
    ) { inv, valuesById -> inv to valuesById }

    private val restSource = combine(
        categoryRepository.observeAll(),
        tagRepository.observeAll(),
        investmentsWithValues,
        insuranceRepository.observeAll(),
    ) { c, t, (inv, valuesById), ins -> Rest(c, t, inv, ins, valuesById) }

    private val bagSource = combine(
        transactionRepository.observeAll(),
        accountRepository.observeAll(),
        cardRepository.observeAll(),
        personRepository.observeAll(),
        restSource,
    ) { txns, accts, cards, people, rest -> Bag(txns, accts, cards, people, rest) }

    val state: StateFlow<SearchUiState> = combine(_query, bagSource) { q, bag ->
        val trimmed = q.trim()
        if (trimmed.isEmpty()) {
            SearchUiState(query = q, results = SearchResults(), isEmpty = true)
        } else {
            val needle = trimmed.lowercase()
            val numeric = trimmed.toDoubleOrNull()
            val results = SearchResults(
                transactions = bag.txns
                    .filter { it.matches(needle, numeric) }
                    .sortedByDescending { it.date }
                    .take(MAX_PER_GROUP),
                accounts = bag.accounts
                    .filter { it.matches(needle) }
                    .take(MAX_PER_GROUP),
                cards = bag.cards
                    .filter { it.matches(needle) }
                    .take(MAX_PER_GROUP),
                people = bag.people
                    .filter { it.matches(needle) }
                    .take(MAX_PER_GROUP),
                categories = bag.rest.categories
                    .filter { it.matches(needle) }
                    .take(MAX_PER_GROUP),
                tags = bag.rest.tags
                    .filter { it.matches(needle) }
                    .take(MAX_PER_GROUP),
                investments = bag.rest.investments
                    .filter { it.matches(needle) }
                    .take(MAX_PER_GROUP),
                insurances = bag.rest.insurances
                    .filter { it.matches(needle) }
                    .take(MAX_PER_GROUP),
                investmentValuesById = bag.rest.investmentValuesById,
            )
            SearchUiState(query = q, results = results, isEmpty = results.isEmpty)
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChanged(v: String) {
        _query.value = v
    }

    fun clear() {
        _query.value = ""
    }

    private companion object {
        const val MAX_PER_GROUP = 12
    }
}

// ─────────────────────────── matchers ────────────────────────────────────────

private fun Transaction.matches(needle: String, numeric: Double?): Boolean {
    if (description.lowercase().contains(needle)) return true
    if (place?.lowercase()?.contains(needle) == true) return true
    if (notes?.lowercase()?.contains(needle) == true) return true
    if (numeric != null) {
        // Match either whole rupees ("1500") or two-decimal exact ("1500.50").
        val wholeRupees = (amount.absoluteValue).roundToLong()
        if (wholeRupees.toString().contains(needle)) return true
        if ("%.2f".format(amount.absoluteValue).contains(needle)) return true
    }
    return false
}

private fun Account.matches(needle: String): Boolean {
    if (name.lowercase().contains(needle)) return true
    if (institution?.lowercase()?.contains(needle) == true) return true
    if (accountNumberLast4?.contains(needle) == true) return true
    return false
}

private fun Card.matches(needle: String): Boolean {
    if (name.lowercase().contains(needle)) return true
    if (issuer?.lowercase()?.contains(needle) == true) return true
    if (cardNumberLast4?.contains(needle) == true) return true
    return false
}

private fun Person.matches(needle: String): Boolean {
    if (name.lowercase().contains(needle)) return true
    if (contact?.lowercase()?.contains(needle) == true) return true
    return false
}

private fun Category.matches(needle: String): Boolean = name.lowercase().contains(needle)

private fun Tag.matches(needle: String): Boolean = name.lowercase().contains(needle)

private fun Investment.matches(needle: String): Boolean {
    if (name.lowercase().contains(needle)) return true
    if (institution?.lowercase()?.contains(needle) == true) return true
    if (taxSection?.lowercase()?.contains(needle) == true) return true
    return false
}

private fun Insurance.matches(needle: String): Boolean {
    if (name.lowercase().contains(needle)) return true
    if (provider.lowercase().contains(needle)) return true
    if (policyNumber?.lowercase()?.contains(needle) == true) return true
    return false
}

class SearchViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val personRepository: PersonRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val investmentRepository: InvestmentRepository,
    private val insuranceRepository: InsuranceRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return SearchViewModel(
            transactionRepository,
            accountRepository,
            cardRepository,
            personRepository,
            categoryRepository,
            tagRepository,
            investmentRepository,
            insuranceRepository,
        ) as T
    }
}
