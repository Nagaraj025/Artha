package com.subramanya.artha.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.RecurringFrequency
import com.subramanya.artha.domain.model.RecurringRule
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.LineTeal
import com.subramanya.artha.ui.theme.Ochre
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Teal900
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.DateFormatter
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val rules by app.recurringRuleRepository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    var formMode: FormMode? by remember { mutableStateOf(null) }
    var pendingDelete: RecurringRule? by remember { mutableStateOf(null) }

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
                    text = { Text(stringResource(R.string.recurring_fab_add)) },
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
                        title = stringResource(R.string.recurring_title),
                        onBack = onBack,
                    )
                }
                item { OchreInfoBanner(text = stringResource(R.string.recurring_banner)) }
                if (rules.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(icon = Icons.Filled.EventRepeat, title = stringResource(R.string.recurring_empty))
                        }
                    }
                } else {
                    items(rules, key = { it.id }) { rule ->
                        RecurringRow(
                            rule = rule,
                            onTap = { formMode = FormMode.Edit(rule) },
                            onToggle = { active ->
                                scope.launch { app.recurringRuleRepository.upsert(rule.copy(isActive = active)) }
                            },
                            onDelete = { pendingDelete = rule },
                        )
                    }
                }
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        RecurringFormSheet(
            editing = (mode as? FormMode.Edit)?.rule,
            onSave = { resolved ->
                scope.launch {
                    app.recurringRuleRepository.upsert(resolved)
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
            title = stringResource(R.string.recurring_delete_confirm_title),
            text = stringResource(R.string.recurring_delete_confirm_body),
            confirmLabel = stringResource(R.string.recurring_delete_confirm_yes),
            confirmDestructive = true,
            onConfirm = {
                scope.launch {
                    app.recurringRuleRepository.delete(toDelete)
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
    data class Edit(val rule: RecurringRule) : FormMode
}

/**
 * HANDOFF §3.7 — Recurring info-banner: ochre 10% fill + ochre 30% border,
 * with an info icon and the verbatim "Rules stored locally … Phase 5" copy.
 */
@Composable
private fun OchreInfoBanner(text: String) {
    Surface(
        color = Ochre.copy(alpha = 0.10f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Ochre.copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = Ochre,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = Text2,
            )
        }
    }
}

@Composable
private fun RecurringRow(rule: RecurringRule, onTap: () -> Unit, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (rule.isActive) LineTeal else Line1,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onTap),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = Teal300,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                    ),
                    color = Text1,
                    modifier = Modifier.weight(1f),
                )
                if (rule.autoConfirm) {
                    Surface(
                        color = Teal900,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.recurring_auto_chip).uppercase(),
                            style = TextStyle(
                                fontFamily = IbmPlexMono,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Teal300,
                                letterSpacing = 0.06.em,
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                }
                Switch(
                    checked = rule.isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Text1,
                        checkedTrackColor = Teal700,
                        uncheckedThumbColor = Text2,
                        uncheckedTrackColor = Surface4,
                        uncheckedBorderColor = Line1,
                    ),
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Text3,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (rule.transactionTemplate.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = rule.transactionTemplate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Text2,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val day = rule.dayOfPeriod
                val dayLabel = if (day != null) " · " + stringResource(R.string.recurring_day_fmt, day) else ""
                Text(
                    text = rule.frequency.label() + dayLabel + " · " +
                        stringResource(R.string.recurring_row_next_run, DateFormatter.longDate(rule.nextRunDate)),
                    style = TextStyle(
                        fontFamily = IbmPlexMono,
                        fontSize = 11.sp,
                        color = Text3,
                        fontFeatureSettings = "tnum, lnum",
                    ),
                )
            }
        }
    }
}

