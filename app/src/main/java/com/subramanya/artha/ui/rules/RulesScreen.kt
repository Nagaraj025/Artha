package com.subramanya.artha.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.domain.model.TransactionRule
import com.subramanya.artha.domain.rules.AmountOp
import com.subramanya.artha.domain.rules.ConditionLogic
import com.subramanya.artha.domain.rules.RuleAction
import com.subramanya.artha.domain.rules.RuleCondition
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.LineTeal
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface3
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Teal900
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.IndianNumberFormat

/**
 * HANDOFF §3.7 Rules — rows render WHEN / THEN clause badges (10sp uppercase
 * mono, Surface3 background for WHEN, Teal900 background for THEN). Top of
 * screen carries the editorial eyebrow + title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: RulesViewModel = viewModel(
        factory = RulesViewModelFactory(app.transactionRuleRepository),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    var formMode: RuleFormMode? by remember { mutableStateOf(null) }
    var pendingDelete: TransactionRule? by remember { mutableStateOf(null) }

    Surface(color = Surface1, modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Surface1,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { formMode = RuleFormMode.Add },
                    containerColor = Teal700,
                    contentColor = Text1,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.rules_fab_add)) },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                com.subramanya.artha.ui.common.InlineTopBar(
                    title = stringResource(R.string.rules_title),
                    onBack = onBack,
                )
                if (state.rules.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            icon = Icons.AutoMirrored.Filled.Rule,
                            title = stringResource(R.string.rules_empty),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 20.dp,
                            vertical = 12.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.rules, key = { it.id }) { rule ->
                            RuleRowCard(
                                rule = rule,
                                onTap = { formMode = RuleFormMode.Edit(rule) },
                                onToggle = { vm.toggleActive(rule, it) },
                                onDelete = { pendingDelete = rule },
                            )
                        }
                    }
                }
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        RuleFormSheet(
            editing = (mode as? RuleFormMode.Edit)?.rule,
            onDismiss = { formMode = null },
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = stringResource(R.string.rules_delete_confirm_title),
            text = if (toDelete.isSystem) {
                stringResource(R.string.rules_delete_system_warning)
            } else {
                stringResource(R.string.rules_delete_confirm_body)
            },
            confirmLabel = stringResource(R.string.rules_delete_confirm_yes),
            confirmDestructive = true,
            onConfirm = {
                vm.delete(toDelete)
                pendingDelete = null
            },
            cancelLabel = stringResource(R.string.common_cancel),
            onCancel = { pendingDelete = null },
        )
    }
}

internal sealed interface RuleFormMode {
    data object Add : RuleFormMode
    data class Edit(val rule: TransactionRule) : RuleFormMode
}

@Composable
private fun RulesEditorialHeader() {
    Column {
        Text(
            text = stringResource(R.string.rules_eyebrow).uppercase(),
            style = EyebrowStyle,
            color = Teal300,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.rules_title),
            style = TextStyle(
                fontFamily = InstrumentSerif,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Normal,
                color = Text1,
            ),
        )
        Spacer(Modifier.height(4.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuleRowCard(rule: TransactionRule, onTap: () -> Unit, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (rule.isActive) LineTeal else Line1, RoundedCornerShape(16.dp))
            .clickable(onClick = onTap),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // ── header: rule name + active switch + delete ─────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Text1,
                    )
                    if (rule.isSystem) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.rules_row_system_tag).uppercase(),
                            style = EyebrowStyle,
                            color = Teal300,
                        )
                    }
                }
                Switch(
                    checked = rule.isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Text1,
                        checkedTrackColor = Teal700,
                        uncheckedThumbColor = Text2,
                        uncheckedTrackColor = Surface3,
                        uncheckedBorderColor = Line1,
                    ),
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.rules_delete_confirm_yes),
                        tint = Text3,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── WHEN clause ─────────────────────────────────────────────────
            ClauseHeader(
                label = stringResource(R.string.rules_when),
                color = Text3,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (rule.conditions.items.isEmpty()) {
                    WhenBadge(stringResource(R.string.rules_no_conditions))
                } else {
                    val logicLabel = when (rule.conditions.logic) {
                        ConditionLogic.ALL -> stringResource(R.string.rules_logic_all)
                        ConditionLogic.ANY -> stringResource(R.string.rules_logic_any)
                    }
                    LogicBadge(logicLabel)
                    rule.conditions.items.forEach { c -> WhenBadge(describe(c)) }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── THEN clause ─────────────────────────────────────────────────
            ClauseHeader(
                label = stringResource(R.string.rules_then),
                color = Teal300,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rule.actions.items.forEach { a -> ThenBadge(describe(a)) }
                if (rule.actions.items.isEmpty()) {
                    WhenBadge("—")
                }
            }
        }
    }
}

@Composable
private fun ClauseHeader(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .height(1.dp)
                .background(color)
                .size(width = 10.dp, height = 1.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = label.uppercase(),
            style = EyebrowStyle,
            color = color,
        )
    }
}

/** Surface3 chip with Line1 border, mono 10sp uppercase letter-spacing 0.06em. */
@Composable
private fun WhenBadge(text: String) {
    Surface(
        color = Surface3,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.border(1.dp, Line1, RoundedCornerShape(6.dp)),
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(
                fontFamily = IbmPlexMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Text2,
                letterSpacing = 0.06.em,
                fontFeatureSettings = "tnum, lnum",
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** Teal900 chip with Teal500 border + Teal300 text — the THEN badge. */
@Composable
private fun ThenBadge(text: String) {
    Surface(
        color = Teal900,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.border(1.dp, Teal500, RoundedCornerShape(6.dp)),
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(
                fontFamily = IbmPlexMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Teal300,
                letterSpacing = 0.06.em,
                fontFeatureSettings = "tnum, lnum",
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** Pill that calls out ALL vs ANY logic so the user can read the clause at a glance. */
@Composable
private fun LogicBadge(text: String) {
    Surface(
        color = Surface4,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.border(1.dp, Line1, RoundedCornerShape(6.dp)),
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(
                fontFamily = IbmPlexMono,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Text3,
                letterSpacing = 0.08.em,
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// ─────────────────────────── condition / action describers ────────────────

private fun describe(c: RuleCondition): String = when (c) {
    is RuleCondition.DescriptionContains -> "DESC ~ \"${c.text.take(18)}\""
    is RuleCondition.AmountCompare -> "AMT ${opLabel(c.op)} ₹${IndianNumberFormat.format(c.value).removePrefix("₹")}"
    is RuleCondition.SourceIs -> "SOURCE = ${c.kind.name}"
    is RuleCondition.DestinationIs -> "DEST = ${c.kind.name}"
    is RuleCondition.PaymentAppIs -> "APP = ${c.app.name}"
    is RuleCondition.TypeIs -> "TYPE = ${c.type.name}"
    is RuleCondition.HasPersonRelation -> "PERSON = ${c.relation.name}"
    is RuleCondition.TimeOfDayBetween -> "TIME ${fmtMin(c.fromMinuteOfDay)}–${fmtMin(c.toMinuteOfDay)}"
}

private fun describe(a: RuleAction): String = when (a) {
    is RuleAction.SetType -> "SET TYPE ${a.type.name}"
    is RuleAction.SetCategory -> "SET CATEGORY"
    is RuleAction.SetTaxSection -> "TAX ${a.section}"
    is RuleAction.AddTag -> "+ TAG"
    is RuleAction.AddPerson -> "+ PERSON"
    RuleAction.ExcludeFromExpenseTotal -> "EXCLUDE FROM EXPENSE"
    RuleAction.PromptSpouse -> "ASK SPOUSE"
}

private fun opLabel(op: AmountOp): String = when (op) {
    AmountOp.EQ -> "="
    AmountOp.GT -> ">"
    AmountOp.LT -> "<"
    AmountOp.GTE -> "≥"
    AmountOp.LTE -> "≤"
}

private fun fmtMin(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)
