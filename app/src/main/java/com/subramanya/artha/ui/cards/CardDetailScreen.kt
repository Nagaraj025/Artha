package com.subramanya.artha.ui.cards

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.ui.accounts.BalanceLineChart
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.ui.transaction.AddTransactionSheet
import com.subramanya.artha.ui.transaction.AddTransactionViewModel
import com.subramanya.artha.ui.transaction.AddTransactionViewModelFactory
import com.subramanya.artha.ui.transaction.FundsEndpoint
import com.subramanya.artha.utils.IndianNumberFormat
import com.subramanya.artha.domain.model.Card as DomainCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(cardId: String, onBack: () -> Unit, onOpenTransaction: (String) -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: CardDetailViewModel = viewModel(
        factory = CardDetailViewModelFactory(
            cardId = cardId,
            cardRepository = app.cardRepository,
            transactionRepository = app.transactionRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    var editing: DomainCard? by remember { mutableStateOf(null) }
    var payBill by remember { mutableStateOf(false) }

    Surface(
        color = com.subramanya.artha.ui.theme.Surface1,
        modifier = modifier.fillMaxSize(),
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            val card = state.card
            com.subramanya.artha.ui.common.InlineTopBar(
                title = card?.name.orEmpty(),
                onBack = onBack,
                trailing = {
                    if (card != null) {
                        IconButton(onClick = { editing = card }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.card_detail_action_edit),
                                tint = com.subramanya.artha.ui.theme.Text2,
                            )
                        }
                        if (card.isArchived) {
                            IconButton(onClick = { vm.restore(onRestored = onBack) }) {
                                Icon(
                                    Icons.Filled.Unarchive,
                                    contentDescription = stringResource(R.string.card_detail_action_restore),
                                    tint = com.subramanya.artha.ui.theme.Text2,
                                )
                            }
                        } else {
                            IconButton(onClick = vm::requestArchive) {
                                Icon(
                                    Icons.Filled.Archive,
                                    contentDescription = stringResource(R.string.card_detail_action_archive),
                                    tint = com.subramanya.artha.ui.theme.Text2,
                                )
                            }
                        }
                        IconButton(onClick = vm::requestDelete) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.card_action_delete),
                                tint = com.subramanya.artha.ui.theme.Danger,
                            )
                        }
                    }
                },
            )
            if (card == null) {
                com.subramanya.artha.ui.common.LoadingPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CardDetailBody(
                    card = card,
                    state = state,
                    onPayBill = { payBill = true },
                    onOpenTransaction = onOpenTransaction,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (state.showArchiveConfirm) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = vm::dismissArchiveConfirm,
            title = stringResource(R.string.card_detail_archive_confirm_title),
            text = stringResource(R.string.card_detail_archive_confirm_body),
            confirmLabel = stringResource(R.string.card_detail_archive_confirm_yes),
            onConfirm = { vm.confirmArchive(onArchived = onBack) },
            cancelLabel = stringResource(R.string.common_cancel),
            onCancel = vm::dismissArchiveConfirm,
        )
    }

    if (state.showDeleteConfirm) {
        if (state.transactions.isNotEmpty()) {
            com.subramanya.artha.ui.common.ArthaAlertDialog(
                onDismissRequest = vm::dismissDeleteConfirm,
                title = stringResource(R.string.card_delete_blocked_title),
                text = stringResource(R.string.card_delete_blocked_body, state.transactions.size),
                confirmLabel = stringResource(R.string.card_delete_blocked_archive),
                confirmDestructive = false,
                onConfirm = {
                    vm.dismissDeleteConfirm()
                    vm.confirmArchive(onArchived = onBack)
                },
                cancelLabel = stringResource(R.string.common_cancel),
                onCancel = vm::dismissDeleteConfirm,
            )
        } else {
            com.subramanya.artha.ui.common.ArthaAlertDialog(
                onDismissRequest = vm::dismissDeleteConfirm,
                title = stringResource(R.string.card_delete_confirm_title),
                text = stringResource(R.string.card_delete_confirm_body),
                confirmLabel = stringResource(R.string.card_delete_confirm_yes),
                confirmDestructive = true,
                onConfirm = { vm.confirmDelete(onDeleted = onBack) },
                cancelLabel = stringResource(R.string.common_cancel),
                onCancel = vm::dismissDeleteConfirm,
            )
        }
    }

    val currentlyEditing = editing
    if (currentlyEditing != null) {
        CardFormSheet(editing = currentlyEditing, onDismiss = { editing = null })
    }

    if (payBill) {
        val cardSnapshot = state.card
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
        // Apply the Transfer-to-this-card prefill once the VM is alive.
        LaunchedEffect(cardSnapshot?.id) {
            if (cardSnapshot != null) {
                txnVm.applyPayBillPrefill(
                    toCard = FundsEndpoint(
                        kind = SourceKind.CARD,
                        id = cardSnapshot.id,
                        displayName = cardSnapshot.name,
                        isCreditCard = cardSnapshot.type == com.subramanya.artha.data.entity.enums.CardType.CREDIT,
                    ),
                )
            }
        }
        AddTransactionSheet(viewModel = txnVm, onDismiss = { payBill = false })
    }
}

// ---------------- body ----------------

