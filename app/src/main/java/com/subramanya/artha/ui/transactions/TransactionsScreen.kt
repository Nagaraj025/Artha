package com.subramanya.artha.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.ui.common.MonoMeta
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.ui.theme.Expense
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.IncomeSoft
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.ui.transaction.AddTransactionSheet
import com.subramanya.artha.ui.transaction.AddTransactionViewModel
import com.subramanya.artha.ui.transaction.AddTransactionViewModelFactory
import com.subramanya.artha.utils.IndianNumberFormat
import com.subramanya.artha.utils.TimeRange

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TransactionsScreen(modifier: Modifier = Modifier, onOpenTransaction: (String) -> Unit = {}) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication

    val vm: TransactionsViewModel = viewModel(
        factory = TransactionsViewModelFactory(
            transactionRepository = app.transactionRepository,
            accountRepository = app.accountRepository,
            cardRepository = app.cardRepository,
            categoryRepository = app.categoryRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var sortMenuOpen by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (state.isSelectionMode) {
                    SelectionTopBar(
                        count = state.selectedIds.size,
                        onClear = vm::clearSelection,
                        onDelete = vm::requestDelete,
                    )
                } else {
                    LedgerHeader(
                        rangeLabel = rangeDisplay(state.filter.range),
                        sort = state.sort,
                        sortMenuOpen = sortMenuOpen,
                        onSortMenuToggle = { sortMenuOpen = it },
                        onSortChanged = vm::onSortChanged,
                    )
                }

                TotalsStrip(inSum = state.inSum, outSum = state.outSum, net = state.net)

                SearchField(query = state.query, onQueryChanged = vm::onQueryChanged)
                Spacer(Modifier.height(8.dp))
                FilterRow(state = state, viewModel = vm)
                Spacer(Modifier.height(4.dp))

                if (state.rows.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Inbox, contentDescription = null, tint = Text3, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.transactions_empty),
                                color = Text3,
                            )
                        }
                    }
                    return@Column
                }

                // Each transaction is its own keyed lazy item (true virtualization) — the day-card
                // look is preserved by rounding only the first/last row of each day and drawing a
                // divider between rows. Selecting/scrolling recomposes one row, not the whole day.
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(state.rows, key = { it.key }) { item ->
                        when (item) {
                            is LedgerListItem.DayHeader ->
                                DayHeader(label = item.display, sum = item.daySum)
                            is LedgerListItem.Entry -> {
                                val shape = when {
                                    item.isFirstInDay && item.isLastInDay -> RoundedCornerShape(14.dp)
                                    item.isFirstInDay -> RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                                    item.isLastInDay -> RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
                                    else -> RoundedCornerShape(0.dp)
                                }
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shape)
                                            .background(Surface2),
                                    ) {
                                        TransactionRow(
                                            txn = item.txn,
                                            category = item.category,
                                            selected = item.txn.id in state.selectedIds,
                                            selectionMode = state.isSelectionMode,
                                            onTap = {
                                                if (state.isSelectionMode) {
                                                    vm.toggleSelected(item.txn.id)
                                                } else {
                                                    onOpenTransaction(item.txn.id)
                                                }
                                            },
                                            onLongPress = { vm.toggleSelected(item.txn.id) },
                                        )
                                        if (!item.isLastInDay) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 56.dp)
                                                    .fillMaxWidth()
                                                    .height(1.dp)
                                                    .background(MaterialTheme.colorScheme.outlineVariant),
                                            )
                                        }
                                    }
                                    if (item.isLastInDay) Spacer(Modifier.height(14.dp))
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // Quick add: open the standard Add Transaction sheet straight from the Ledger.
        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.ledger_add_transaction))
        }
    }

    if (state.showDeleteConfirm) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = vm::dismissDeleteConfirm,
            title = stringResource(R.string.transactions_delete_confirm_title),
            text = stringResource(R.string.transactions_delete_confirm_body, state.selectedIds.size),
            confirmLabel = stringResource(R.string.transactions_delete_confirm_yes),
            confirmDestructive = true,
            onConfirm = vm::confirmDelete,
            cancelLabel = stringResource(R.string.common_cancel),
            onCancel = vm::dismissDeleteConfirm,
        )
    }

    if (showAddSheet) {
        val txnVm: AddTransactionViewModel = viewModel(
            factory = AddTransactionViewModelFactory(
                accountRepository = app.accountRepository,
                cardRepository = app.cardRepository,
                categoryRepository = app.categoryRepository,
                personRepository = app.personRepository,
                tagRepository = app.tagRepository,
                transactionRepository = app.transactionRepository,
                transactionRuleRepository = app.transactionRuleRepository,
                investmentRepository = app.investmentRepository,
                settingsPreferences = app.settingsPreferences,
            ),
        )
        AddTransactionSheet(viewModel = txnVm, onDismiss = { showAddSheet = false })
    }
}

