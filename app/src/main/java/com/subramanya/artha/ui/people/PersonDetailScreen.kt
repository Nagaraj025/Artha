package com.subramanya.artha.ui.people

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Person
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.ui.common.AutoShrinkAmountText
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.ui.theme.Danger
import com.subramanya.artha.ui.theme.Expense
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.IncomeSoft
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal900
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Per-person detail. Mirrors the structure of AccountDetailScreen:
 *   - Editorial top bar with Edit + Delete
 *   - Hero block with three figures (Net / They owe you / You owe them)
 *   - Full transaction list filtered to txns tagged with this person
 *
 * Tapping the row on PeopleScreen routes here; Edit on the top bar opens the
 * same PersonFormSheet used by the list-screen Add/Edit flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(personId: String, onBack: () -> Unit, onOpenTransaction: (String) -> Unit = {}, modifier: Modifier = Modifier) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ArthaApplication
    val vm: PersonDetailViewModel = viewModel(
        factory = PersonDetailViewModelFactory(
            personId = personId,
            personRepository = app.personRepository,
            transactionRepository = app.transactionRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var editing: Person? by remember { mutableStateOf(null) }

    // If the person row gets deleted out from under us (e.g. the user nukes
    // it from this screen), bounce back to the list — there's nothing left
    // to show. The `person == null` after the initial load means deletion;
    // we skip the very first emit (loading) via a flag.
    var seenPerson by remember { mutableStateOf(false) }
    LaunchedEffect(state.person) {
        if (state.person != null) {
            seenPerson = true
        } else if (seenPerson) {
            onBack()
        }
    }

    Surface(color = Surface1, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            val person = state.person
            com.subramanya.artha.ui.common.InlineTopBar(
                title = person?.name.orEmpty(),
                onBack = onBack,
                trailing = {
                    if (person != null) {
                        IconButton(onClick = { editing = person }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.people_action_edit),
                                tint = Text2,
                            )
                        }
                        IconButton(onClick = vm::requestDelete) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.people_delete_confirm_yes),
                                tint = Danger,
                            )
                        }
                    }
                },
            )
            if (person == null) {
                com.subramanya.artha.ui.common.LoadingPlaceholder(modifier = Modifier.fillMaxSize())
            } else {
                Body(state = state, onOpenTransaction = onOpenTransaction)
            }
        }
    }

    val editingNow = editing
    if (editingNow != null) {
        PersonFormSheet(
            editing = editingNow,
            onSave = { resolved ->
                vm.upsert(resolved)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    if (state.showDeleteConfirm) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = vm::dismissDeleteConfirm,
            title = stringResource(R.string.people_delete_confirm_title),
            text = stringResource(R.string.people_delete_confirm_body),
            confirmLabel = stringResource(R.string.people_delete_confirm_yes),
            confirmDestructive = true,
            onConfirm = { vm.confirmDelete(onDeleted = onBack) },
            cancelLabel = stringResource(R.string.common_cancel),
            onCancel = vm::dismissDeleteConfirm,
        )
    }
}

