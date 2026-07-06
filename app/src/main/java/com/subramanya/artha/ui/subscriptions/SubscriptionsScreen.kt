package com.subramanya.artha.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.SubscriptionFrequency
import com.subramanya.artha.data.entity.enums.SubscriptionStatus
import com.subramanya.artha.domain.model.Subscription
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.LineTeal
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val all by app.subscriptionRepository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val active by app.subscriptionRepository.observeActive().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    var formMode: FormMode? by remember { mutableStateOf(null) }
    var pendingDelete: Subscription? by remember { mutableStateOf(null) }

    val monthlyAverage = app.subscriptionRepository.annualisedMonthlyAverage(active)
    val yearly = monthlyAverage * 12.0

    Surface(color = Surface1, modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Surface1,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { formMode = FormMode.Add },
                    containerColor = Teal700,
                    contentColor = Text1,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.subscriptions_fab_add)) },
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    com.subramanya.artha.ui.common.InlineTopBar(
                        title = stringResource(R.string.subscriptions_title),
                        onBack = onBack,
                    )
                }
                if (active.isNotEmpty()) {
                    item {
                        SubscriptionsHero(monthly = monthlyAverage, yearly = yearly)
                    }
                }
                if (all.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(
                                icon = Icons.Filled.Subscriptions,
                                title = stringResource(R.string.subscriptions_empty),
                            )
                        }
                    }
                } else {
                    items(all, key = { it.id }) { sub ->
                        SubscriptionRow(
                            sub = sub,
                            onTap = { formMode = FormMode.Edit(sub) },
                            onDelete = { pendingDelete = sub },
                        )
                    }
                }
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        SubscriptionFormSheet(
            editing = (mode as? FormMode.Edit)?.subscription,
            onSave = { resolved ->
                scope.launch {
                    app.subscriptionRepository.upsert(resolved)
                    formMode = null
                }
            },
            onDismiss = { formMode = null },
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = stringResource(R.string.subscriptions_delete_confirm_title),
            text = stringResource(R.string.subscriptions_delete_confirm_body),
            confirmLabel = stringResource(R.string.subscriptions_delete_confirm_yes),
            confirmDestructive = true,
            onConfirm = {
                scope.launch {
                    app.subscriptionRepository.delete(toDelete)
                    pendingDelete = null
                }
            },
            cancelLabel = stringResource(R.string.common_cancel),
            onCancel = { pendingDelete = null },
        )
    }
}

private sealed interface FormMode {
    data object Add : FormMode
    data class Edit(val subscription: Subscription) : FormMode
}

/** HANDOFF §3.7 — "Monthly outgo (annualised)" hero card with editorial number. */
@Composable
private fun SubscriptionsHero(monthly: Double, yearly: Double) {
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LineTeal, RoundedCornerShape(18.dp)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                text = stringResource(R.string.subscriptions_hero_monthly).uppercase(),
                style = EyebrowStyle,
                color = Text3,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = IndianNumberFormat.format(monthly),
                style = TextStyle(
                    fontFamily = InstrumentSerif,
                    fontSize = 40.sp,
                    lineHeight = 46.sp,
                    color = Text1,
                    fontFeatureSettings = "tnum, lnum",
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.subscriptions_hero_yearly, IndianNumberFormat.format(yearly)),
                style = TextStyle(
                    fontFamily = IbmPlexMono,
                    fontSize = 12.sp,
                    color = Text3,
                    fontFeatureSettings = "tnum, lnum",
                ),
            )
        }
    }
}

