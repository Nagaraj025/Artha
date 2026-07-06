package com.subramanya.artha.ui.accounts

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
import androidx.compose.material.icons.filled.Unarchive
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
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.utils.IndianNumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(accountId: String, onBack: () -> Unit, onOpenTransaction: (String) -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: AccountDetailViewModel = viewModel(
        factory = AccountDetailViewModelFactory(
            accountId = accountId,
            accountRepository = app.accountRepository,
            transactionRepository = app.transactionRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    /** Non-null when the Edit sheet is open. */
    var editing: Account? by remember { mutableStateOf(null) }

    Surface(
        color = com.subramanya.artha.ui.theme.Surface1,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val account = state.account
            com.subramanya.artha.ui.common.InlineTopBar(
                title = account?.name.orEmpty(),
                onBack = onBack,
                trailing = {
                    if (account != null) {
                        IconButton(onClick = { editing = account }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.account_detail_action_edit),
                                tint = com.subramanya.artha.ui.theme.Text2,
                            )
                        }
                        if (account.isArchived) {
                            IconButton(onClick = { vm.restore(onRestored = onBack) }) {
                                Icon(
                                    Icons.Filled.Unarchive,
                                    contentDescription = stringResource(R.string.account_detail_action_restore),
                                    tint = com.subramanya.artha.ui.theme.Text2,
                                )
                            }
                        } else {
                            IconButton(onClick = vm::requestArchive) {
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
            if (account == null) {
                com.subramanya.artha.ui.common.LoadingPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AccountDetailBody(
                    account = account,
                    state = state,
                    onOpenTransaction = onOpenTransaction,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (state.showArchiveConfirm) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = vm::dismissArchiveConfirm,
            title = stringResource(R.string.account_detail_archive_confirm_title),
            text = stringResource(R.string.account_detail_archive_confirm_body),
            confirmLabel = stringResource(R.string.account_detail_archive_confirm_yes),
            onConfirm = { vm.confirmArchive(onArchived = onBack) },
            cancelLabel = stringResource(R.string.common_cancel),
            onCancel = vm::dismissArchiveConfirm,
        )
    }

    if (state.showDeleteConfirm) {
        if (state.transactions.isNotEmpty()) {
            // Deleting would orphan these transactions — steer the user to Archive instead.
            com.subramanya.artha.ui.common.ArthaAlertDialog(
                onDismissRequest = vm::dismissDeleteConfirm,
                title = stringResource(R.string.account_delete_blocked_title),
                text = stringResource(R.string.account_delete_blocked_body, state.transactions.size),
                confirmLabel = stringResource(R.string.account_delete_blocked_archive),
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
                title = stringResource(R.string.account_delete_confirm_title),
                text = stringResource(R.string.account_delete_confirm_body),
                confirmLabel = stringResource(R.string.account_delete_confirm_yes),
                confirmDestructive = true,
                onConfirm = { vm.confirmDelete(onDeleted = onBack) },
                cancelLabel = stringResource(R.string.common_cancel),
                onCancel = vm::dismissDeleteConfirm,
            )
        }
    }

    val currentlyEditing = editing
    if (currentlyEditing != null) {
        AccountFormSheet(editing = currentlyEditing, onDismiss = { editing = null })
    }
}

// ---------------- body ----------------

@Composable
private fun AccountDetailBody(
    account: Account,
    state: AccountDetailUiState,
    onOpenTransaction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item("header") { Header(account = account) }
        item("hero") { Hero(state = state) }
        item("chart") { ChartSection(state = state) }
        item("txnsHeader") {
            Text(
                text = stringResource(R.string.account_detail_txns_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 4.dp),
            )
        }
        if (state.transactions.isEmpty()) {
            item("txnsEmpty") {
                EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = stringResource(R.string.account_detail_txns_empty),
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
private fun Header(account: Account) {
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
                .background(Color(account.color)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = Color.White)
        }
        Spacer(modifier = Modifier.padding(start = 12.dp))
        Column {
            Text(account.name, style = MaterialTheme.typography.titleLarge)
            val subtitle = buildString {
                if (!account.institution.isNullOrBlank()) append(account.institution)
                if (!account.accountNumberLast4.isNullOrBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append("••${account.accountNumberLast4}")
                }
            }
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Hero(state: AccountDetailUiState) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.account_detail_hero_current),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = IndianNumberFormat.format(state.currentBalance),
                style = ArthaAmountStyles.display,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HeroFigure(
                    labelRes = R.string.account_detail_hero_opening,
                    amount = state.account?.openingBalance ?: 0.0,
                )
                HeroFigure(
                    labelRes = R.string.account_detail_hero_total_in,
                    amount = state.totalIn,
                )
                HeroFigure(
                    labelRes = R.string.account_detail_hero_total_out,
                    amount = state.totalOut,
                )
            }
        }
    }
}

@Composable
private fun HeroFigure(labelRes: Int, amount: Double) {
    Column {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = IndianNumberFormat.format(amount),
            style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun ChartSection(state: AccountDetailUiState) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.account_detail_chart_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )
        if (state.chartPoints.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Inbox,
                title = stringResource(R.string.account_detail_chart_empty),
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

// ---------------- helpers (duplicated from DashboardScreen — extract in Session 10 if reused once more) ----------------

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
