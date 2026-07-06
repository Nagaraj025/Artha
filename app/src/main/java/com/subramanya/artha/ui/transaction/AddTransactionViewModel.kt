package com.subramanya.artha.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.entity.enums.CardType
import com.subramanya.artha.data.entity.enums.CategoryType
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.PersonRelation
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.data.preferences.SettingsPreferences
import com.subramanya.artha.data.preferences.SpouseTransactionDefault
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.InvestmentRepository
import com.subramanya.artha.data.repository.PersonRepository
import com.subramanya.artha.data.repository.TagRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.data.repository.TransactionRuleRepository
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.Investment
import com.subramanya.artha.domain.model.Person
import com.subramanya.artha.domain.model.Tag
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.domain.rules.RuleEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * State for the Add Transaction sheet.
 *
 * Save flow: trySave() → interceptSaveIfNeeded() → commitSave(). The intercept hook
 * is where the spouse-prompt dialog short-circuits: if the user is saving an EXPENSE
 * with a person tagged as SPOUSE and they haven't pinned a permanent default in
 * Settings → Behavior, we stash a `pendingSpousePrompt` and let the UI render the
 * dialog. Otherwise we either save straight through or apply the saved default
 * override before saving.
 */
class AddTransactionViewModel(
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val personRepository: PersonRepository,
    private val tagRepository: TagRepository,
    private val transactionRepository: TransactionRepository,
    private val transactionRuleRepository: TransactionRuleRepository,
    private val investmentRepository: InvestmentRepository,
    private val settingsPreferences: SettingsPreferences,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val _state = MutableStateFlow(AddTransactionUiState())
    val state: StateFlow<AddTransactionUiState> = _state.asStateFlow()

    /**
     * One-shot signal emitted exactly once per successful save — an explicit alternative to
     * watching `state.savedAndClose` transition, for callers (like ReviewScreen) that need to
     * react to "a save just completed" without racing [AddTransactionSheet]'s own internal
     * effect that resets `savedAndClose` back to false via [acknowledgeClose]. Buffered with
     * capacity 1 so an emission isn't lost if the collector isn't registered yet at the exact
     * moment commitSave() runs.
     */
    private val _saveCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveCompleted: SharedFlow<Unit> = _saveCompleted.asSharedFlow()

    /**
     * Live lists for the From/To pickers; merged into a single [FundsEndpoint]
     * catalogue. Includes Investments so the new Invest tab can pick one as
     * the destination (RD top-up, SIP, gold buy, etc.).
     */
    val fundsCatalogue: StateFlow<List<FundsEndpoint>> = combine(
        accountRepository.observeActive(),
        cardRepository.observeActive(),
        investmentRepository.observeActive(),
    ) { accounts: List<Account>, cards: List<Card>, investments: List<Investment> ->
        buildList {
            accounts.forEach {
                add(FundsEndpoint(kind = SourceKind.ACCOUNT, id = it.id, displayName = it.name))
            }
            cards.forEach {
                add(
                    FundsEndpoint(
                        kind = SourceKind.CARD,
                        id = it.id,
                        displayName = it.name,
                        isCreditCard = it.type == com.subramanya.artha.data.entity.enums.CardType.CREDIT,
                    ),
                )
            }
            investments.forEach {
                add(FundsEndpoint(kind = SourceKind.INVESTMENT, id = it.id, displayName = it.name))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allCategories: StateFlow<List<Category>> =
        categoryRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val people: StateFlow<List<Person>> =
        personRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tags: StateFlow<List<Tag>> =
        tagRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Persisted spouse default — mirrored locally so trySave reads it synchronously. */
    private val spouseDefault: StateFlow<SpouseTransactionDefault> =
        settingsPreferences.spouseTransactionDefault
            .stateIn(viewModelScope, SharingStarted.Eagerly, SpouseTransactionDefault.ASK)

    // ---------- prefill ----------

    /**
     * Card Detail "Pay Bill" flow: pre-fill the sheet as a Transfer to the supplied
     * credit card. The user still picks the source account and amount. Idempotent —
     * safe to call from LaunchedEffect.
     */
    /**
     * AI Quick Entry handoff (Phase 3). The parser surfaces a typed
     * [com.subramanya.artha.ai.AiQuickEntryParsed]; we copy whatever has non-null
     * value into the form, leaving source/destination/category-id resolution to
     * the user since fuzzy-matching a string like "Food" to a category id is
     * brittle and the user is already in the sheet.
     */
    fun applyAiPrefill(parsed: com.subramanya.artha.ai.AiQuickEntryParsed) {
        _state.update { current ->
            current.copy(
                tab = parsed.type.value?.toTab() ?: current.tab,
                amountText = parsed.amount.value?.toPlainString() ?: current.amountText,
                description = parsed.description.value ?: current.description,
                paymentApp = parsed.paymentApp.value ?: current.paymentApp,
                place = parsed.place.value ?: current.place,
                dateTimeMillis = parsed.dateMillis.value ?: current.dateTimeMillis,
                notes = parsed.notes?.takeIf { it.isNotBlank() } ?: current.notes,
            )
        }
    }

    /**
     * SMS auto-detect Review tab handoff. The pending row is a structured parse of a bank
     * SMS (amount/direction/merchant/date, plus an optional rule-suggested category); we
     * copy it straight into the form fields the same way [applyAiPrefill] does for AI Quick
     * Entry, leaving source/destination account resolution to the user.
     *
     * Builds from a fresh [AddTransactionUiState] rather than `current.copy(...)` — the Review
     * tab reuses a single VM instance (scoped to the Review NavBackStackEntry) across opening
     * and dismissing multiple different pending items in one visit. Copying off `current` would
     * leak the previous item's source/tags/notes/category into the next item's sheet if the
     * user dismissed without saving.
     */
    fun applyPendingSmsPrefill(pending: com.subramanya.artha.domain.model.PendingSmsTransaction, suggestedCategoryName: String?) {
        _state.value = AddTransactionUiState(
            tab = if (pending.direction == com.subramanya.artha.domain.model.SmsDirection.DEBIT) {
                TransactionTab.EXPENSE
            } else {
                TransactionTab.INCOME
            },
            amountText = pending.amount.toString(),
            description = pending.merchant ?: pending.sender,
            dateTimeMillis = pending.receivedAt,
            categoryId = pending.suggestedCategoryId,
            categoryDisplay = suggestedCategoryName,
        )
    }

    /**
     * Investment Detail "Add contribution" flow: pre-fill the sheet on the Invest tab
     * with the given investment pre-selected as the destination. The user still picks
     * the funding account (source) and the amount. Idempotent — safe to call from
     * LaunchedEffect.
     */
    fun applyInvestContributionPrefill(investment: FundsEndpoint) {
        _state.update {
            it.copy(
                tab = TransactionTab.INVEST,
                destination = investment,
                categoryId = null,
                categoryDisplay = null,
                subCategoryId = null,
                subCategoryDisplay = null,
            )
        }
    }

    fun applyPayBillPrefill(toCard: FundsEndpoint) {
        _state.update {
            it.copy(
                tab = TransactionTab.TRANSFER,
                destination = toCard,
                categoryId = null,
                categoryDisplay = null,
                subCategoryId = null,
                subCategoryDisplay = null,
            )
        }
    }

    /**
     * Transaction Detail "Edit" flow: hydrate every field from an existing transaction.
     * Caller is responsible for resolving [source] / [destination] FundsEndpoints from
     * the live catalogue (because that data lives in flows the caller already has).
     *
     * The VM remembers the existing id and createdAt timestamp so save() upserts the
     * same row rather than minting a new one.
     */
    fun applyEditPrefill(
        transaction: Transaction,
        source: FundsEndpoint?,
        destination: FundsEndpoint?,
        categoryDisplay: String?,
        subCategoryDisplay: String?,
    ) {
        editingTransactionId = transaction.id
        editingCreatedAt = transaction.createdAt
        editingOriginalType = transaction.type
        editingExcludedFromExpense = transaction.excludedFromExpenseTotal
        _state.update {
            it.copy(
                isEditing = true,
                tab = transaction.type.toTab(),
                amountText = transaction.amount.toPlainString(),
                dateTimeMillis = transaction.date,
                source = source,
                destination = destination,
                categoryId = transaction.categoryId,
                categoryDisplay = categoryDisplay,
                subCategoryId = transaction.subCategoryId,
                subCategoryDisplay = subCategoryDisplay,
                description = transaction.description,
                paymentApp = transaction.paymentApp,
                peopleIds = transaction.peopleIds.toSet(),
                place = transaction.place.orEmpty(),
                tagIds = transaction.tagIds.toSet(),
                receiptUri = transaction.receiptUri,
                notes = transaction.notes.orEmpty(),
            )
        }
    }

    private var editingTransactionId: String? = null
    private var editingCreatedAt: Long? = null

    /**
     * The type of the transaction being edited. The INVEST tab can only emit INVESTMENT_BUY,
     * so without this an edited INVESTMENT_SELL would be silently rewritten to a BUY —
     * inverting both the account leg and the invested amount. Preserve SELL across edit.
     */
    private var editingOriginalType: TransactionType? = null

    /** Preserves the edited transaction's exclude-from-expense flag (rules don't re-run on edit). */
    private var editingExcludedFromExpense: Boolean = false

    private fun TransactionType.toTab(): TransactionTab = when (this) {
        TransactionType.INCOME -> TransactionTab.INCOME
        TransactionType.TRANSFER, TransactionType.CARD_PAYMENT -> TransactionTab.TRANSFER
        TransactionType.INVESTMENT_BUY, TransactionType.INVESTMENT_SELL -> TransactionTab.INVEST
        else -> TransactionTab.EXPENSE
    }

    private fun Double.toPlainString(): String = if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

    // ---------- field setters ----------

    fun onTabChanged(tab: TransactionTab) {
        _state.update {
            it.copy(
                tab = tab,
                categoryId = null,
                categoryDisplay = null,
                subCategoryId = null,
                subCategoryDisplay = null,
                // Transfer + Invest both need a destination; everything else
                // doesn't, so wipe the destination when leaving those tabs.
                destination = if (tab == TransactionTab.TRANSFER || tab == TransactionTab.INVEST) {
                    it.destination
                } else {
                    null
                },
                showValidationErrors = false,
            )
        }
    }

    fun onAmountChanged(value: String) {
        val sanitised = value.filterIndexed { index, c ->
            c.isDigit() || (c == '.' && value.indexOf('.') == index)
        }
        _state.update { it.copy(amountText = sanitised) }
    }

    fun onDateTimeChanged(millis: Long) {
        _state.update { it.copy(dateTimeMillis = millis) }
    }

    fun onSourceSelected(endpoint: FundsEndpoint?) {
        _state.update { it.copy(source = endpoint) }
    }

    fun onDestinationSelected(endpoint: FundsEndpoint?) {
        _state.update { it.copy(destination = endpoint) }
    }

    fun onCategorySelected(category: Category) {
        _state.update {
            it.copy(
                categoryId = category.id,
                categoryDisplay = category.name,
                subCategoryId = null,
                subCategoryDisplay = null,
            )
        }
    }

    fun onSubCategorySelected(category: Category?) {
        _state.update {
            it.copy(
                subCategoryId = category?.id,
                subCategoryDisplay = category?.name,
            )
        }
    }

    fun onDescriptionChanged(value: String) {
        _state.update { it.copy(description = value) }
    }

    fun onPaymentAppChanged(app: PaymentApp) {
        _state.update { it.copy(paymentApp = app) }
    }

    fun togglePerson(personId: String) {
        _state.update {
            val updated = if (personId in it.peopleIds) it.peopleIds - personId else it.peopleIds + personId
            it.copy(peopleIds = updated)
        }
    }

    fun onPlaceChanged(value: String) {
        _state.update { it.copy(place = value) }
    }

    fun toggleTag(tagId: String) {
        _state.update {
            val updated = if (tagId in it.tagIds) it.tagIds - tagId else it.tagIds + tagId
            it.copy(tagIds = updated)
        }
    }

    fun onReceiptPicked(uri: String?) {
        _state.update { it.copy(receiptUri = uri) }
    }

    fun onNotesChanged(value: String) {
        _state.update { it.copy(notes = value) }
    }

    // ---------- inline adders ----------

    fun addPersonInline(name: String, relation: PersonRelation, autoSelect: Boolean = true) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            personRepository.upsert(
                Person(
                    id = newId,
                    name = name.trim(),
                    relation = relation,
                    contact = null,
                    avatarUri = null,
                    createdAt = clock(),
                ),
            )
            if (autoSelect) {
                _state.update { it.copy(peopleIds = it.peopleIds + newId) }
            }
        }
    }

    fun addTagInline(name: String, autoSelect: Boolean = true) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            tagRepository.upsert(
                Tag(id = newId, name = name.trim(), color = DEFAULT_TAG_COLOR),
            )
            if (autoSelect) {
                _state.update { it.copy(tagIds = it.tagIds + newId) }
            }
        }
    }

    // ---------- save ----------

    fun trySave() {
        val snapshot = _state.value
        if (!snapshot.isValid || snapshot.isSaving) {
            _state.update { it.copy(showValidationErrors = true) }
            return
        }
        if (interceptSaveIfNeeded(snapshot)) return
        commitSave(snapshot)
    }

    /**
     * Implements PRD §7.5.1. Returns true when the save has been deferred (dialog shown
     * to the user, or an override was applied and commitSave already invoked).
     */
    private fun interceptSaveIfNeeded(snapshot: AddTransactionUiState): Boolean {
        if (snapshot.tab != TransactionTab.EXPENSE) return false
        val spousePerson = people.value.firstOrNull {
            it.id in snapshot.peopleIds && it.relation == PersonRelation.SPOUSE
        } ?: return false

        return when (spouseDefault.value) {
            SpouseTransactionDefault.ASK -> {
                _state.update {
                    it.copy(
                        pendingSpousePrompt = SpousePromptInfo(
                            amount = snapshot.parsedAmount ?: 0.0,
                            personId = spousePerson.id,
                            personName = spousePerson.name,
                        ),
                    )
                }
                true
            }
            SpouseTransactionDefault.TRANSFER -> {
                commitSave(snapshot, applySpouseTransferOverride(spousePerson))
                true
            }
            SpouseTransactionDefault.EXPENSE -> false
        }
    }

    /**
     * User responded to the prompt. Optionally persist a permanent default, then save.
     * Either choice closes the dialog; CANCEL goes through [cancelSpousePrompt].
     */
    fun respondToSpousePrompt(choice: SpouseChoice, persistDefault: SpouseTransactionDefault?) {
        val snapshot = _state.value
        val spousePerson = people.value.firstOrNull {
            it.id in snapshot.peopleIds && it.relation == PersonRelation.SPOUSE
        }
        if (persistDefault != null) {
            viewModelScope.launch { settingsPreferences.setSpouseTransactionDefault(persistDefault) }
        }
        _state.update { it.copy(pendingSpousePrompt = null) }
        when (choice) {
            SpouseChoice.TRANSFER -> {
                if (spousePerson != null) {
                    commitSave(snapshot, applySpouseTransferOverride(spousePerson))
                } else {
                    commitSave(snapshot)
                }
            }
            SpouseChoice.EXPENSE -> commitSave(snapshot)
        }
    }

    fun cancelSpousePrompt() {
        _state.update { it.copy(pendingSpousePrompt = null) }
    }

    /**
     * Override spec for "Transfer to spouse": type becomes TRANSFER (excluded from monthly
     * expense totals via MonthlyAggregator) and destination becomes a synthetic EXTERNAL
     * endpoint pointing at the spouse Person — informational, doesn't credit any account.
     */
    private fun applySpouseTransferOverride(spouse: Person): SaveOverride = SaveOverride(
        type = TransactionType.TRANSFER,
        destination = FundsEndpoint(
            kind = SourceKind.EXTERNAL,
            id = spouse.id,
            displayName = spouse.name,
        ),
    )

    private fun commitSave(snapshot: AddTransactionUiState, override: SaveOverride? = null) {
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val now = clock()
            val isEditing = editingTransactionId != null
            val id = editingTransactionId ?: UUID.randomUUID().toString()
            val created = editingCreatedAt ?: now
            val tabType = override?.type ?: snapshot.effectiveType
            // The INVEST tab always yields INVESTMENT_BUY; if we're editing a row that was an
            // INVESTMENT_SELL, keep it a SELL so its direction isn't inverted on save.
            val effectiveType =
                if (tabType == TransactionType.INVESTMENT_BUY &&
                    editingOriginalType == TransactionType.INVESTMENT_SELL
                ) {
                    TransactionType.INVESTMENT_SELL
                } else {
                    tabType
                }
            val effectiveDestination = override?.destination ?: snapshot.destination
            // Debit/prepaid cards are conduits to a bank account, not credit lines. Record the
            // transaction against the linked account so the account is debited/credited and no
            // phantom card "outstanding" accrues. Credit cards keep their own endpoints (and
            // their outstanding). Resolved AFTER effectiveType so credit-card CARD_PAYMENT
            // detection on the Transfer tab is unaffected.
            val resolvedSource = resolveCardAlias(snapshot.source!!)
            val resolvedDestination = effectiveDestination?.let { resolveCardAlias(it) }
            val baseTxn = Transaction(
                id = id,
                type = effectiveType,
                amount = snapshot.parsedAmount ?: 0.0,
                currency = "INR",
                date = snapshot.dateTimeMillis,
                description = snapshot.description.trim(),
                categoryId = snapshot.categoryId,
                subCategoryId = snapshot.subCategoryId,
                sourceType = resolvedSource.kind,
                sourceId = resolvedSource.id,
                destinationType = resolvedDestination?.kind,
                destinationId = resolvedDestination?.id,
                paymentApp = snapshot.paymentApp,
                place = snapshot.place.trim().takeIf { it.isNotBlank() },
                latitude = null,
                longitude = null,
                peopleIds = snapshot.peopleIds.toList(),
                tagIds = snapshot.tagIds.toList(),
                receiptUri = snapshot.receiptUri,
                notes = snapshot.notes.trim().takeIf { it.isNotBlank() },
                taxSection = null,
                recurringRuleId = null,
                isSplit = false,
                splitGroupId = null,
                source = TransactionSource.MANUAL,
                createdAt = created,
                updatedAt = now,
            )
            // Apply the Rules Engine only on CREATE. On edit we persist the user's values
            // verbatim — re-running rules would re-overwrite a field the user just manually
            // corrected (e.g. a type/category the rule originally mis-set). The engine may
            // rewrite type, category, tax section, and add tags/people. PromptSpouse /
            // ExcludeFromExpense signals are produced too but ignored here for now — the spouse
            // path is already handled by interceptSaveIfNeeded() above; exclude-from-expense is
            // a future enhancement for the MonthlyAggregator hint table.
            val toSave = if (isEditing) {
                // Rules don't re-run on edit; preserve the row's existing exclude-from-expense flag.
                baseTxn.copy(excludedFromExpenseTotal = editingExcludedFromExpense)
            } else {
                val activeRules = transactionRuleRepository.observeActive()
                    .let { runCatching { it.first() }.getOrDefault(emptyList()) }
                val result = RuleEngine.apply(baseTxn, activeRules, people.value)
                // Honor the engine's ExcludeFromExpenseTotal signal by persisting it on the txn.
                result.transaction.copy(excludedFromExpenseTotal = result.excludeFromExpenseTotal)
            }
            transactionRepository.save(toSave)
            _state.update { it.copy(isSaving = false, savedAndClose = true) }
            _saveCompleted.tryEmit(Unit)
        }
    }

    /**
     * Maps a DEBIT/PREPAID card endpoint to its linked bank account. A debit/prepaid card draws
     * from an account, so spends/credits on it should move the account — not accrue a fictitious
     * card "outstanding". Credit cards (and cards with no linked account) are returned unchanged.
     */
    private suspend fun resolveCardAlias(endpoint: FundsEndpoint): FundsEndpoint {
        if (endpoint.kind != SourceKind.CARD) return endpoint
        val card = cardRepository.getById(endpoint.id) ?: return endpoint
        if (card.type == CardType.CREDIT) return endpoint
        val linkedAccountId = card.linkedAccountId ?: return endpoint
        return endpoint.copy(kind = SourceKind.ACCOUNT, id = linkedAccountId, isCreditCard = false)
    }

    /** Called by the host after consuming `savedAndClose`, in case the sheet is reused. */
    fun acknowledgeClose() {
        _state.value = AddTransactionUiState()
        // Clear edit-mode markers too — otherwise a reused VM would treat the NEXT fresh entry as
        // an edit and upsert over the previously-edited transaction's id (and skip the rules).
        editingTransactionId = null
        editingCreatedAt = null
        editingOriginalType = null
        editingExcludedFromExpense = false
    }

    /** Returns the category-children list for the current categoryId, used by sub-cat picker. */
    fun childrenOf(parentId: String?, type: CategoryType): List<Category> {
        if (parentId == null) return emptyList()
        return allCategories.value.filter { it.parentId == parentId && it.type == type }
    }

    private data class SaveOverride(val type: TransactionType, val destination: FundsEndpoint?)

    private companion object {
        private const val DEFAULT_TAG_COLOR: Long = 0xFF6366F1
    }
}

class AddTransactionViewModelFactory(
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val personRepository: PersonRepository,
    private val tagRepository: TagRepository,
    private val transactionRepository: TransactionRepository,
    private val transactionRuleRepository: TransactionRuleRepository,
    private val investmentRepository: InvestmentRepository,
    private val settingsPreferences: SettingsPreferences,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AddTransactionViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return AddTransactionViewModel(
            accountRepository,
            cardRepository,
            categoryRepository,
            personRepository,
            tagRepository,
            transactionRepository,
            transactionRuleRepository,
            investmentRepository,
            settingsPreferences,
        ) as T
    }
}