@Composable
private fun CardDetailBody(
    card: DomainCard,
    state: CardDetailUiState,
    onPayBill: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item("header") { Header(card = card) }
        item("hero") { Hero(card = card, state = state) }
        if (card.type == com.subramanya.artha.data.entity.enums.CardType.CREDIT) {
            item("payBill") { PayBillButton(onPayBill = onPayBill) }
        }
        item("chart") { ChartSection(state = state) }
        item("txnsHeader") {
            Text(
                text = stringResource(R.string.card_detail_txns_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 4.dp),
            )
        }
        if (state.transactions.isEmpty()) {
            item("txnsEmpty") {
                EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = stringResource(R.string.card_detail_txns_empty),
                )
            }
        } else {
            items(state.transactions, key = { it.id }) { txn ->
                TxnRow(txn, onClick = { onOpenTransaction(txn.id) })
            }
        }
        item("bottomSpacer") { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun Header(card: DomainCard) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(card.color)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.CreditCard, contentDescription = null, tint = Color.White)
        }
        Spacer(modifier = Modifier.padding(start = 12.dp))
        Column {
            Text(card.name, style = MaterialTheme.typography.titleLarge)
            val subtitle = buildString {
                append(card.network.name)
                if (!card.issuer.isNullOrBlank()) append(" · ${card.issuer}")
                if (!card.cardNumberLast4.isNullOrBlank()) append(" · ••${card.cardNumberLast4}")
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Hero(card: DomainCard, state: CardDetailUiState) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.card_detail_hero_outstanding),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = IndianNumberFormat.format(state.currentOutstanding),
                style = ArthaAmountStyles.display,
            )
            if (card.type == com.subramanya.artha.data.entity.enums.CardType.CREDIT && card.creditLimit != null) {
                Spacer(Modifier.height(12.dp))
                state.utilizationFraction?.let { fraction ->
                    com.subramanya.artha.ui.common.LinearMeter(
                        fraction = fraction,
                        fillColor = if (fraction > 0.3f) {
                            com.subramanya.artha.ui.theme.Ochre
                        } else {
                            com.subramanya.artha.ui.theme.Teal500
                        },
                    )
                    Text(
                        text = stringResource(R.string.cards_utilization_label, (fraction * 100).toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    HeroFigure(
                        labelRes = R.string.card_detail_hero_credit_limit,
                        amount = card.creditLimit,
                    )
                    if (state.availableLimit != null) {
                        HeroFigure(
                            labelRes = R.string.card_detail_hero_available,
                            amount = state.availableLimit,
                        )
                    }
                }
                if (card.statementDayOfMonth != null || card.dueDayOfMonth != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (card.statementDayOfMonth != null) {
                            HeroDay(
                                labelRes = R.string.card_detail_hero_statement_day,
                                day = card.statementDayOfMonth,
                            )
                        }
                        if (card.dueDayOfMonth != null) {
                            HeroDay(
                                labelRes = R.string.card_detail_hero_due_day,
                                day = card.dueDayOfMonth,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroFigure(labelRes: Int, amount: Double) {
    Column {
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.labelSmall)
        Text(
            text = IndianNumberFormat.format(amount),
            style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun HeroDay(labelRes: Int, day: Int) {
    Column {
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.labelSmall)
        Text(text = day.toString(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun PayBillButton(onPayBill: () -> Unit) {
    Button(
        onClick = onPayBill,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(Icons.Filled.Payments, contentDescription = null)
        Text(
            text = stringResource(R.string.card_detail_action_pay_bill),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun ChartSection(state: CardDetailUiState) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.card_detail_chart_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )
        if (state.chartPoints.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Inbox,
                title = stringResource(R.string.card_detail_chart_empty),
            )
        } else {
            BalanceLineChart(values = state.chartPoints)
        }
    }
}

@Composable
private fun TxnRow(txn: Transaction, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = iconForType(txn.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = { Text(txn.description, maxLines = 1) },
        supportingContent = {
            Text(
                text = txn.type.name.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Text(
                text = signedAmount(txn),
                style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
                color = amountColor(txn.type),
            )
        },
    )
}

@Composable
private fun amountColor(type: TransactionType): Color = when (type) {
    TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK,
    TransactionType.INTEREST, TransactionType.LOAN_RECEIVED, TransactionType.GIFT_RECEIVED,
    -> MaterialTheme.colorScheme.primary
    TransactionType.EXPENSE, TransactionType.LOAN_GIVEN, TransactionType.GIFT_SENT,
    -> com.subramanya.artha.ui.theme.Danger
    else -> MaterialTheme.colorScheme.onSurface
}

private fun iconForType(type: TransactionType) = when (type) {
    TransactionType.CARD_PAYMENT -> Icons.Filled.CreditCard
    else -> Icons.Filled.AccountBalance
}

private fun signedAmount(txn: Transaction): String {
    val abs = IndianNumberFormat.format(txn.amount)
    return when (txn.type) {
        TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK,
        TransactionType.INTEREST, TransactionType.LOAN_RECEIVED, TransactionType.GIFT_RECEIVED,
        -> "+$abs"
        TransactionType.EXPENSE, TransactionType.LOAN_GIVEN, TransactionType.GIFT_SENT,
        -> "−$abs"
        else -> abs
    }
}
