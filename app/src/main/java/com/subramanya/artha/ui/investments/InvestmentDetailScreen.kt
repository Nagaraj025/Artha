package com.subramanya.artha.ui.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.InvestmentType
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.ValuationMode
import com.subramanya.artha.domain.model.Investment
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.ui.transaction.AddTransactionSheet
import com.subramanya.artha.ui.transaction.AddTransactionViewModel
import com.subramanya.artha.ui.transaction.AddTransactionViewModelFactory
import com.subramanya.artha.ui.transaction.FundsEndpoint
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentDetailScreen(
    investmentId: String,
    onBack: () -> Unit,
    onOpenTransaction: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: InvestmentDetailViewModel = viewModel(
        factory = InvestmentDetailViewModelFactory(
            investmentId = investmentId,
            investmentRepository = app.investmentRepository,
            transactionRepository = app.transactionRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var editing: Investment? by remember { mutableStateOf(null) }
    var showPostInterest by remember { mutableStateOf(false) }
    var showAddContribution by remember { mutableStateOf(false) }

    Surface(
        color = com.subramanya.artha.ui.theme.Surface1,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val inv = state.investment
            com.subramanya.artha.ui.common.InlineTopBar(
                title = inv?.name.orEmpty(),
                onBack = onBack,
                trailing = {
                    if (inv != null) {
                        IconButton(onClick = { editing = inv }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.account_detail_action_edit),
                                tint = com.subramanya.artha.ui.theme.Text2,
                            )
                        }
                        if (inv.isArchived) {
                            IconButton(onClick = { vm.restore(onRestored = onBack) }) {
                                Icon(
                                    Icons.Filled.Unarchive,
                                    contentDescription = stringResource(R.string.account_detail_action_restore),
                                    tint = com.subramanya.artha.ui.theme.Text2,
                                )
                            }
                        } else {
                            IconButton(onClick = { vm.archive(onArchived = onBack) }) {
                                Icon(
                                    Icons.Filled.Archive,
                                    contentDescription = stringResource(R.string.account_detail_action_archive),
                                    tint = com.subramanya.artha.ui.theme.Text2,
                                )
                            }
                        }
                        IconButton(onClick = vm::requestDelete) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.account_action_delete),
                                tint = com.subramanya.artha.ui.theme.Danger,
                            )
                        }
                    }
                },
            )
            if (inv == null) {
                com.subramanya.artha.ui.common.LoadingPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                )
                return@Column
            }
            Column(modifier = Modifier.fillMaxSize()) {
                HeroBlock(
                    investment = inv,
                    value = state.value,
                    invested = state.investedAmount,
                    interest = state.interest,
                    gain = state.absoluteGain,
                    pctGain = state.percentGain,
                )

                ActionRow(
                    // "Post interest" only makes sense for DERIVED deposits, where value
                    // is grown by posted interest rather than a manual current value.
                    showPostInterest = inv.valuationMode == ValuationMode.DERIVED,
                    onPostInterest = { showPostInterest = true },
                    onAddContribution = { showAddContribution = true },
                )

                MetaBlock(investment = inv)

                Text(
                    text = stringResource(R.string.investment_detail_txns_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 4.dp),
                )

                if (state.transactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.Inbox,
                            title = stringResource(R.string.investment_detail_txns_empty),
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.transactions, key = { it.id }) { txn ->
                            TransactionRow(txn = txn, onClick = { onOpenTransaction(txn.id) })
                        }
                    }
                }
            }
        }
    }

    val editingNow = editing
    if (editingNow != null) {
        InvestmentFormSheet(
            editing = editingNow,
            onDismiss = { editing = null },
        )
    }

    if (state.showDeleteConfirm) {
        if (state.transactions.isNotEmpty()) {
            com.subramanya.artha.ui.common.ArthaAlertDialog(
                onDismissRequest = vm::dismissDeleteConfirm,
                title = stringResource(R.string.investment_delete_blocked_title),
                text = stringResource(R.string.investment_delete_blocked_body, state.transactions.size),
                confirmLabel = stringResource(R.string.investment_delete_blocked_archive),
                confirmDestructive = false,
                onConfirm = {
                    vm.dismissDeleteConfirm()
                    vm.archive(onArchived = onBack)
                },
                cancelLabel = stringResource(R.string.common_cancel),
                onCancel = vm::dismissDeleteConfirm,
            )
        } else {
            com.subramanya.artha.ui.common.ArthaAlertDialog(
                onDismissRequest = vm::dismissDeleteConfirm,
                title = stringResource(R.string.investment_delete_confirm_title),
                text = stringResource(R.string.investment_delete_confirm_body),
                confirmLabel = stringResource(R.string.investment_delete_confirm_yes),
                confirmDestructive = true,
                onConfirm = { vm.confirmDelete(onDeleted = onBack) },
                cancelLabel = stringResource(R.string.common_cancel),
                onCancel = vm::dismissDeleteConfirm,
            )
        }
    }

    if (showPostInterest) {
        PostInterestDialog(
            onConfirm = { amount, dateMillis ->
                vm.postInterest(amount, dateMillis)
                showPostInterest = false
            },
            onDismiss = { showPostInterest = false },
        )
    }

    // "Add contribution" reuses the existing Add Transaction sheet, pre-filled on the
    // Invest tab with this investment as the destination. The user picks the funding
    // account and amount, then saves through the normal transaction path.
    if (showAddContribution) {
        val invSnapshot = state.investment
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
        LaunchedEffect(invSnapshot?.id) {
            if (invSnapshot != null) {
                txnVm.applyInvestContributionPrefill(
                    investment = FundsEndpoint(
                        kind = SourceKind.INVESTMENT,
                        id = invSnapshot.id,
                        displayName = invSnapshot.name,
                    ),
                )
            }
        }
        AddTransactionSheet(viewModel = txnVm, onDismiss = { showAddContribution = false })
    }
}

