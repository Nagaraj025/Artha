package com.subramanya.artha.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
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
import com.subramanya.artha.ai.AiQuickEntryParsed
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.AccountWithBalance
import com.subramanya.artha.domain.model.CardWithBalance
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.ui.ai.AiQuickEntrySheet
import com.subramanya.artha.ui.common.BandhaniOverlay
import com.subramanya.artha.ui.common.BlockPrintOverlay
import com.subramanya.artha.ui.common.MonoMeta
import com.subramanya.artha.ui.common.RefreshableContent
import com.subramanya.artha.ui.common.SectionEyebrow
import com.subramanya.artha.ui.theme.AccEmerald
import com.subramanya.artha.ui.theme.AccIndigo
import com.subramanya.artha.ui.theme.AccMagenta
import com.subramanya.artha.ui.theme.AccSaffron
import com.subramanya.artha.ui.theme.AccTeal
import com.subramanya.artha.ui.theme.AccViolet
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.ui.theme.Expense
import com.subramanya.artha.ui.theme.ExpenseSoft
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.IncomeSoft
import com.subramanya.artha.ui.theme.LineTeal
import com.subramanya.artha.ui.theme.OchreSoft
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Teal950
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.ui.theme.TiroDevanagariHindi
import com.subramanya.artha.ui.transaction.AddTransactionSheet
import com.subramanya.artha.ui.transaction.AddTransactionViewModel
import com.subramanya.artha.ui.transaction.AddTransactionViewModelFactory
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onOpenTransactions: () -> Unit = {},
    onOpenAccount: (String) -> Unit = {},
    onOpenCard: (String) -> Unit = {},
    onOpenTransaction: (String) -> Unit = {},
    onOpenInsurance: (String) -> Unit = {},
    onAddAccount: () -> Unit = {},
    onAddCard: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication

    val vm: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(
            accountRepository = app.accountRepository,
            cardRepository = app.cardRepository,
            transactionRepository = app.transactionRepository,
            investmentRepository = app.investmentRepository,
            insuranceRepository = app.insuranceRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var showSheet by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }
    var pendingAiPrefill: AiQuickEntryParsed? by remember { mutableStateOf(null) }

    val showMonthly by app.settingsPreferences.dashboardShowMonthly.collectAsStateWithLifecycle(initialValue = true)
    val showAccounts by app.settingsPreferences.dashboardShowAccounts.collectAsStateWithLifecycle(initialValue = true)
    val showCards by app.settingsPreferences.dashboardShowCards.collectAsStateWithLifecycle(initialValue = true)
    val showRecent by app.settingsPreferences.dashboardShowRecent.collectAsStateWithLifecycle(initialValue = true)
    // AI Quick Entry is opt-in — the card stays hidden until enabled in Settings (default off).
    val showAiQuickEntry by app.settingsPreferences.aiQuickEntryEnabled.collectAsStateWithLifecycle(initialValue = false)

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        RefreshableContent(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 120.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                NetPositionHero(state)

                if (state.premiumsDueThisWeek.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    PremiumDueBanner(
                        policies = state.premiumsDueThisWeek,
                        onTap = { onOpenInsurance(state.premiumsDueThisWeek.first().id) },
                    )
                }

                if (showMonthly) {
                    Spacer(Modifier.height(14.dp))
                    FlowStrip(state)
                }

                if (showAiQuickEntry) {
                    Spacer(Modifier.height(14.dp))
                    AiEntryCard(onOpen = { showAiSheet = true })
                }

                if (showAccounts) {
                    Spacer(Modifier.height(20.dp))
                    AccountsRow(
                        accounts = state.accounts,
                        onOpenAccount = onOpenAccount,
                        onAddAccount = onAddAccount,
                    )
                }

                if (showCards) {
                    Spacer(Modifier.height(18.dp))
                    CardsRow(
                        cards = state.cards,
                        onOpenCard = onOpenCard,
                        onAddCard = onAddCard,
                    )
                }

                if (showRecent) {
                    Spacer(Modifier.height(20.dp))
                    RecentSection(
                        transactions = state.recentTransactions,
                        onViewAll = onOpenTransactions,
                        onOpenTransaction = onOpenTransaction,
                    )
                }
            }

            FabRow(
                onTap = { showSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp),
            )
        }
    }

    if (showSheet) {
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
        pendingAiPrefill?.let { parsed ->
            androidx.compose.runtime.LaunchedEffect(parsed) {
                txnVm.applyAiPrefill(parsed)
                pendingAiPrefill = null
            }
        }
        AddTransactionSheet(viewModel = txnVm, onDismiss = { showSheet = false })
    }

    if (showAiSheet) {
        AiQuickEntrySheet(
            onDismiss = { showAiSheet = false },
            onConfirmed = { parsed ->
                pendingAiPrefill = parsed
                showAiSheet = false
                showSheet = true
            },
        )
    }
}