@Composable
private fun RecurringFrequency.label(): String = when (this) {
    RecurringFrequency.DAILY -> stringResource(R.string.recurring_freq_daily)
    RecurringFrequency.WEEKLY -> stringResource(R.string.recurring_freq_weekly)
    RecurringFrequency.MONTHLY -> stringResource(R.string.recurring_freq_monthly)
    RecurringFrequency.YEARLY -> stringResource(R.string.recurring_freq_yearly)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecurringFormSheet(editing: RecurringRule?, onSave: (RecurringRule) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var template by remember(editing) { mutableStateOf(editing?.transactionTemplate.orEmpty()) }
    var freq by remember(editing) { mutableStateOf(editing?.frequency ?: RecurringFrequency.MONTHLY) }
    var dayText by remember(editing) { mutableStateOf((editing?.dayOfPeriod ?: 1).toString()) }
    var autoConfirm by remember(editing) { mutableStateOf(editing?.autoConfirm ?: false) }

    val freqOptions = listOf(
        com.subramanya.artha.ui.common.PillOption(RecurringFrequency.DAILY, stringResource(R.string.recurring_freq_daily)),
        com.subramanya.artha.ui.common.PillOption(RecurringFrequency.WEEKLY, stringResource(R.string.recurring_freq_weekly)),
        com.subramanya.artha.ui.common.PillOption(RecurringFrequency.MONTHLY, stringResource(R.string.recurring_freq_monthly)),
        com.subramanya.artha.ui.common.PillOption(RecurringFrequency.YEARLY, stringResource(R.string.recurring_freq_yearly)),
    )

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
                    if (editing == null) R.string.recurring_form_add_title else R.string.recurring_form_edit_title,
                ),
                sub = stringResource(R.string.recurring_form_hint),
            )

            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.recurring_form_name_label)) {
                com.subramanya.artha.ui.common.ArthaTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Rent",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.recurring_form_template_label)) {
                com.subramanya.artha.ui.common.ArthaTextField(
                    value = template,
                    onValueChange = { template = it },
                    placeholder = "Rent — Bangalore flat",
                    singleLine = false,
                )
            }
            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.recurring_form_frequency_label)) {
                com.subramanya.artha.ui.common.PillRadio(
                    value = freq,
                    options = freqOptions,
                    onChange = { freq = it },
                )
            }
            if (freq == RecurringFrequency.MONTHLY || freq == RecurringFrequency.WEEKLY) {
                com.subramanya.artha.ui.common.FieldRow(
                    label = if (freq == RecurringFrequency.MONTHLY) {
                        stringResource(R.string.recurring_form_day_of_month)
                    } else {
                        stringResource(R.string.recurring_form_day_of_week)
                    },
                ) {
                    com.subramanya.artha.ui.common.ArthaTextField(
                        value = dayText,
                        onValueChange = { v -> dayText = v.filter { it.isDigit() }.take(2) },
                        placeholder = "1",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.subramanya.artha.ui.theme.Surface2, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.recurring_form_auto_confirm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = com.subramanya.artha.ui.theme.Text1,
                    )
                    Text(
                        text = "Fire and forget · for fixed amounts like rent",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.subramanya.artha.ui.theme.Text3,
                    )
                }
                Switch(checked = autoConfirm, onCheckedChange = { autoConfirm = it })
            }

            Spacer(Modifier.height(28.dp))
            com.subramanya.artha.ui.common.SavePrimaryButton(
                label = stringResource(R.string.common_save),
                enabled = name.isNotBlank() && template.isNotBlank(),
                onClick = {
                    val now = System.currentTimeMillis()
                    val day = dayText.toIntOrNull()
                    onSave(
                        RecurringRule(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            transactionTemplate = template.trim(),
                            frequency = freq,
                            dayOfPeriod = day,
                            nextRunDate = editing?.nextRunDate ?: now,
                            lastRunDate = editing?.lastRunDate,
                            autoConfirm = autoConfirm,
                            isActive = editing?.isActive ?: true,
                            createdAt = editing?.createdAt ?: now,
                        ),
                    )
                },
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}
