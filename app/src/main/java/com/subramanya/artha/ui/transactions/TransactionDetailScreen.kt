package com.subramanya.artha.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.ui.transaction.AddTransactionSheet
import com.subramanya.artha.ui.transaction.AddTransactionViewModel
import com.subramanya.artha.ui.transaction.AddTransactionViewModelFactory
import com.subramanya.artha.ui.transaction.FundsEndpoint
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(transactionId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: TransactionDetailViewModel = viewModel(
        factory = TransactionDetailViewModelFactory(
            transactionId = transactionId,
            transactionRepository = app.transactionRepository,
            accountRepository = app.accountRepository,
            cardRepository = app.cardRepository,
            categoryRepository = app.categoryRepository,
            personRepository = app.personRepository,
            tagRepository = app.tagRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }

    Surface(
        color = com.subramanya.artha.ui.theme.Surface1,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val txn = state.transaction
            com.subramanya.artha.ui.common.InlineTopBar(
                title = stringResource(R.string.txn_detail_title),
                onBack = onBack,
                trailing = {
                    if (txn != null) {
                        IconButton(onClick = { editing = true }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.txn_detail_action_edit),
                                tint = com.subramanya.artha.ui.theme.Text2,
                            )
                        }
                        IconButton(onClick = { vm.duplicate(onDuplicated = onBack) }) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = stringResource(R.string.txn_detail_action_duplicate),
                                tint = com.subramanya.artha.ui.theme.Text2,
                            )
                        }
                        IconButton(onClick = vm::requestDelete) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.txn_detail_action_delete),
                                tint = com.subramanya.artha.ui.theme.Danger,
                            )
                        }
                    }
                },
            )
            if (txn == null) {
                com.subramanya.artha.ui.common.LoadingPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    AmountHero(txn = txn)
                    Spacer(Modifier.height(16.dp))
                    FieldsCard(state = state, txn = txn)
                    Spacer(Modifier.height(16.dp))
                    AuditRow(txn = txn)
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    if (state.showDeleteConfirm) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = vm::dismissDeleteConfirm,
            title = stringResource(R.string.txn_detail_delete_confirm_title),
            text = stringResource(R.string.txn_detail_delete_confirm_body),
            confirmLabel = stringResource(R.string.txn_detail_delete_confirm_yes),
            confirmDestructive = true,
            onConfirm = { vm.confirmDelete(onDeleted = onBack) },
            cancelLabel = stringResource(R.string.common_cancel),
            onCancel = vm::dismissDeleteConfirm,
        )
    }

    if (editing) {
        val txn = state.transaction
        val source = txn?.let {
            it.sourceId?.let { id ->
                FundsEndpoint(
                    kind = it.sourceType,
                    id = id,
                    displayName = state.sourceName ?: id,
                    isCreditCard = false,
                )
            }
        }
        val destination = txn?.let { t ->
            val id = t.destinationId
            val kind = t.destinationType
            if (id != null && kind != null) {
                FundsEndpoint(
                    kind = kind,
                    id = id,
                    displayName = state.destinationName ?: id,
                    isCreditCard = t.type == TransactionType.CARD_PAYMENT,
                )
            } else {
                null
            }
        }
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
        LaunchedEffect(txn?.id) {
            if (txn != null) {
                txnVm.applyEditPrefill(
                    transaction = txn,
                    source = source,
                    destination = destination,
                    categoryDisplay = state.categoryName,
                    subCategoryDisplay = state.subCategoryName,
                )
            }
        }
        AddTransactionSheet(viewModel = txnVm, onDismiss = { editing = false })
    }
}

// ---------------- pieces ----------------

@Composable
private fun AmountHero(txn: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.txn_detail_field_amount), style = MaterialTheme.typography.titleSmall)
            Text(
                text = signedAmount(txn),
                style = ArthaAmountStyles.display.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = txn.type.name.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun FieldsCard(state: TransactionDetailUiState, txn: Transaction) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailRow(stringResource(R.string.txn_detail_field_date), formatDate(txn.date))
            DetailRow(stringResource(R.string.txn_detail_field_description), txn.description)
            state.sourceName?.let {
                DetailRow(stringResource(R.string.txn_detail_field_source), it)
            }
            state.destinationName?.let {
                DetailRow(stringResource(R.string.txn_detail_field_destination), it)
            }
            state.categoryName?.let {
                DetailRow(stringResource(R.string.txn_detail_field_category), it)
            }
            state.subCategoryName?.let {
                DetailRow(stringResource(R.string.txn_detail_field_subcategory), it)
            }
            DetailRow(
                stringResource(R.string.txn_detail_field_payment_app),
                txn.paymentApp.name.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() },
            )
            if (state.peopleNames.isNotEmpty()) {
                DetailRow(
                    stringResource(R.string.txn_detail_field_people),
                    state.peopleNames.joinToString(", "),
                )
            }
            if (!txn.place.isNullOrBlank()) {
                DetailRow(stringResource(R.string.txn_detail_field_place), txn.place)
            }
            if (state.tagNames.isNotEmpty()) {
                DetailRow(
                    stringResource(R.string.txn_detail_field_tags),
                    state.tagNames.joinToString(", "),
                )
            }
            if (!txn.receiptUri.isNullOrBlank()) {
                DetailRow(
                    stringResource(R.string.txn_detail_field_receipt),
                    stringResource(R.string.txn_detail_receipt_yes),
                )
            }
            if (!txn.notes.isNullOrBlank()) {
                DetailRow(stringResource(R.string.txn_detail_field_notes), txn.notes)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.height(1.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AuditRow(txn: Transaction) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.txn_detail_audit_created, formatDate(txn.createdAt)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (txn.updatedAt != txn.createdAt) {
            Text(
                text = stringResource(R.string.txn_detail_audit_updated, formatDate(txn.updatedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.txn_detail_audit_source, txn.source.name.lowercase().replaceFirstChar { it.titlecase() }),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------- helpers ----------------

private fun formatDate(millis: Long): String {
    val ldt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    val date = DateFormatter.shortDate(ldt.date)
    return "$date %02d:%02d".format(ldt.hour, ldt.minute)
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