// ───────────────────────────── Net Position Hero ─────────────────────────────

@Composable
private fun NetPositionHero(state: DashboardUiState) {
    val liquid = state.accounts.sumOf { it.currentBalance }
    val cardOs = state.cards.sumOf { it.currentOutstanding }
    val invested = state.investmentTotalValue

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface2)
            .border(width = 1.dp, color = LineTeal, shape = RoundedCornerShape(20.dp)),
    ) {
        // Jaali / block-print overlay
        BlockPrintOverlay(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(20.dp)),
            tint = Teal300,
            alpha = 0.05f,
        )

        // Corner अ glyph — subordinated to the amount. Shrunk from 32dp/18sp to
        // 24dp/13sp and tucked further into the corner so a long net-position
        // amount ("₹1,00,00,000") doesn't visually collide with it.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 14.dp)
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(width = 1.dp, color = LineTeal, shape = RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "अ",
                color = Teal300,
                fontFamily = TiroDevanagariHindi,
                fontSize = 13.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Normal,
            )
        }

        // end padding accounts for the corner mark (24dp + 14dp + 12dp breathing).
        Column(modifier = Modifier.padding(start = 22.dp, end = 52.dp, top = 20.dp, bottom = 18.dp)) {
            Text(
                text = stringResource(R.string.dashboard_net_position).uppercase(),
                style = EyebrowStyle,
                color = Text3,
            )
            Spacer(Modifier.height(6.dp))
            com.subramanya.artha.ui.common.AutoShrinkAmountText(
                text = IndianNumberFormat.format(state.netPosition),
                style = ArthaAmountStyles.hero,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Change row: ↑ ₹delta · +x.x% · this month
            Spacer(Modifier.height(10.dp))
            val change = state.netChangeThisMonth
            val pct = if (state.netPosition - change != 0.0) {
                change / (state.netPosition - change) * 100.0
            } else {
                0.0
            }
            val positive = change >= 0
            val changeTint = if (positive) Income else Expense
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (positive) {
                        Icons.AutoMirrored.Filled.TrendingUp
                    } else {
                        Icons.AutoMirrored.Filled.TrendingDown
                    },
                    contentDescription = null,
                    tint = changeTint,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = IndianNumberFormat.format(kotlin.math.abs(change)),
                    color = changeTint,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = IbmPlexMono,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum",
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Text("•", color = Text3, fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = (if (pct >= 0) "+" else "") + "%.1f%%".format(pct),
                    color = changeTint,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = IbmPlexMono,
                        fontSize = 13.sp,
                        fontFeatureSettings = "tnum",
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Text("this month", color = Text3, style = MaterialTheme.typography.bodySmall)
            }

            // Sparkline — 30 days of end-of-day net position. Hidden if no history.
            if (state.netPositionSpark.size >= 2) {
                Spacer(Modifier.height(14.dp))
                com.subramanya.artha.ui.common.Sparkline(
                    points = state.netPositionSpark,
                    color = com.subramanya.artha.ui.theme.Teal500,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BreakdownCell(label = "Liquid", value = liquid, color = Teal300, modifier = Modifier.weight(1f))
                BreakdownCell(label = "Invested", value = invested, color = OchreSoft, modifier = Modifier.weight(1f))
                BreakdownCell(label = "Card o/s", value = -cardOs, color = Expense, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BreakdownCell(label: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label.uppercase(), style = EyebrowStyle, color = Text3)
        Spacer(Modifier.height(4.dp))
        Text(
            text = IndianNumberFormat.formatCompact(value),
            color = color,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 18.sp,
        )
    }
}

// ───────────────────────────── Premium-Due Banner ────────────────────────────

@Composable
private fun PremiumDueBanner(policies: List<com.subramanya.artha.domain.model.Insurance>, onTap: () -> Unit) {
    val nearest = policies.first()
    val dueText = nearest.nextPremiumDate?.let { DateFormatter.longDate(it) }.orEmpty()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onTap),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (policies.size == 1) {
                        stringResource(R.string.dashboard_premium_due_one)
                    } else {
                        stringResource(R.string.dashboard_premium_due_many, policies.size)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = "${nearest.name} · $dueText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

// ───────────────────────────── Flow Strip (Income / Spending) ────────────────

@Composable
private fun FlowStrip(state: DashboardUiState) {
    // Current month abbreviation (JAN..DEC) — was previously hardcoded to "MAY".
    val monthLabel = remember {
        kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
            .month.name.take(3)
    }
    Row(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FlowTile(
            modifier = Modifier.weight(1f),
            inDirection = true,
            label = stringResource(R.string.dashboard_this_month_income),
            value = state.monthlyTotals.income,
            footer = monthLabel,
        )
        FlowTile(
            modifier = Modifier.weight(1f),
            inDirection = false,
            label = stringResource(R.string.dashboard_this_month_expense),
            value = state.monthlyTotals.expense,
            footer = monthLabel,
        )
    }
}

@Composable
private fun FlowTile(modifier: Modifier, inDirection: Boolean, label: String, value: Double, footer: String) {
    val tint = if (inDirection) Income else Expense
    val softTint = if (inDirection) IncomeSoft else ExpenseSoft
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface2)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(softTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (inDirection) {
                        Icons.AutoMirrored.Filled.TrendingUp
                    } else {
                        Icons.AutoMirrored.Filled.TrendingDown
                    },
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(13.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(text = label.uppercase(), style = EyebrowStyle, color = Text3)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = IndianNumberFormat.formatCompact(value),
            style = ArthaAmountStyles.display.copy(fontSize = 28.sp, lineHeight = 32.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = footer,
            color = Text3,
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = IbmPlexMono,
                fontSize = 11.sp,
                fontFeatureSettings = "tnum",
            ),
        )
    }
}

// ───────────────────────────── AI Quick Entry Card ───────────────────────────

@Composable
private fun AiEntryCard(onOpen: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Surface2, Teal950),
                ),
            )
            .border(1.dp, LineTeal, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Teal700)
                    .border(1.dp, Teal300.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Quick add with Gemini",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "\"Auto to MG Road ₹180\" — type, dictate, snap a receipt",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Teal300, modifier = Modifier.size(18.dp))
        }
    }
}