@Composable
private fun SubscriptionRow(sub: Subscription, onTap: () -> Unit, onDelete: () -> Unit) {
    val isPaused = sub.status != SubscriptionStatus.ACTIVE
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isPaused) Line1 else LineTeal.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onTap),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Surface4),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Subscriptions,
                        contentDescription = null,
                        tint = if (isPaused) Text3 else Teal300,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sub.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isPaused) Text2 else Text1,
                    )
                    Spacer(Modifier.height(2.dp))
                    val pieces = buildList {
                        sub.provider?.takeIf { it.isNotBlank() }?.let { add(it) }
                        add(sub.frequency.label())
                        add(stringResource(R.string.subscriptions_row_due, DateFormatter.longDate(sub.nextDueDate)))
                    }
                    Text(
                        text = pieces.joinToString(" · "),
                        style = TextStyle(
                            fontFamily = IbmPlexMono,
                            fontSize = 11.sp,
                            color = Text3,
                            fontFeatureSettings = "tnum, lnum",
                        ),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = IndianNumberFormat.format(sub.amount),
                        style = TextStyle(
                            fontFamily = InstrumentSerif,
                            fontSize = 18.sp,
                            color = if (isPaused) Text3 else Text1,
                            fontFeatureSettings = "tnum, lnum",
                        ),
                    )
                    if (isPaused) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.subscriptions_paused).uppercase(),
                            style = EyebrowStyle,
                            color = Text3,
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Text3,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionFrequency.label(): String = when (this) {
    SubscriptionFrequency.MONTHLY -> stringResource(R.string.subscription_freq_monthly)
    SubscriptionFrequency.QUARTERLY -> stringResource(R.string.subscription_freq_quarterly)
    SubscriptionFrequency.YEARLY -> stringResource(R.string.subscription_freq_yearly)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionFormSheet(editing: Subscription?, onSave: (Subscription) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var provider by remember(editing) { mutableStateOf(editing?.provider.orEmpty()) }
    var amountText by remember(editing) { mutableStateOf(editing?.amount?.toPlainString() ?: "") }
    var freq by remember(editing) { mutableStateOf(editing?.frequency ?: SubscriptionFrequency.MONTHLY) }
    var status by remember(editing) { mutableStateOf(editing?.status ?: SubscriptionStatus.ACTIVE) }
    var nextDue by remember(editing) { mutableStateOf(editing?.nextDueDate ?: System.currentTimeMillis()) }
    var pickingDue by remember { mutableStateOf(false) }

    val freqOptions = listOf(
        com.subramanya.artha.ui.common.PillOption(SubscriptionFrequency.MONTHLY, stringResource(R.string.subscription_freq_monthly)),
        com.subramanya.artha.ui.common.PillOption(SubscriptionFrequency.QUARTERLY, stringResource(R.string.subscription_freq_quarterly)),
        com.subramanya.artha.ui.common.PillOption(SubscriptionFrequency.YEARLY, stringResource(R.string.subscription_freq_yearly)),
    )
    val statusOptions = SubscriptionStatus.entries.map {
        com.subramanya.artha.ui.common.PillOption(it, it.name)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.subramanya.artha.ui.theme.Surface3,
        contentWindowInsets = com.subramanya.artha.ui.common.SheetWindowInsets,
        dragHandle = { com.subramanya.artha.ui.common.ArthaSheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            com.subramanya.artha.ui.common.SheetTitle(
                title = stringResource(
                    if (editing == null) R.string.subscriptions_form_add_title else R.string.subscriptions_form_edit_title,
                ),
            )

            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.subscriptions_form_name_label)) {
                com.subramanya.artha.ui.common.ArthaTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Spotify Family",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
            com.subramanya.artha.ui.common.FieldRow(
                label = stringResource(R.string.subscriptions_form_provider_label),
                optional = true,
            ) {
                com.subramanya.artha.ui.common.ArthaTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    placeholder = "Spotify",
                )
            }
            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.subscriptions_form_amount_label)) {
                com.subramanya.artha.ui.common.ArthaTextField(
                    value = amountText,
                    onValueChange = { v ->
                        amountText = v.filterIndexed { i, c -> c.isDigit() || (c == '.' && v.indexOf('.') == i) }
                    },
                    placeholder = "179",
                    suffix = "₹",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.subscriptions_form_frequency_label)) {
                com.subramanya.artha.ui.common.PillRadio(
                    value = freq,
                    options = freqOptions,
                    onChange = { freq = it },
                )
            }
            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.subscriptions_form_status_label)) {
                com.subramanya.artha.ui.common.PillRadio(
                    value = status,
                    options = statusOptions,
                    onChange = { status = it },
                )
            }
            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.subscriptions_form_next_due_label)) {
                androidx.compose.material3.AssistChip(
                    onClick = { pickingDue = true },
                    label = { androidx.compose.material3.Text(com.subramanya.artha.utils.DateFormatter.longDate(nextDue)) },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                        )
                    },
                )
            }

            Spacer(Modifier.height(28.dp))
            com.subramanya.artha.ui.common.SavePrimaryButton(
                label = stringResource(R.string.common_save),
                // Require name AND amount — used to silently default to
                // "Subscription".
                enabled = name.isNotBlank() && amountText.toDoubleOrNull() != null,
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@SavePrimaryButton
                    val now = System.currentTimeMillis()
                    onSave(
                        Subscription(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            provider = provider.trim().takeIf { it.isNotBlank() },
                            amount = amount,
                            frequency = freq,
                            nextDueDate = nextDue,
                            lastPaidDate = editing?.lastPaidDate,
                            categoryId = editing?.categoryId,
                            paymentMethodType = editing?.paymentMethodType,
                            paymentMethodId = editing?.paymentMethodId,
                            status = status,
                            autoCharge = editing?.autoCharge ?: false,
                            logoUri = editing?.logoUri,
                            color = editing?.color ?: 0xFF6D28D9L,
                            createdAt = editing?.createdAt ?: now,
                        ),
                    )
                },
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    if (pickingDue) {
        val pickerState = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = nextDue)
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { pickingDue = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { nextDue = it }
                    pickingDue = false
                }) { androidx.compose.material3.Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pickingDue = false }) {
                    androidx.compose.material3.Text(stringResource(R.string.common_cancel))
                }
            },
        ) { androidx.compose.material3.DatePicker(state = pickerState) }
    }
}

private fun Double.toPlainString(): String = if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()
