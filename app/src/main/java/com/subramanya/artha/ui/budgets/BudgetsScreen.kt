package com.subramanya.artha.ui.budgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.BudgetPeriod
import com.subramanya.artha.data.entity.enums.BudgetScope
import com.subramanya.artha.domain.model.Budget
import com.subramanya.artha.domain.model.BudgetWithProgress
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.Expense
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.LineTeal
import com.subramanya.artha.ui.theme.Ochre
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.IndianNumberFormat
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val items by app.budgetRepository.observeActiveWithProgress()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    var formMode: FormMode? by remember { mutableStateOf(null) }
    var pendingDelete: Budget? by remember { mutableStateOf(null) }

    Surface(color = com.subramanya.artha.ui.theme.Surface1, modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = com.subramanya.artha.ui.theme.Surface1,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            floatingActionButton = {
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = { formMode = FormMode.Add },
                    containerColor = com.subramanya.artha.ui.theme.Teal700,
                    contentColor = com.subramanya.artha.ui.theme.Text1,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.budgets_fab_add)) },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                com.subramanya.artha.ui.common.InlineTopBar(
                    title = stringResource(R.string.budgets_title),
                    onBack = onBack,
                )
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.AccountBalanceWallet,
                            title = stringResource(R.string.budgets_empty),
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(items, key = { it.budget.id }) { row ->
                            BudgetRow(
                                row = row,
                                onTap = { formMode = FormMode.Edit(row.budget) },
                                onDelete = { pendingDelete = row.budget },
                            )
                        }
                    }
                }
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        BudgetFormSheet(
            editing = (mode as? FormMode.Edit)?.budget,
            onSave = { resolved ->
                scope.launch {
                    app.budgetRepository.upsert(resolved)
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
            title = stringResource(R.string.budgets_delete_confirm_title),
            text = stringResource(R.string.budgets_delete_confirm_body),
            confirmLabel = stringResource(R.string.budgets_delete_confirm_yes),
            confirmDestructive = true,
            onConfirm = {
                scope.launch {
                    app.budgetRepository.delete(toDelete)
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
    data class Edit(val budget: Budget) : FormMode
}

/**
 * HANDOFF §3.7 — Budget row: Surface2 card with hairline border, header row
 * (36dp Surface4 icon + name + scope/period meta + quiet delete), then the
 * stripe-overflow progress bar. When `spent > cap` the bar fills 100% and
 * an additional diagonally-striped extension shows the overage amount.
 */
@Composable
private fun BudgetRow(row: BudgetWithProgress, onTap: () -> Unit, onDelete: () -> Unit) {
    val cap = row.budget.amount
    val spent = row.spent
    val ratio = if (cap <= 0.0) 0f else (spent / cap).toFloat()
    val overspent = ratio > 1f
    val warnAt = (row.budget.alertThresholdPercent / 100f).coerceIn(0f, 1f)
    val barColor = when {
        overspent -> com.subramanya.artha.ui.theme.Expense
        ratio >= warnAt -> com.subramanya.artha.ui.theme.Ochre
        else -> com.subramanya.artha.ui.theme.Income
    }
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .border(
                width = 1.dp,
                color = if (overspent) LineTeal else Line1,
                shape = RoundedCornerShape(16.dp),
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
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Teal300,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.budget.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = Text1,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = row.budget.scope.label() + " · " + row.budget.period.label() +
                            " · " + stringResource(R.string.budgets_days_left, row.daysRemainingInPeriod),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFeatureSettings = "tnum, lnum",
                        ),
                        color = Text3,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Text3,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            StripeOverflowBar(
                fraction = ratio,
                baseColor = barColor,
                overflowColor = Expense,
            )
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = IndianNumberFormat.format(spent) + " / " + IndianNumberFormat.format(cap),
                    style = TextStyle(
                        fontFamily = IbmPlexMono,
                        fontSize = 12.sp,
                        color = Text1,
                        fontFeatureSettings = "tnum, lnum",
                    ),
                )
                Text(
                    text = (ratio * 100).toInt().toString() + "%",
                    style = TextStyle(
                        fontFamily = IbmPlexMono,
                        fontSize = 12.sp,
                        color = barColor,
                        fontFeatureSettings = "tnum, lnum",
                    ),
                )
            }
        }
    }
}

