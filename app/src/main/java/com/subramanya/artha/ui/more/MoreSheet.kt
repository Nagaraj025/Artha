package com.subramanya.artha.ui.more

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subramanya.artha.R
import com.subramanya.artha.ui.common.ArthaSheetHandle
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface3
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text3

enum class MoreAction {
    Categories,
    Tags,
    Settings,
    About,
    Investments,
    Insurance,
    Rules,
    People,
    Budgets,
    Goals,
    Subscriptions,
    Recurring,
    Reports,
}

private data class MoreRow(val action: MoreAction, val icon: ImageVector, @StringRes val titleRes: Int, @StringRes val subRes: Int)

private data class MoreSection(@StringRes val titleRes: Int, val rows: List<MoreRow>)

/**
 * HANDOFF §3.5 — More sheet rendered as 5 grouped sections (Money,
 * Recurring, People & rules, Look-ups, App), each a card-flush container
 * with Surface4-tiled icons, teal-300 glyph, label + sub, trailing chevron.
 */
private val MoreSections: List<MoreSection> = listOf(
    MoreSection(
        titleRes = R.string.more_section_money,
        rows = listOf(
            MoreRow(MoreAction.Investments, Icons.AutoMirrored.Filled.TrendingUp, R.string.more_investments, R.string.more_sub_investments),
            MoreRow(MoreAction.Insurance, Icons.Filled.Shield, R.string.more_insurance, R.string.more_sub_insurance),
            MoreRow(MoreAction.Budgets, Icons.Filled.AccountBalanceWallet, R.string.more_budgets, R.string.more_sub_budgets),
            MoreRow(MoreAction.Goals, Icons.Filled.Flag, R.string.more_goals, R.string.more_sub_goals),
            MoreRow(MoreAction.Reports, Icons.Filled.BarChart, R.string.more_reports, R.string.more_sub_reports),
        ),
    ),
    MoreSection(
        titleRes = R.string.more_section_recurring,
        rows = listOf(
            MoreRow(MoreAction.Subscriptions, Icons.Filled.Subscriptions, R.string.more_subscriptions, R.string.more_sub_subscriptions),
            MoreRow(MoreAction.Recurring, Icons.Filled.EventRepeat, R.string.more_recurring, R.string.more_sub_recurring),
        ),
    ),
    MoreSection(
        titleRes = R.string.more_section_rules_people,
        rows = listOf(
            MoreRow(MoreAction.Rules, Icons.AutoMirrored.Filled.Rule, R.string.more_rules, R.string.more_sub_rules),
            MoreRow(MoreAction.People, Icons.Filled.Group, R.string.more_people, R.string.more_sub_people),
        ),
    ),
    MoreSection(
        titleRes = R.string.more_section_lookups,
        rows = listOf(
            MoreRow(MoreAction.Categories, Icons.Filled.Category, R.string.more_categories, R.string.more_sub_categories),
            MoreRow(MoreAction.Tags, Icons.Filled.Sell, R.string.more_tags, R.string.more_sub_tags),
        ),
    ),
    MoreSection(
        titleRes = R.string.more_section_app,
        rows = listOf(
            MoreRow(MoreAction.Settings, Icons.Filled.Settings, R.string.more_settings, R.string.more_sub_settings),
            MoreRow(MoreAction.About, Icons.Filled.Info, R.string.more_about, R.string.more_sub_about),
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreSheet(onDismiss: () -> Unit, onActionSelected: (MoreAction) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface3,
        contentWindowInsets = com.subramanya.artha.ui.common.SheetWindowInsets,
        dragHandle = { ArthaSheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            // Editorial header per §3.5: eyebrow + Instrument Serif title.
            Text(
                text = stringResource(R.string.more_eyebrow).uppercase(),
                style = EyebrowStyle,
                color = Teal300,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.more_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal),
                color = Text1,
            )
            Spacer(Modifier.height(20.dp))

            MoreSections.forEachIndexed { idx, section ->
                if (idx > 0) Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(section.titleRes).uppercase(),
                    style = EyebrowStyle,
                    color = Text3,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                MoreSectionCard(rows = section.rows, onClick = onActionSelected)
            }
        }
    }
}

@Composable
private fun MoreSectionCard(rows: List<MoreRow>, onClick: (MoreAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .border(1.dp, Line1, RoundedCornerShape(16.dp)),
    ) {
        rows.forEachIndexed { i, row ->
            if (i > 0) {
                // 1px hairline divider, indented past the icon so it lines up with the label edge.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 64.dp)
                        .height(1.dp)
                        .background(Line1),
                )
            }
            MoreActionRow(row = row, onClick = { onClick(row.action) })
        }
    }
}

@Composable
private fun MoreActionRow(row: MoreRow, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Surface4),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = row.icon,
                contentDescription = null,
                tint = Teal300,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(row.titleRes),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = Text1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(row.subRes),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = Text3,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Text3,
            modifier = Modifier.size(18.dp),
        )
    }
}