// ───────────────────────────── Header (Editorial title + sort) ───────────────

@Composable
private fun LedgerHeader(
    rangeLabel: String,
    sort: TransactionSort,
    sortMenuOpen: Boolean,
    onSortMenuToggle: (Boolean) -> Unit,
    onSortChanged: (TransactionSort) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "THE LEDGER",
                style = EyebrowStyle,
                color = Text3,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = rangeLabel,
                fontFamily = InstrumentSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box {
            IconButton(onClick = { onSortMenuToggle(true) }) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.transactions_sort_label))
            }
            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { onSortMenuToggle(false) }) {
                TransactionSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayLabel()) },
                        onClick = {
                            onSortChanged(option)
                            onSortMenuToggle(false)
                        },
                        leadingIcon = if (sort == option) {
                            { Icon(Icons.Filled.FilterList, contentDescription = null) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

// ───────────────────────────── In / Out / Net strip ──────────────────────────

@Composable
private fun TotalsStrip(inSum: Double, outSum: Double, net: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TotalCell(label = "IN", value = inSum, color = Income, dot = Income)
        VerticalSep()
        TotalCell(label = "OUT", value = outSum, color = MaterialTheme.colorScheme.onSurface, dot = Expense)
        VerticalSep()
        TotalCell(label = "NET", value = net, color = MaterialTheme.colorScheme.onSurface, dot = null, sign = true)
    }
}

@Composable
private fun TotalCell(label: String, value: Double, color: Color, dot: Color?, sign: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dot != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dot),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(label, style = EyebrowStyle, color = Text3)
        }
        Spacer(Modifier.height(4.dp))
        val rendered = if (sign && value >= 0) {
            "+" + IndianNumberFormat.formatCompact(value)
        } else {
            IndianNumberFormat.formatCompact(value)
        }
        Text(
            text = rendered,
            color = color,
            fontFamily = InstrumentSerif,
            fontSize = 20.sp,
            lineHeight = 24.sp,
        )
    }
}

@Composable
private fun VerticalSep() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(34.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

// ───────────────────────────── Search + Filters ──────────────────────────────

@Composable
private fun SearchField(query: String, onQueryChanged: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.transactions_search_placeholder), color = Text3) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Text3) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = null, tint = Text3)
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Surface2,
            focusedContainerColor = Surface2,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(50.dp),
    )
}

@Composable
private fun FilterRow(state: TransactionsUiState, viewModel: TransactionsViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RangeFilterChip(
            current = state.filter.range,
            onPick = { range -> viewModel.onFilterChanged { it.copy(range = range) } },
        )
        TypeFilterChip(
            current = state.filter.typeFilter,
            onPick = { type -> viewModel.onFilterChanged { it.copy(typeFilter = type) } },
        )
        AccountFilterChip(
            accounts = state.accounts,
            currentId = state.filter.accountId,
            onPick = { id -> viewModel.onFilterChanged { it.copy(accountId = id) } },
        )
        CardFilterChip(
            cards = state.cards,
            currentId = state.filter.cardId,
            onPick = { id -> viewModel.onFilterChanged { it.copy(cardId = id) } },
        )
        CategoryFilterChip(
            categories = state.categories,
            currentId = state.filter.categoryId,
            onPick = { id -> viewModel.onFilterChanged { it.copy(categoryId = id) } },
        )
        if (state.filter != TransactionsFilter() || state.query.isNotEmpty()) {
            TextButton(onClick = { viewModel.clearFilters() }) {
                Text(stringResource(R.string.transactions_filter_clear_all))
            }
        }
    }
}

