package com.subramanya.artha.ui.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.InvestmentType
import com.subramanya.artha.data.entity.enums.ValuationMode
import com.subramanya.artha.domain.model.Investment
import com.subramanya.artha.domain.model.InvestmentWithMetrics
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.utils.IndianNumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(onBack: () -> Unit, onOpenInvestment: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: InvestmentsViewModel = viewModel(
        factory = InvestmentsViewModelFactory(app.investmentRepository),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val toastMsg by vm.toastMessage.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(toastMsg) {
        toastMsg?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            vm.consumeToast()
        }
    }

    var formMode: FormMode? by remember { mutableStateOf(null) }
    var pendingDelete: Investment? by remember { mutableStateOf(null) }

    Surface(modifier = modifier.fillMaxSize(), color = com.subramanya.artha.ui.theme.Surface1) {
        Scaffold(
            containerColor = com.subramanya.artha.ui.theme.Surface1,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            floatingActionButton = {
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = { formMode = FormMode.Add },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    containerColor = com.subramanya.artha.ui.theme.Teal700,
                    contentColor = com.subramanya.artha.ui.theme.Text1,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.investments_fab_add)) },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                com.subramanya.artha.ui.common.InlineTopBar(
                    title = stringResource(R.string.investments_title),
                    onBack = onBack,
                )
                HeroCard(
                    invested = state.totalInvested,
                    currentValue = state.totalCurrentValue,
                )

                if (state.rows.isNotEmpty()) {
                    AllocationBar(rows = state.rows)
                }

                ViewToggleRow(
                    current = state.view,
                    onShowAll = vm::showAll,
                    onShowByType = vm::showByType,
                )

                if (state.rows.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.Savings,
                            title = stringResource(R.string.investments_empty),
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        when (state.view) {
                            InvestmentsView.ALL -> {
                                items(state.rows, key = { it.investment.id }) { row ->
                                    InvestmentRow(
                                        row = row,
                                        onTap = { onOpenInvestment(row.investment.id) },
                                        onEdit = { formMode = FormMode.Edit(row.investment) },
                                        onArchive = { vm.archive(row.investment) },
                                        onDelete = { pendingDelete = row.investment },
                                    )
                                }
                            }
                            InvestmentsView.BY_TYPE -> {
                                state.grouped.forEach { (type, rows) ->
                                    item(key = "header-${type.name}") {
                                        TypeHeader(type)
                                    }
                                    items(rows, key = { it.investment.id }) { row ->
                                        InvestmentRow(
                                            row = row,
                                            onTap = { onOpenInvestment(row.investment.id) },
                                            onEdit = { formMode = FormMode.Edit(row.investment) },
                                            onArchive = { vm.archive(row.investment) },
                                            onDelete = { pendingDelete = row.investment },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        InvestmentFormSheet(
            editing = (mode as? FormMode.Edit)?.investment,
            onDismiss = { formMode = null },
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = stringResource(R.string.investment_delete_confirm_title),
            text = stringResource(R.string.investment_delete_confirm_body),
            confirmLabel = stringResource(R.string.investment_delete_confirm_yes),
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

private sealed interface FormMode {
    data object Add : FormMode
    data class Edit(val investment: Investment) : FormMode
}

// ---------------- hero ----------------

@Composable
private fun HeroCard(invested: Double, currentValue: Double) {
    val gain = currentValue - invested
    val pct = if (invested == 0.0) Double.NaN else (gain / invested) * 100.0
    val positive = gain >= 0.0
    val gainColor =
        if (positive) MaterialTheme.colorScheme.primary else com.subramanya.artha.ui.theme.Danger

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.investments_hero_current),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = IndianNumberFormat.format(currentValue),
                style = ArthaAmountStyles.display,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
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
                    text = IndianNumberFormat.format(gain),
                    style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
                    color = gainColor,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = if (pct.isNaN()) "—" else "%+.2f%%".format(pct),
                    style = MaterialTheme.typography.bodyMedium,
                    color = gainColor,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.investments_hero_invested) +
                    ": " + IndianNumberFormat.format(invested),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

// ---------------- allocation bar ----------------

/**
 * Allocation horizontal stacked bar per HANDOFF §3.6 — 8dp tall, each segment
 * weighted by current value, separated by 1dp `surface-1` gaps. Legend below
 * shows one chip per unique investment type with a matching colour dot.
 */
@Composable
private fun AllocationBar(rows: List<com.subramanya.artha.domain.model.InvestmentWithMetrics>) {
    val total = rows.sumOf { it.value }
    if (total <= 0.0) return
    val palette = listOf(
        com.subramanya.artha.ui.theme.AccTeal,
        com.subramanya.artha.ui.theme.AccIndigo,
        com.subramanya.artha.ui.theme.AccSaffron,
        com.subramanya.artha.ui.theme.AccEmerald,
        com.subramanya.artha.ui.theme.AccMagenta,
        com.subramanya.artha.ui.theme.AccViolet,
    )
    Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
        Text(
            text = "ALLOCATION",
            style = com.subramanya.artha.ui.theme.EyebrowStyle,
            color = com.subramanya.artha.ui.theme.Text3,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
        ) {
            rows.forEachIndexed { i, row ->
                val weight = (row.value / total).toFloat().coerceAtLeast(0.001f)
                val tone = palette[i % palette.size]
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .background(tone)
                        .let { if (i < rows.size - 1) it.then(Modifier.padding(end = 1.dp)) else it },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // Legend — group by InvestmentType, show one chip per unique kind.
        val byType = rows.groupBy { it.investment.type }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            byType.entries.forEachIndexed { idx, (type, items) ->
                val first = rows.indexOf(items.first())
                val tone = palette[first % palette.size]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(tone),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = type.name.lowercase()
                            .replaceFirstChar { it.titlecase() }
                            .replace('_', ' '),
                        color = com.subramanya.artha.ui.theme.Text2,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ---------------- view toggle ----------------

@Composable
private fun ViewToggleRow(current: InvestmentsView, onShowAll: () -> Unit, onShowByType: () -> Unit) {
    // horizontalScroll keeps chips on one line on narrow phones (Categories/Transactions
    // already do the same).
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = current == InvestmentsView.ALL,
            onClick = onShowAll,
            label = { Text(stringResource(R.string.investments_view_all)) },
        )
        FilterChip(
            selected = current == InvestmentsView.BY_TYPE,
            onClick = onShowByType,
            label = { Text(stringResource(R.string.investments_view_by_type)) },
        )
    }
}

@Composable
private fun TypeHeader(type: InvestmentType) {
    Text(
        text = type.displayName(),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 4.dp),
    )
}

// ---------------- row ----------------

@Composable
private fun InvestmentRow(row: InvestmentWithMetrics, onTap: () -> Unit, onEdit: () -> Unit, onArchive: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val positive = row.absoluteGain >= 0.0
    val gainColor =
        if (positive) MaterialTheme.colorScheme.primary else com.subramanya.artha.ui.theme.Danger
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        colors = ListItemDefaults.colors(),
        leadingContent = { InvestmentAvatar(color = row.investment.color) },
        headlineContent = { Text(row.investment.name, maxLines = 1) },
        supportingContent = {
            val pieces = buildList {
                add(row.investment.type.displayName())
                row.investment.institution?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            Text(
                text = pieces.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    // Headline amount is the per-mode computed value (MARKET → current
                    // price; DERIVED → contributions + posted interest).
                    Text(
                        text = IndianNumberFormat.format(row.value),
                        style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
                    )
                    // Secondary line branches on valuation mode:
                    //  - DERIVED: the gain IS the posted interest (always additive), so we
                    //    label it "Interest" and never show a percentage (interest % is noisy).
                    //  - MARKET:  show gain ₹ and its percent, guarding the NaN case
                    //    (invested == 0) by hiding the percent.
                    when (row.investment.valuationMode) {
                        ValuationMode.DERIVED -> {
                            Text(
                                text = stringResource(
                                    R.string.investments_subline_interest,
                                    IndianNumberFormat.format(row.investedAmount),
                                    IndianNumberFormat.format(row.absoluteGain),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        ValuationMode.MARKET -> {
                            val pct = row.percentGain
                            val text = if (pct.isNaN()) {
                                stringResource(
                                    R.string.investments_subline_gain_no_pct,
                                    IndianNumberFormat.format(row.investedAmount),
                                    IndianNumberFormat.format(row.absoluteGain),
                                )
                            } else {
                                stringResource(
                                    R.string.investments_subline_gain,
                                    IndianNumberFormat.format(row.investedAmount),
                                    IndianNumberFormat.format(row.absoluteGain),
                                    "%+.1f%%".format(pct),
                                )
                            }
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = gainColor,
                            )
                        }
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.account_detail_action_edit)) },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.account_detail_action_archive)) },
                            onClick = {
                                menuOpen = false
                                onArchive()
                            },
                            leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.account_action_delete),
                                    color = com.subramanya.artha.ui.theme.Danger,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = com.subramanya.artha.ui.theme.Danger,
                                )
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun InvestmentAvatar(color: Long) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(color)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Savings,
            contentDescription = null,
            tint = Color.White,
        )
    }
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