// ───────────────────────────── Accounts Row ──────────────────────────────────

private val ACCOUNT_GRADIENTS = listOf(AccTeal, AccIndigo, AccSaffron, AccEmerald, AccMagenta, AccViolet)

@Composable
private fun AccountsRow(accounts: List<AccountWithBalance>, onOpenAccount: (String) -> Unit, onAddAccount: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.dashboard_section_accounts),
            action = stringResource(R.string.dashboard_view_all),
            onAction = onAddAccount,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            accounts.forEachIndexed { index, row ->
                val tone = ACCOUNT_GRADIENTS[index % ACCOUNT_GRADIENTS.size]
                AccountChip(
                    row = row,
                    tone = tone,
                    onClick = { onOpenAccount(row.account.id) },
                )
            }
            AddChip(onClick = onAddAccount, label = "Add account")
        }
    }
}

@Composable
private fun AccountChip(row: AccountWithBalance, tone: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(158.dp)
            .height(116.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(tone, tone.darken(0.45f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        BandhaniOverlay(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp)),
            tint = Color.White,
            alpha = 0.16f,
        )
        Column(
            modifier = Modifier
                .matchParentSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = row.account.type.name.replace('_', ' '),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                style = EyebrowStyle,
            )
            Column {
                Text(
                    text = row.account.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = IndianNumberFormat.format(row.currentBalance),
                        color = Color.White,
                        style = androidx.compose.ui.text.TextStyle(
                            fontWeight = FontWeight.Medium,
                            fontSize = 17.sp,
                            fontFeatureSettings = "tnum",
                        ),
                    )
                    row.account.accountNumberLast4?.let {
                        Text(
                            text = "•$it",
                            color = Color.White.copy(alpha = 0.7f),
                            fontFamily = IbmPlexMono,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun Color.darken(amount: Float): Color {
    val a = amount.coerceIn(0f, 1f)
    return Color(
        red = red * (1f - a),
        green = green * (1f - a),
        blue = blue * (1f - a),
        alpha = alpha,
    )
}

// ───────────────────────────── Cards Row ─────────────────────────────────────

@Composable
private fun CardsRow(cards: List<CardWithBalance>, onOpenCard: (String) -> Unit, onAddCard: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.dashboard_section_cards),
            action = if (cards.isNotEmpty()) stringResource(R.string.dashboard_view_all) else null,
            onAction = onAddCard,
        )
        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface2)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.dashboard_section_cards_empty),
                    color = Text3,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                cards.forEachIndexed { i, c ->
                    val tone = listOf(AccIndigo, AccMagenta, AccSaffron, AccViolet)[i % 4]
                    CardChip(row = c, tone = tone, onClick = { onOpenCard(c.card.id) })
                }
                AddChip(onClick = onAddCard, label = "Add card")
            }
        }
    }
}

