package com.subramanya.artha.ui.reports

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.subramanya.artha.ui.common.BlockPrintOverlay
import com.subramanya.artha.ui.theme.AccEmerald
import com.subramanya.artha.ui.theme.AccIndigo
import com.subramanya.artha.ui.theme.AccMagenta
import com.subramanya.artha.ui.theme.AccSaffron
import com.subramanya.artha.ui.theme.AccTeal
import com.subramanya.artha.ui.theme.AccViolet
import com.subramanya.artha.ui.theme.Expense
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.IncomeSoft
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.LineTeal
import com.subramanya.artha.ui.theme.Ochre
import com.subramanya.artha.ui.theme.OchreSoft
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface3
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.IndianNumberFormat

/**
 * HANDOFF §3.7 Reports — editorial header, Net Worth hero with block-print
 * pattern, four sub-section composables: CategoryBars, AppBars (stacked +
 * legend), TopMerchants, TaxSections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: ReportsViewModel = viewModel(
        factory = ReportsViewModelFactory(
            transactionRepository = app.transactionRepository,
            accountRepository = app.accountRepository,
            cardRepository = app.cardRepository,
            investmentRepository = app.investmentRepository,
            categoryRepository = app.categoryRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Surface(color = Surface1, modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Surface1,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                com.subramanya.artha.ui.common.InlineTopBar(
                    title = stringResource(R.string.reports_title),
                    onBack = onBack,
                )
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(8.dp))
                    RangePicker(state.range, vm::onRangeChanged)
                    Spacer(Modifier.height(16.dp))
                    NetWorthHero(value = state.netWorth)
                    Spacer(Modifier.height(16.dp))
                    InOutNetStrip(income = state.totalIncome, expense = state.totalExpense)

                    Spacer(Modifier.height(24.dp))
                    CategoryBarsSection(slices = state.spendingByCategory)

                    Spacer(Modifier.height(24.dp))
                    AppBarsSection(slices = state.spendingByPaymentApp)

                    Spacer(Modifier.height(24.dp))
                    TopMerchantsSection(merchants = state.topMerchants)

                    Spacer(Modifier.height(24.dp))
                    TaxSectionsBlock(rows = state.taxSections)

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun RangePicker(current: ReportRange, onChange: (ReportRange) -> Unit) {
    val items = listOf(
        ReportRange.THIS_MONTH to R.string.reports_range_this_month,
        ReportRange.LAST_MONTH to R.string.reports_range_last_month,
        ReportRange.FISCAL_YEAR to R.string.reports_range_fy,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (range, labelRes) ->
            val active = current == range
            Surface(
                color = if (active) com.subramanya.artha.ui.theme.Teal900 else Surface2,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .clickable { onChange(range) }
                    .border(
                        1.dp,
                        if (active) com.subramanya.artha.ui.theme.Teal500 else Line1,
                        RoundedCornerShape(999.dp),
                    ),
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = if (active) Teal300 else Text2,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** Net worth hero — block-print pattern at 5% teal per HANDOFF §4. */
@Composable
private fun NetWorthHero(value: Double) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface2)
            .border(1.dp, LineTeal, RoundedCornerShape(20.dp)),
    ) {
        BlockPrintOverlay(
            tint = com.subramanya.artha.ui.theme.Teal500,
            alpha = 0.05f,
        )
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
            Text(
                text = stringResource(R.string.reports_net_worth).uppercase(),
                style = EyebrowStyle,
                color = Text3,
            )
            Spacer(Modifier.height(8.dp))
            com.subramanya.artha.ui.common.AutoShrinkAmountText(
                text = IndianNumberFormat.format(value),
                color = Text1,
                style = TextStyle(
                    fontFamily = InstrumentSerif,
                    fontSize = 40.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.02).sp,
                    color = Text1,
                    fontFeatureSettings = "tnum, lnum",
                ),
            )
        }
    }
}

/** In · Out · Net 3-column strip — same pattern as the Ledger totals strip. */
@Composable
private fun InOutNetStrip(income: Double, expense: Double) {
    val net = income - expense
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line1, RoundedCornerShape(16.dp)),
    ) {
        Row(modifier = Modifier.padding(vertical = 14.dp)) {
            StripCol(stringResource(R.string.reports_in), income, Income, weight = 1f)
            StripDivider()
            StripCol(stringResource(R.string.reports_out), -expense, Expense, weight = 1f)
            StripDivider()
            StripCol(stringResource(R.string.reports_net), net, if (net >= 0) Income else Expense, weight = 1.1f)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StripCol(label: String, value: Double, color: Color, weight: Float) {
    Column(
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color),
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = label.uppercase(),
                style = EyebrowStyle,
                color = Text3,
            )
        }
        Spacer(Modifier.height(4.dp))
        // 1/3 of a phone width can't fit a 7-figure ₹ at 20sp serif → wraps to
        // a second row. Drop the size and force single-line with auto-shrink.
        androidx.compose.material3.Text(
            text = IndianNumberFormat.format(value),
            color = Text1,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
            style = TextStyle(
                fontFamily = InstrumentSerif,
                fontSize = 17.sp,
                lineHeight = 20.sp,
                color = Text1,
                fontFeatureSettings = "tnum, lnum",
            ),
        )
    }
}

@Composable
private fun StripDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(44.dp)
            .background(Line1),
    )
}

// ─────────────────────────── Sub-sections ─────────────────────────────────────

private val accentPalette: List<Color> = listOf(
    AccTeal,
    AccIndigo,
    AccEmerald,
    AccSaffron,
    AccMagenta,
    AccViolet,
    Ochre,
    OchreSoft,
)