/**
 * HANDOFF §3.7 Budgets — progress bar with diagonal stripe overflow.
 * - fraction <= 1: a single filled bar of [baseColor] up to `fraction`
 * - fraction > 1: bar fills 100% in [baseColor], plus a diagonally striped
 *   extension of width `(fraction - 1)` clamped to <= 1 in [overflowColor]
 *   that visually overflows the cap.
 *
 * The bar is implemented as a Canvas so we can paint the diagonal stripes
 * explicitly without resorting to a tiled BrushPaint hack.
 */
@Composable
private fun StripeOverflowBar(fraction: Float, baseColor: Color, overflowColor: Color) {
    val barHeightDp = 10.dp
    val cornerDp = 999.dp
    val capped = fraction.coerceAtLeast(0f)
    val baseFraction = capped.coerceAtMost(1f)
    val overflowFraction = (capped - 1f).coerceAtLeast(0f).coerceAtMost(1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeightDp)
            .clip(RoundedCornerShape(cornerDp))
            .background(Surface4),
    ) {
        // Filled base segment.
        if (baseFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(baseFraction)
                    .background(baseColor),
            )
        }
        // Striped overflow segment. We compute its width inside the *remaining*
        // space after the base so the overall bar still represents 100% of cap+overflow.
        if (overflowFraction > 0f) {
            val remainingFraction = (1f - baseFraction).coerceAtLeast(0.01f)
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(overflowFraction / remainingFraction),
            ) {
                val w = size.width
                val h = size.height
                drawRect(color = overflowColor.copy(alpha = 0.35f))
                val stripeSpacing = 8.dp.toPx()
                val stripeWidth = 2.dp.toPx()
                var x = -h
                while (x < w + h) {
                    drawLine(
                        color = overflowColor,
                        start = Offset(x, 0f),
                        end = Offset(x + h, h),
                        strokeWidth = stripeWidth,
                    )
                    x += stripeSpacing
                }
            }
        }
    }
}

@Composable
private fun BudgetScope.label(): String = when (this) {
    BudgetScope.OVERALL -> stringResource(R.string.budget_scope_overall)
    BudgetScope.CATEGORY -> stringResource(R.string.budget_scope_category)
}