@Composable
private fun CardChip(row: CardWithBalance, tone: Color, onClick: () -> Unit) {
    val limit = row.card.creditLimit ?: 0.0
    val pct = if (limit > 0) (row.currentOutstanding / limit).toFloat().coerceIn(0f, 1f) else 0f
    Box(
        modifier = Modifier
            .width(208.dp)
            .height(116.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(tone, tone.darken(0.5f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        BandhaniOverlay(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)), alpha = 0.14f)
        Column(
            modifier = Modifier.matchParentSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CreditCard,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = row.card.network.name,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = EyebrowStyle,
                )
            }
            Column {
                Text(
                    text = row.card.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Outstanding " + IndianNumberFormat.formatCompact(row.currentOutstanding),
                    color = Color.White,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        fontFeatureSettings = "tnum",
                    ),
                )
                Spacer(Modifier.height(6.dp))
                com.subramanya.artha.ui.common.LinearMeter(
                    fraction = pct,
                    fillColor = Color.White,
                    trackColor = Color.White.copy(alpha = 0.18f),
                    heightDp = 3,
                )
            }
        }
    }
}

@Composable
private fun AddChip(onClick: () -> Unit, label: String) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .height(116.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Text3, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(6.dp))
        Text(text = label, color = Text3, fontSize = 12.sp)
    }
}

// ───────────────────────────── Recent activity ───────────────────────────────

