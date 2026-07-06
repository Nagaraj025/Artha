package com.subramanya.artha.ui.insurance

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.InsuranceType
import com.subramanya.artha.domain.model.Insurance
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.Expense
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.LineTeal
import com.subramanya.artha.ui.theme.Ochre
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsurancesScreen(onBack: () -> Unit, onOpenInsurance: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: InsurancesViewModel = viewModel(
        factory = InsurancesViewModelFactory(app.insuranceRepository),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    var formMode: FormMode? by remember { mutableStateOf(null) }
    var pendingDelete: Insurance? by remember { mutableStateOf(null) }

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
                    text = { Text(stringResource(R.string.insurance_fab_add)) },
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
                        title = stringResource(R.string.insurance_title),
                        onBack = onBack,
                    )
                }
                if (state.activeCount > 0) {
                    item {
                        HeroCard(
                            annualTotal = state.annualPremiumTotal,
                            count = state.activeCount,
                            dueSoonCount = state.dueWithin30Days.size,
                        )
                    }
                }
                if (state.dueWithin30Days.isNotEmpty()) {
                    item { DueSoonBanner(state.dueWithin30Days) }
                }
                if (state.grouped.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(
                                icon = Icons.Filled.Shield,
                                title = stringResource(R.string.insurance_empty),
                            )
                        }
                    }
                } else {
                    state.grouped.forEach { (type, rows) ->
                        item(key = "header-${type.name}") { TypeHeader(type) }
                        items(rows, key = { it.id }) { policy ->
                            InsuranceRow(
                                insurance = policy,
                                onTap = { onOpenInsurance(policy.id) },
                                onEdit = { formMode = FormMode.Edit(policy) },
                                onArchive = { vm.archive(policy) },
                                onDelete = { pendingDelete = policy },
                            )
                        }
                    }
                }
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        InsuranceFormSheet(
            editing = (mode as? FormMode.Edit)?.insurance,
            onDismiss = { formMode = null },
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = stringResource(R.string.insurance_delete_confirm_title),
            text = stringResource(R.string.insurance_delete_confirm_body),
            confirmLabel = stringResource(R.string.insurance_delete_confirm_yes),
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
    data class Edit(val insurance: Insurance) : FormMode
}

// ---------------- hero ----------------

@Composable
private fun HeroCard(annualTotal: Double, count: Int, dueSoonCount: Int) {
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LineTeal, RoundedCornerShape(18.dp)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                text = stringResource(R.string.insurance_hero_annual_premium).uppercase(),
                style = EyebrowStyle,
                color = Text3,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = IndianNumberFormat.format(annualTotal),
                style = TextStyle(
                    fontFamily = InstrumentSerif,
                    fontSize = 40.sp,
                    lineHeight = 46.sp,
                    color = Text1,
                    fontFeatureSettings = "tnum, lnum",
                ),
            )
            Spacer(Modifier.height(6.dp))
            val subtitle = stringResource(R.string.insurance_hero_subtitle, count)
            val withDue = if (dueSoonCount > 0) {
                subtitle + " · " + stringResource(R.string.insurance_hero_due_soon, dueSoonCount)
            } else {
                subtitle
            }
            Text(
                text = withDue,
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

/** Ochre due-soon banner (premium reminder, not an error). */
@Composable
private fun DueSoonBanner(items: List<Insurance>) {
    val nearest = items.first()
    Surface(
        color = Ochre.copy(alpha = 0.10f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Ochre.copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = Ochre,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.insurance_due_banner_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Text1,
                )
                val due = nearest.nextPremiumDate?.let { DateFormatter.longDate(it) }.orEmpty()
                Text(
                    text = "${nearest.name} · $due",
                    style = TextStyle(
                        fontFamily = IbmPlexMono,
                        fontSize = 11.sp,
                        color = Text2,
                        fontFeatureSettings = "tnum, lnum",
                    ),
                )
            }
        }
    }
}

@Composable
private fun TypeHeader(type: InsuranceType) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 1.dp)
                .background(Teal300),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = type.displayName().uppercase(),
            style = EyebrowStyle,
            color = Teal300,
        )
    }
}

// ---------------- row ----------------

@Composable
private fun InsuranceRow(insurance: Insurance, onTap: () -> Unit, onEdit: () -> Unit, onArchive: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val due = insurance.nextPremiumDate
    val daysUntil = due?.let {
        val now = Clock.System.now().toEpochMilliseconds()
        ((it - now) / (1000L * 60 * 60 * 24)).toInt()
    }
    val dueSoon = daysUntil != null && daysUntil <= 30
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (dueSoon) Ochre.copy(alpha = 0.40f) else Line1,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onTap),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InsuranceAvatar(color = insurance.color)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insurance.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Text1,
                )
                Spacer(Modifier.height(2.dp))
                val parts = buildList {
                    add(insurance.provider)
                    add(
                        stringResource(
                            R.string.insurance_row_premium_fmt,
                            IndianNumberFormat.format(insurance.premiumAmount),
                        ),
                    )
                    due?.let {
                        val label = when {
                            daysUntil == null -> ""
                            daysUntil < 0 -> stringResource(R.string.insurance_row_overdue)
                            daysUntil == 0 -> stringResource(R.string.insurance_row_due_today)
                            daysUntil == 1 -> stringResource(R.string.insurance_row_due_tomorrow)
                            else -> stringResource(R.string.insurance_row_due_in_days, daysUntil)
                        }
                        if (label.isNotBlank()) add(label)
                    }
                }
                Text(
                    text = parts.joinToString(" · "),
                    style = TextStyle(
                        fontFamily = IbmPlexMono,
                        fontSize = 11.sp,
                        color = when {
                            daysUntil != null && daysUntil < 0 -> Expense
                            dueSoon -> Ochre
                            else -> Text3
                        },
                        fontWeight = if (dueSoon) FontWeight.SemiBold else FontWeight.Normal,
                        fontFeatureSettings = "tnum, lnum",
                    ),
                )
            }
            Spacer(Modifier.size(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.insurance_row_sum_assured_label).uppercase(),
                    style = EyebrowStyle,
                    color = Text3,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = IndianNumberFormat.format(insurance.sumAssured),
                    style = TextStyle(
                        fontFamily = InstrumentSerif,
                        fontSize = 16.sp,
                        color = Text1,
                        fontFeatureSettings = "tnum, lnum",
                    ),
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = null,
                        tint = Text3,
                        modifier = Modifier.size(18.dp),
                    )
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
                                color = Expense,
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
                                tint = Expense,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun InsuranceAvatar(color: Long) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(color)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Shield, contentDescription = null, tint = Text1)
    }
}

@Composable
internal fun InsuranceType.displayName(): String = when (this) {
    InsuranceType.HEALTH -> stringResource(R.string.insurance_type_health)
    InsuranceType.VEHICLE -> stringResource(R.string.insurance_type_vehicle)
    InsuranceType.LIFE_TERM -> stringResource(R.string.insurance_type_life_term)
    InsuranceType.LIFE_ENDOWMENT -> stringResource(R.string.insurance_type_life_endowment)
    InsuranceType.TRAVEL -> stringResource(R.string.insurance_type_travel)
    InsuranceType.HOME -> stringResource(R.string.insurance_type_home)
    InsuranceType.OTHER -> stringResource(R.string.insurance_type_other)
}