@Composable
private fun BudgetPeriod.label(): String = when (this) {
    BudgetPeriod.WEEKLY -> stringResource(R.string.budget_period_weekly)
    BudgetPeriod.MONTHLY -> stringResource(R.string.budget_period_monthly)
    BudgetPeriod.YEARLY -> stringResource(R.string.budget_period_yearly)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetFormSheet(editing: Budget?, onSave: (Budget) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val categories by app.categoryRepository.observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var scope by remember(editing) { mutableStateOf(editing?.scope ?: BudgetScope.OVERALL) }
    var period by remember(editing) { mutableStateOf(editing?.period ?: BudgetPeriod.MONTHLY) }
    var amountText by remember(editing) { mutableStateOf(editing?.amount?.toPlainString() ?: "") }
    var categoryId by remember(editing) { mutableStateOf(editing?.categoryId.orEmpty()) }
    var threshold by remember(editing) { mutableStateOf((editing?.alertThresholdPercent ?: 80).toString()) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val selectedCategoryName = remember(categoryId, categories) {
        categories.firstOrNull { it.id == categoryId }?.name
    }

    val scopeOptions = listOf(
        com.subramanya.artha.ui.common.PillOption(BudgetScope.OVERALL, stringResource(R.string.budget_scope_overall)),
        com.subramanya.artha.ui.common.PillOption(BudgetScope.CATEGORY, stringResource(R.string.budget_scope_category)),
    )
    val periodOptions = listOf(
        com.subramanya.artha.ui.common.PillOption(BudgetPeriod.WEEKLY, stringResource(R.string.budget_period_weekly)),
        com.subramanya.artha.ui.common.PillOption(BudgetPeriod.MONTHLY, stringResource(R.string.budget_period_monthly)),
        com.subramanya.artha.ui.common.PillOption(BudgetPeriod.YEARLY, stringResource(R.string.budget_period_yearly)),
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
                    if (editing == null) R.string.budgets_form_add_title else R.string.budgets_form_edit_title,
                ),
            )

            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.budgets_form_name_label)) {
                com.subramanya.artha.ui.common.ArthaTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "May groceries",
                )
            }
            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.budgets_form_scope_label)) {
                com.subramanya.artha.ui.common.PillRadio(
                    value = scope,
                    options = scopeOptions,
                    onChange = { scope = it },
                )
            }
            if (scope == BudgetScope.CATEGORY) {
                com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.budgets_form_category_id_label)) {
                    // Real category picker — was a raw text field asking for
                    // the internal ID. Now opens CategoryPickerSheet so the
                    // user picks by name and we store the resolved id.
                    com.subramanya.artha.ui.common.SheetChip(
                        label = selectedCategoryName
                            ?: stringResource(R.string.budgets_form_category_pick),
                        leading = androidx.compose.material.icons.Icons.Filled.Category,
                        onClick = { showCategoryPicker = true },
                    )
                }
            }
            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.budgets_form_period_label)) {
                com.subramanya.artha.ui.common.PillRadio(
                    value = period,
                    options = periodOptions,
                    onChange = { period = it },
                )
            }
            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.budgets_form_amount_label)) {
                com.subramanya.artha.ui.common.ArthaTextField(
                    value = amountText,
                    onValueChange = { v ->
                        amountText = v.filterIndexed { i, c -> c.isDigit() || (c == '.' && v.indexOf('.') == i) }
                    },
                    placeholder = "8000",
                    suffix = "₹",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.budgets_form_threshold_label)) {
                com.subramanya.artha.ui.common.ArthaTextField(
                    value = threshold,
                    onValueChange = { v -> threshold = v.filter { it.isDigit() }.take(3) },
                    placeholder = "80",
                    suffix = "%",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            Spacer(Modifier.height(28.dp))
            com.subramanya.artha.ui.common.SavePrimaryButton(
                label = stringResource(R.string.common_save),
                // Require a name AND an amount — used to fall through to
                // "Budget" as the default name on blank input.
                enabled = name.isNotBlank() && amountText.toDoubleOrNull() != null,
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@SavePrimaryButton
                    val t = threshold.toIntOrNull()?.coerceIn(1, 100) ?: 80
                    val now = System.currentTimeMillis()
                    onSave(
                        Budget(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            scope = scope,
                            categoryId = categoryId.takeIf { it.isNotBlank() && scope == BudgetScope.CATEGORY },
                            amount = amount,
                            period = period,
                            startDate = editing?.startDate ?: now,
                            alertThresholdPercent = t,
                            isActive = editing?.isActive ?: true,
                            createdAt = editing?.createdAt ?: now,
                        ),
                    )
                },
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    // Category picker — only relevant when scope = CATEGORY. Filters to
    // EXPENSE categories since budgets cap spending, not income.
    if (showCategoryPicker) {
        com.subramanya.artha.ui.transaction.CategoryPickerSheet(
            categories = categories.filter {
                it.type == com.subramanya.artha.data.entity.enums.CategoryType.EXPENSE
            },
            type = com.subramanya.artha.data.entity.enums.CategoryType.EXPENSE,
            onSelected = {
                categoryId = it.id
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
        )
    }
}

private fun Double.toPlainString(): String = if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()