@Composable
private fun Body(state: PersonDetailUiState, onOpenTransaction: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item("hero") { Hero(state) }
        item("txnsHeader") {
            Text(
                text = stringResource(R.string.person_detail_txns_title).uppercase(),
                style = EyebrowStyle,
                color = Text3,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 6.dp),
            )
        }
        if (state.transactions.isEmpty()) {
            item("txnsEmpty") {
                EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = stringResource(R.string.person_detail_txns_empty),
                )
            }
        } else {
            items(state.transactions, key = { it.id }) { txn ->
                TxnRow(txn = txn, onClick = { onOpenTransaction(txn.id) })
            }
        }
        item("bottomSpacer") { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun Hero(state: PersonDetailUiState) {
    val net = state.netBalance
    val netColor = when {
        kotlin.math.abs(net) < 0.005 -> Text2
        net > 0 -> Income
        else -> Expense
    }
    val statusRes = when {
        kotlin.math.abs(net) < 0.005 -> R.string.people_settled
        net > 0 -> R.string.people_owes_you
        else -> R.string.people_you_owe
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Surface2)
            .border(1.dp, Line1, RoundedCornerShape(20.dp)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.person_detail_hero_net).uppercase(),
                style = EyebrowStyle,
                color = Text3,
            )
            Spacer(Modifier.height(6.dp))
            AutoShrinkAmountText(
                text = IndianNumberFormat.format(kotlin.math.abs(net)),
                color = netColor,
                style = ArthaAmountStyles.hero.copy(
                    fontSize = 40.sp,
                    lineHeight = 44.sp,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(statusRes),
                style = TextStyle(
                    fontFamily = IbmPlexMono,
                    fontSize = 11.sp,
                    color = Text3,
                    fontFeatureSettings = "tnum, lnum",
                ),
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HeroFigure(
                    labelRes = R.string.person_detail_hero_they_owe,
                    amount = state.theyOweYou,
                    color = Income,
                    modifier = Modifier.weight(1f),
                )
                HeroFigure(
                    labelRes = R.string.person_detail_hero_you_owe,
                    amount = state.youOweThem,
                    color = Expense,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeroFigure(labelRes: Int, amount: Double, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface4)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = stringResource(labelRes).uppercase(),
            style = EyebrowStyle,
            color = Text3,
        )
        Spacer(Modifier.height(4.dp))
        AutoShrinkAmountText(
            text = IndianNumberFormat.format(amount),
            color = color,
            style = TextStyle(
                fontFamily = InstrumentSerif,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
                fontFeatureSettings = "tnum, lnum",
            ),
        )
    }
}

@Composable
private fun TxnRow(txn: Transaction, onClick: () -> Unit) {
    val isIncome = txn.type in INCOMEISH
    val isOutflow = txn.type in OUTFLOWISH
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isIncome) IncomeSoft else Teal900),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = txn.description.firstOrNull()?.uppercaseChar()?.toString() ?: "•",
                color = if (isIncome) Income else Teal300,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = txn.description.ifBlank { txn.type.name.lowercase().replace('_', ' ') },
                style = MaterialTheme.typography.bodyMedium,
                color = Text1,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            val date = remember(txn.date) {
                DateFormatter.shortDate(
                    Instant.fromEpochMilliseconds(txn.date)
                        .toLocalDateTime(TimeZone.currentSystemDefault()).date,
                )
            }
            Text(
                text = date + " · " + txn.type.name.lowercase().replace('_', ' '),
                style = TextStyle(
                    fontFamily = IbmPlexMono,
                    fontSize = 11.sp,
                    color = Text3,
                    fontFeatureSettings = "tnum, lnum",
                ),
            )
        }
        val signed = when {
            isIncome -> "+" + IndianNumberFormat.format(txn.amount)
            isOutflow -> "−" + IndianNumberFormat.format(txn.amount)
            else -> IndianNumberFormat.format(txn.amount)
        }
        val amountColor = when {
            isIncome -> Income
            isOutflow -> Expense
            else -> Text1
        }
        Text(
            text = signed,
            color = amountColor,
            style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

// Only the types that actually move a person's net balance render as a signed row, so the row
// signs agree with the Net / "they owe" / "you owe" hero figures. REFUND/CASHBACK/INTEREST are
// deliberately omitted — they don't count toward a person ledger (see PeopleViewModel net math).
private val INCOMEISH: Set<TransactionType> = setOf(
    TransactionType.INCOME,
    TransactionType.LOAN_RECEIVED,
    TransactionType.GIFT_RECEIVED,
)

private val OUTFLOWISH: Set<TransactionType> = setOf(
    TransactionType.EXPENSE,
    TransactionType.LOAN_GIVEN,
    TransactionType.GIFT_SENT,
)