@Composable
private fun RecentSection(transactions: List<Transaction>, onViewAll: () -> Unit, onOpenTransaction: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Recent activity",
            action = stringResource(R.string.dashboard_view_all),
            onAction = onViewAll,
        )
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface2)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Inbox, contentDescription = null, tint = Text3)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.dashboard_recent_empty), color = Text3)
                }
            }
            return@Column
        }

        // Group transactions by day label (e.g. "Today", "Yesterday", weekday).
        val grouped: Map<String, List<Transaction>> = transactions.groupBy { dayLabel(it.date) }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            grouped.forEach { (day, list) ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = day.uppercase(),
                    style = EyebrowStyle,
                    color = Text3,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface2),
                ) {
                    list.forEachIndexed { i, txn ->
                        TransactionRow(txn = txn, onClick = { onOpenTransaction(txn.id) })
                        if (i < list.size - 1) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 56.dp)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(txn: Transaction, onClick: () -> Unit) {
    val isIncome = txn.type.isIncomeLike()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (isIncome) IncomeSoft else Surface4),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconForType(txn.type),
                contentDescription = null,
                tint = if (isIncome) Income else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = txn.description.ifBlank { txn.type.name.replace('_', ' ') },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            MonoMeta(text = transactionMeta(txn))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = signedAmount(txn),
                color = if (isIncome) Income else MaterialTheme.colorScheme.onSurface,
                style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(2.dp))
            txn.categoryId?.let { cid ->
                Text(
                    text = cid.removePrefix("cat_").replace('_', ' '),
                    color = Text3,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String?, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 8.dp)
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionEyebrow(label = title, modifier = Modifier.weight(1f))
        if (action != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = action,
                    color = Teal300,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Teal300, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ───────────────────────────── FAB ────────────────────────────────────────────

/**
 * HANDOFF §2 — Extended FAB on Home only: 56dp tall, "Add" label, plus icon,
 * Teal700. Long-press → AI Quick Entry was removed because that flow already
 * has its own dedicated entry point (the AI Entry card on the dashboard);
 * doubling it up on the FAB was firing both sheets on a single press.
 */
@Composable
private fun FabRow(onTap: () -> Unit, modifier: Modifier = Modifier) {
    ExtendedFloatingActionButton(
        onClick = onTap,
        shape = RoundedCornerShape(18.dp),
        containerColor = Teal700,
        contentColor = androidx.compose.ui.graphics.Color(0xFFF0EAD6),
        icon = {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.dashboard_fab_add_a11y),
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dashboard_fab_add),
                style = MaterialTheme.typography.labelLarge,
            )
        },
        modifier = modifier,
    )
}

// ───────────────────────────── helpers ───────────────────────────────────────

private fun TransactionType.isIncomeLike() = this in setOf(
    TransactionType.INCOME,
    TransactionType.REFUND,
    TransactionType.CASHBACK,
    TransactionType.INTEREST,
    TransactionType.LOAN_RECEIVED,
    TransactionType.GIFT_RECEIVED,
)

private fun signedAmount(txn: Transaction): String {
    val abs = IndianNumberFormat.format(txn.amount)
    return when {
        txn.type.isIncomeLike() -> "+$abs"
        txn.type == TransactionType.EXPENSE ||
            txn.type == TransactionType.LOAN_GIVEN ||
            txn.type == TransactionType.GIFT_SENT -> "−$abs"
        else -> abs
    }
}

private fun iconForType(type: TransactionType) = when (type) {
    TransactionType.CARD_PAYMENT -> Icons.Filled.CreditCard
    TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK,
    TransactionType.INTEREST,
    -> Icons.AutoMirrored.Filled.TrendingDown
    TransactionType.LOAN_GIVEN, TransactionType.GIFT_SENT -> Icons.AutoMirrored.Filled.TrendingUp
    else -> Icons.Filled.Add
}

private fun transactionMeta(txn: Transaction): String {
    val pieces = buildList {
        txn.place?.takeIf { it.isNotBlank() }?.let { add(it) }
        add(DateFormatter.shortDate(txn.date))
    }
    return pieces.joinToString(" · ")
}

/** Compact day label per recent-list grouping in the design. */
private fun dayLabel(epochMillis: Long): String {
    val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
    val today = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    val tdy = kotlinx.datetime.Instant.fromEpochMilliseconds(today)
        .toLocalDateTime(tz).date
    val d = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(tz).date
    return when (d) {
        tdy -> "Today"
        tdy.minus(1, kotlinx.datetime.DateTimeUnit.DAY) -> "Yesterday"
        else -> DateFormatter.shortDate(d)
    }
}