@Composable
private fun RangeFilterChip(current: TimeRange, onPick: (TimeRange) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ElevatedFilterChip(
            selected = current != TimeRange.ALL_TIME,
            onClick = { expanded = true },
            label = { Text(rangeLabel(current)) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TimeRange.entries.forEach { range ->
                DropdownMenuItem(
                    text = { Text(rangeLabel(range)) },
                    onClick = {
                        onPick(range)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun rangeLabel(range: TimeRange): String = when (range) {
    TimeRange.TODAY -> stringResource(R.string.dashboard_section_recent_today)
    TimeRange.THIS_WEEK -> stringResource(R.string.dashboard_section_recent_week)
    TimeRange.THIS_MONTH -> stringResource(R.string.dashboard_section_recent_month)
    TimeRange.ALL_TIME -> stringResource(R.string.transactions_filter_date) + ": " +
        stringResource(R.string.transactions_filter_any)
}

@Composable
private fun rangeDisplay(range: TimeRange): String = when (range) {
    TimeRange.TODAY -> "Today"
    TimeRange.THIS_WEEK -> "This week"
    TimeRange.THIS_MONTH -> "This month"
    TimeRange.ALL_TIME -> "All time"
}

@Composable
private fun TypeFilterChip(current: TransactionType?, onPick: (TransactionType?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ElevatedFilterChip(
            selected = current != null,
            onClick = { expanded = true },
            label = { Text(current?.displayLabel() ?: stringResource(R.string.transactions_filter_type)) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.transactions_filter_any)) },
                onClick = {
                    onPick(null)
                    expanded = false
                },
            )
            TransactionType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayLabel()) },
                    onClick = {
                        onPick(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AccountFilterChip(accounts: List<Account>, currentId: String?, onPick: (String?) -> Unit) {
    if (accounts.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val currentName = currentId?.let { id -> accounts.firstOrNull { it.id == id }?.name }
    Box {
        ElevatedFilterChip(
            selected = currentId != null,
            onClick = { expanded = true },
            label = { Text(currentName ?: stringResource(R.string.transactions_filter_account)) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.transactions_filter_any)) },
                onClick = {
                    onPick(null)
                    expanded = false
                },
            )
            accounts.forEach { acct ->
                DropdownMenuItem(
                    text = { Text(acct.name) },
                    onClick = {
                        onPick(acct.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CardFilterChip(cards: List<Card>, currentId: String?, onPick: (String?) -> Unit) {
    if (cards.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val currentName = currentId?.let { id -> cards.firstOrNull { it.id == id }?.name }
    Box {
        ElevatedFilterChip(
            selected = currentId != null,
            onClick = { expanded = true },
            label = { Text(currentName ?: stringResource(R.string.transactions_filter_card)) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.transactions_filter_any)) },
                onClick = {
                    onPick(null)
                    expanded = false
                },
            )
            cards.forEach { card ->
                DropdownMenuItem(
                    text = { Text(card.name) },
                    onClick = {
                        onPick(card.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterChip(categories: List<Category>, currentId: String?, onPick: (String?) -> Unit) {
    if (categories.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val currentName = currentId?.let { id -> categories.firstOrNull { it.id == id }?.name }
    Box {
        ElevatedFilterChip(
            selected = currentId != null,
            onClick = { expanded = true },
            label = { Text(currentName ?: stringResource(R.string.transactions_filter_category)) },
        )
        val parents = remember(categories) { categories.filter { it.parentId == null } }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.transactions_filter_any)) },
                onClick = {
                    onPick(null)
                    expanded = false
                },
            )
            parents.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.name) },
                    onClick = {
                        onPick(cat.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ───────────────────────────── Day Header + Rows ─────────────────────────────

@Composable
private fun DayHeader(label: String, sum: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 6.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label.uppercase(), style = EyebrowStyle, color = Text3, modifier = Modifier.weight(1f))
        Text(
            text = (if (sum >= 0) "+" else "") + IndianNumberFormat.format(sum),
            color = Text3,
            fontFamily = IbmPlexMono,
            fontSize = 11.sp,
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TransactionRow(
    txn: Transaction,
    category: Category?,
    selected: Boolean,
    selectionMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    val isIncome = txn.type.isIncomeLike()
    // Prefer the category-specific icon (Restaurant for food, DirectionsCar for
    // transport, etc.) so the row is scannable by shape; fall back to the type
    // glyph for transactions with no category (transfers, raw card payments).
    val rowIcon = category?.icon
        ?.let { com.subramanya.artha.utils.MaterialIcons.resolve(it) }
        ?: iconForType(txn.type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(container)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (isIncome) IncomeSoft else Surface4),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = rowIcon,
                contentDescription = null,
                tint = if (isIncome) Income else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = txn.description.ifBlank { txn.type.displayLabel() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            MonoMeta(text = txn.type.displayLabel())
        }
        Text(
            text = signedAmount(txn),
            color = if (isIncome) Income else MaterialTheme.colorScheme.onSurface,
            style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

// ───────────────────────────── Selection top bar ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(count: Int, onClear: () -> Unit, onDelete: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.transactions_select_count, count)) },
        navigationIcon = {
            IconButton(onClick = onClear) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.transactions_select_clear))
            }
        },
        actions = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.transactions_select_delete))
            }
        },
    )
}

// ───────────────────────────── helpers ───────────────────────────────────────

@Composable
private fun TransactionType.displayLabel(): String = when (this) {
    TransactionType.EXPENSE -> stringResource(R.string.txn_type_expense)
    TransactionType.INCOME -> stringResource(R.string.txn_type_income)
    TransactionType.TRANSFER -> stringResource(R.string.txn_type_transfer)
    TransactionType.CARD_PAYMENT -> stringResource(R.string.txn_type_card_payment)
    TransactionType.REFUND -> stringResource(R.string.txn_type_refund)
    TransactionType.CASHBACK -> stringResource(R.string.txn_type_cashback)
    TransactionType.INTEREST -> stringResource(R.string.txn_type_interest)
    TransactionType.LOAN_GIVEN -> stringResource(R.string.txn_type_loan_given)
    TransactionType.LOAN_RECEIVED -> stringResource(R.string.txn_type_loan_received)
    TransactionType.GIFT_SENT -> stringResource(R.string.txn_type_gift_sent)
    TransactionType.GIFT_RECEIVED -> stringResource(R.string.txn_type_gift_received)
    TransactionType.ADJUSTMENT -> stringResource(R.string.txn_type_adjustment)
    else -> name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }
}

@Composable
private fun TransactionSort.displayLabel(): String = when (this) {
    TransactionSort.DATE_DESC -> stringResource(R.string.transactions_sort_date_desc)
    TransactionSort.DATE_ASC -> stringResource(R.string.transactions_sort_date_asc)
    TransactionSort.AMOUNT_DESC -> stringResource(R.string.transactions_sort_amount_desc)
    TransactionSort.AMOUNT_ASC -> stringResource(R.string.transactions_sort_amount_asc)
}

private fun iconForType(type: TransactionType) = when (type) {
    TransactionType.CARD_PAYMENT -> Icons.Filled.CreditCard
    else -> Icons.Filled.AccountBalance
}

private fun TransactionType.isIncomeLike() = this in setOf(
    TransactionType.INCOME,
    TransactionType.REFUND,
    TransactionType.CASHBACK,
    TransactionType.INTEREST,
    TransactionType.LOAN_RECEIVED,
    TransactionType.GIFT_RECEIVED,
)

private fun signedAmount(txn: Transaction): String {
    val abs = IndianNumberFormat.format(txn.amount)
    return when {
        txn.type.isIncomeLike() -> "+$abs"
        txn.type == TransactionType.EXPENSE ||
            txn.type == TransactionType.LOAN_GIVEN ||
            txn.type == TransactionType.GIFT_SENT -> "−$abs"
        else -> abs
    }
}