@Composable
private fun SectionHeading(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(1.dp)
                .background(com.subramanya.artha.ui.theme.Teal500),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = title.uppercase(),
            style = EyebrowStyle,
            color = Text3,
        )
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun EmptyHint(text: String) {
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line1, RoundedCornerShape(12.dp)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Text3,
            modifier = Modifier.padding(14.dp),
        )
    }
}

/** Horizontal bars per category, accent-coloured by index. */
@Composable
private fun CategoryBarsSection(slices: List<CategorySlice>) {
    SectionHeading(stringResource(R.string.reports_section_categorybars))
    if (slices.isEmpty()) {
        EmptyHint(stringResource(R.string.reports_empty_period))
        return
    }
    val max = slices.first().total.takeIf { it > 0.0 } ?: 1.0
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line1, RoundedCornerShape(16.dp)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            slices.forEachIndexed { i, slice ->
                if (i > 0) Spacer(Modifier.height(10.dp))
                CategoryBarRow(
                    label = slice.displayName,
                    value = slice.total,
                    fraction = (slice.total / max).coerceIn(0.0, 1.0).toFloat(),
                    color = accentPalette[i % accentPalette.size],
                )
            }
        }
    }
}

@Composable
private fun CategoryBarRow(label: String, value: Double, fraction: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Text1)
            Text(
                text = IndianNumberFormat.format(value),
                style = TextStyle(
                    fontFamily = IbmPlexMono,
                    fontSize = 13.sp,
                    color = Text1,
                    fontFeatureSettings = "tnum, lnum",
                ),
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Surface4),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color),
            )
        }
    }
}

/** Stacked bar of per-payment-app shares + dot legend. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppBarsSection(slices: List<CategorySlice>) {
    SectionHeading(stringResource(R.string.reports_section_appbars))
    if (slices.isEmpty()) {
        EmptyHint(stringResource(R.string.reports_empty_period))
        return
    }
    val total = slices.sumOf { it.total }.takeIf { it > 0.0 } ?: 1.0
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line1, RoundedCornerShape(16.dp)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Stacked bar.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Surface4),
            ) {
                slices.forEachIndexed { i, slice ->
                    val frac = (slice.total / total).toFloat()
                    if (frac > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(frac)
                                .fillMaxWidth()
                                .background(accentPalette[i % accentPalette.size]),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                slices.forEachIndexed { i, slice ->
                    LegendChip(
                        color = accentPalette[i % accentPalette.size],
                        label = slice.displayName,
                        value = IndianNumberFormat.format(slice.total),
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
        Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Text2)
        Spacer(Modifier.size(6.dp))
        Text(
            text = value,
            style = TextStyle(
                fontFamily = IbmPlexMono,
                fontSize = 11.sp,
                color = Text3,
                fontFeatureSettings = "tnum, lnum",
            ),
        )
    }
}

/** Ranked merchant list — mono numerals, ×N count chip on the right. */
@Composable
private fun TopMerchantsSection(merchants: List<MerchantRow>) {
    SectionHeading(stringResource(R.string.reports_section_top_merchants))
    if (merchants.isEmpty()) {
        EmptyHint(stringResource(R.string.reports_empty_period))
        return
    }
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line1, RoundedCornerShape(16.dp)),
    ) {
        Column {
            merchants.forEachIndexed { i, m ->
                if (i > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp)
                            .height(1.dp)
                            .background(Line1),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "%02d".format(i + 1),
                        style = TextStyle(
                            fontFamily = IbmPlexMono,
                            fontSize = 11.sp,
                            color = Text3,
                            fontFeatureSettings = "tnum, lnum",
                        ),
                        modifier = Modifier.width(28.dp),
                    )
                    Text(
                        text = m.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Text1,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.reports_merchant_count_fmt, m.count),
                        style = TextStyle(
                            fontFamily = IbmPlexMono,
                            fontSize = 11.sp,
                            color = Text3,
                            fontFeatureSettings = "tnum, lnum",
                        ),
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = IndianNumberFormat.format(m.total),
                        style = TextStyle(
                            fontFamily = InstrumentSerif,
                            fontSize = 15.sp,
                            color = Text1,
                            fontFeatureSettings = "tnum, lnum",
                        ),
                    )
                }
            }
        }
    }
}

/** 80C / 80D / 80CCD usage cards with progress + headroom. */
@Composable
private fun TaxSectionsBlock(rows: List<TaxSectionRow>) {
    SectionHeading(stringResource(R.string.reports_section_tax))
    if (rows.isEmpty()) {
        EmptyHint(stringResource(R.string.reports_tax_empty))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { TaxSectionCard(it) }
    }
}

@Composable
private fun TaxSectionCard(row: TaxSectionRow) {
    val limit = row.limit
    val fraction = if (limit == null || limit == 0.0) 0f else (row.used / limit).toFloat().coerceIn(0f, 1f)
    val isOver = limit != null && row.used >= limit
    val barColor = if (isOver) IncomeSoft else Teal300
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line1, RoundedCornerShape(14.dp)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = row.section.uppercase(),
                    style = EyebrowStyle,
                    color = Teal300,
                )
                Text(
                    text = IndianNumberFormat.format(row.used) +
                        (limit?.let { " / " + IndianNumberFormat.format(it) } ?: ""),
                    style = TextStyle(
                        fontFamily = IbmPlexMono,
                        fontSize = 12.sp,
                        color = Text1,
                        fontFeatureSettings = "tnum, lnum",
                    ),
                )
            }
            if (limit != null) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Surface4),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(barColor),
                    )
                }
                val remaining = (limit - row.used).coerceAtLeast(0.0)
                if (remaining > 0.0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(
                            R.string.reports_tax_headroom_fmt,
                            IndianNumberFormat.format(remaining),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Text3,
                    )
                }
            }
        }
    }
}

@Suppress("unused")
private val keepSurface3Reference = Surface3