/**
 * Action row beneath the hero. "Post interest" is gated to DERIVED investments by the
 * caller; "Add contribution" is always available.
 */
@Composable
private fun ActionRow(showPostInterest: Boolean, onPostInterest: () -> Unit, onAddContribution: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showPostInterest) {
            OutlinedButton(onClick = onPostInterest, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Percent, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.investment_detail_action_post_interest))
            }
        }
        OutlinedButton(onClick = onAddContribution, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.investment_detail_action_add_contribution))
        }
    }
}

/**
 * Small dialog for posting an interest credit: numeric amount + a date (default today,
 * via the same Material 3 DatePicker the Add Transaction sheet uses). Confirm is
 * disabled until the amount parses to a positive number.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostInterestDialog(onConfirm: (Double, Long) -> Unit, onDismiss: () -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val amount = amountText.toDoubleOrNull()?.takeIf { it > 0.0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.investment_post_interest_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { value ->
                        // Allow digits and a single decimal point only.
                        amountText = value.filterIndexed { index, c ->
                            c.isDigit() || (c == '.' && value.indexOf('.') == index)
                        }
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.investment_post_interest_amount_label)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.investment_post_interest_date_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(8.dp))
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(DateFormatter.longDate(dateMillis)) },
                    leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { amount?.let { onConfirm(it, dateMillis) } },
                enabled = amount != null,
            ) {
                Text(stringResource(R.string.investment_post_interest_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// ---------------- pieces ----------------

@Composable
private fun HeroBlock(investment: Investment, value: Double, invested: Double, interest: Double, gain: Double, pctGain: Double) {
    val positive = gain >= 0.0
    val gainColor =
        if (positive) MaterialTheme.colorScheme.primary else com.subramanya.artha.ui.theme.Danger

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(investment.color)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Savings, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(
                        text = investment.type.displayName(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    investment.institution?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.investments_hero_current),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            // Headline is the per-mode computed value (MARKET → current price;
            // DERIVED → contributions + posted interest).
            Text(
                text = IndianNumberFormat.format(value),
                style = ArthaAmountStyles.display,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            // Subline branches on valuation mode, mirroring the list screen:
            //  - DERIVED: the gain IS the posted interest (always additive), so we label
            //    it "Interest" and show no percentage.
            //  - MARKET:  show gain ₹ and its percent, guarding the NaN case (invested == 0).
            when (investment.valuationMode) {
                ValuationMode.DERIVED -> {
                    Text(
                        text = stringResource(
                            R.string.investment_detail_subline_interest,
                            IndianNumberFormat.format(invested),
                            IndianNumberFormat.format(interest),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                ValuationMode.MARKET -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (positive) {
                                Icons.AutoMirrored.Filled.TrendingUp
                            } else {
                                Icons.AutoMirrored.Filled.TrendingDown
                            },
                            contentDescription = null,
                            tint = gainColor,
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = if (pctGain.isNaN()) {
                                stringResource(
                                    R.string.investment_detail_subline_gain_no_pct,
                                    IndianNumberFormat.format(invested),
                                    IndianNumberFormat.format(gain),
                                )
                            } else {
                                stringResource(
                                    R.string.investment_detail_subline_gain,
                                    IndianNumberFormat.format(invested),
                                    IndianNumberFormat.format(gain),
                                    "%+.2f%%".format(pctGain),
                                )
                            },
                            style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
                            color = gainColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaBlock(investment: Investment) {
    Column(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
        MetaRow(
            label = stringResource(R.string.investment_detail_meta_start),
            value = DateFormatter.longDate(investment.startDate),
        )
        investment.maturityDate?.let {
            MetaRow(
                label = stringResource(R.string.investment_detail_meta_maturity),
                value = DateFormatter.longDate(it),
            )
        }
        investment.units?.let {
            MetaRow(
                label = stringResource(R.string.investment_detail_meta_units),
                value = "%.4f".format(it).trimEnd('0').trimEnd('.'),
            )
        }
        investment.nav?.let {
            MetaRow(
                label = stringResource(R.string.investment_detail_meta_nav),
                value = IndianNumberFormat.format(it),
            )
        }
        investment.taxSection?.takeIf { it.isNotBlank() }?.let {
            MetaRow(
                label = stringResource(R.string.investment_detail_meta_tax_section),
                value = it,
            )
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TransactionRow(txn: Transaction, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        headlineContent = { Text(txn.description.ifBlank { txn.type.name }, maxLines = 1) },
        supportingContent = {
            Text(
                text = DateFormatter.shortDate(txn.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Text(
                text = IndianNumberFormat.format(txn.amount),
                style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
            )
        },
    )
}

@Composable
private fun InvestmentType.displayName(): String = when (this) {
    InvestmentType.FD -> stringResource(R.string.investment_type_fd)
    InvestmentType.RD -> stringResource(R.string.investment_type_rd)
    InvestmentType.SIP -> stringResource(R.string.investment_type_sip)
    InvestmentType.MUTUAL_FUND -> stringResource(R.string.investment_type_mutual_fund)
    InvestmentType.EQUITY -> stringResource(R.string.investment_type_equity)
    InvestmentType.GOLD_PHYSICAL -> stringResource(R.string.investment_type_gold_physical)
    InvestmentType.GOLD_DIGITAL -> stringResource(R.string.investment_type_gold_digital)
    InvestmentType.BONDS -> stringResource(R.string.investment_type_bonds)
    InvestmentType.PPF -> stringResource(R.string.investment_type_ppf)
    InvestmentType.EPF -> stringResource(R.string.investment_type_epf)
    InvestmentType.NPS -> stringResource(R.string.investment_type_nps)
    InvestmentType.ULIP -> stringResource(R.string.investment_type_ulip)
    InvestmentType.OTHER -> stringResource(R.string.investment_type_other)
}
