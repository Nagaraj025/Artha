package com.subramanya.artha.ui.search

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.Insurance
import com.subramanya.artha.domain.model.Investment
import com.subramanya.artha.domain.model.Person
import com.subramanya.artha.domain.model.Tag
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.ui.theme.Expense
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.PlusJakartaSans
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.ui.theme.Text4
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat

/**
 * Full-screen global search. Tap the search icon on the Dashboard greeting
 * to land here. As the user types, every matching transaction / account /
 * card / person / category / tag / investment / insurance shows up in
 * grouped sections; tapping a row navigates to the relevant detail screen.
 *
 * Edge-to-edge: outer Scaffold has contentWindowInsets = WindowInsets(0)
 * (set in MainActivity), so this screen owns its own status-bar + nav-bar
 * + IME inset handling.
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    onOpenAccount: (String) -> Unit,
    onOpenCard: (String) -> Unit,
    onOpenInvestment: (String) -> Unit,
    onOpenInsurance: (String) -> Unit,
    onOpenPeople: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenTags: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(
            transactionRepository = app.transactionRepository,
            accountRepository = app.accountRepository,
            cardRepository = app.cardRepository,
            personRepository = app.personRepository,
            categoryRepository = app.categoryRepository,
            tagRepository = app.tagRepository,
            investmentRepository = app.investmentRepository,
            insuranceRepository = app.insuranceRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        color = Surface1,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            SearchInputBar(
                value = state.query,
                onValueChange = vm::onQueryChanged,
                onBack = {
                    keyboard?.hide()
                    onBack()
                },
                onClear = vm::clear,
                focusRequester = focusRequester,
            )

            if (state.query.isBlank()) {
                EmptyHint()
            } else if (state.isEmpty) {
                NoResults(query = state.query)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 12.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    val r = state.results

                    if (r.transactions.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.search_section_transactions), r.transactions.size) }
                        items(r.transactions, key = { "t-${it.id}" }) { txn ->
                            TransactionResultRow(txn = txn, onClick = { onOpenTransaction(txn.id) })
                        }
                    }
                    if (r.accounts.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.search_section_accounts), r.accounts.size) }
                        items(r.accounts, key = { "a-${it.id}" }) { acct ->
                            EntityRow(
                                icon = Icons.Filled.AccountBalance,
                                title = acct.name,
                                subtitle = listOfNotNull(
                                    acct.institution,
                                    acct.accountNumberLast4?.let { "•$it" },
                                ).joinToString(" · "),
                                onClick = { onOpenAccount(acct.id) },
                            )
                        }
                    }
                    if (r.cards.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.search_section_cards), r.cards.size) }
                        items(r.cards, key = { "c-${it.id}" }) { card ->
                            EntityRow(
                                icon = Icons.Filled.CreditCard,
                                title = card.name,
                                subtitle = listOfNotNull(
                                    card.issuer,
                                    card.cardNumberLast4?.let { "•$it" },
                                ).joinToString(" · "),
                                onClick = { onOpenCard(card.id) },
                            )
                        }
                    }
                    if (r.people.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.search_section_people), r.people.size) }
                        items(r.people, key = { "p-${it.id}" }) { person ->
                            EntityRow(
                                icon = Icons.Filled.Person,
                                title = person.name,
                                subtitle = listOfNotNull(
                                    person.relation.name.lowercase().replaceFirstChar { it.titlecase() },
                                    person.contact,
                                ).joinToString(" · "),
                                onClick = { onOpenPeople() },
                            )
                        }
                    }
                    if (r.categories.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.search_section_categories), r.categories.size) }
                        items(r.categories, key = { "cat-${it.id}" }) { cat ->
                            EntityRow(
                                icon = Icons.Filled.Category,
                                title = cat.name,
                                subtitle = cat.type.name.lowercase().replaceFirstChar { it.titlecase() },
                                onClick = { onOpenCategories() },
                            )
                        }
                    }
                    if (r.tags.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.search_section_tags), r.tags.size) }
                        items(r.tags, key = { "tag-${it.id}" }) { tag ->
                            EntityRow(
                                icon = Icons.Filled.Sell,
                                title = tag.name,
                                subtitle = stringResource(R.string.search_tag_subtitle),
                                onClick = { onOpenTags() },
                            )
                        }
                    }
                    if (r.investments.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.search_section_investments), r.investments.size) }
                        items(r.investments, key = { "i-${it.id}" }) { inv ->
                            EntityRow(
                                icon = Icons.Filled.TrendingUp,
                                title = inv.name,
                                subtitle = listOfNotNull(inv.institution, inv.taxSection).joinToString(" · "),
                                onClick = { onOpenInvestment(inv.id) },
                                // Computed per-mode value (DERIVED → contributions + interest),
                                // falling back to raw currentValue if not in the map.
                                trailing = "₹${IndianNumberFormat.format(r.investmentValuesById[inv.id] ?: inv.currentValue)}",
                            )
                        }
                    }
                    if (r.insurances.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.search_section_insurances), r.insurances.size) }
                        items(r.insurances, key = { "ins-${it.id}" }) { ins ->
                            EntityRow(
                                icon = Icons.Filled.Shield,
                                title = ins.name,
                                subtitle = ins.provider,
                                onClick = { onOpenInsurance(ins.id) },
                                trailing = "₹${IndianNumberFormat.format(ins.sumAssured)}",
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

// ─────────────────────────── input bar ───────────────────────────────────────

@Composable
private fun SearchInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.search_back),
                tint = Text1,
                modifier = Modifier.size(22.dp),
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Surface2)
                .border(1.dp, Line1, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = Text3,
                modifier = Modifier.size(18.dp),
            )
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    cursorBrush = SolidColor(Teal300),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Search,
                    ),
                    textStyle = TextStyle(
                        fontFamily = PlusJakartaSans,
                        fontSize = 15.sp,
                        color = Text1,
                    ),
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                style = TextStyle(
                                    fontFamily = PlusJakartaSans,
                                    fontSize = 15.sp,
                                    color = Text4,
                                ),
                            )
                        }
                        inner()
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                )
            }
            if (value.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Surface4)
                        .clickable(onClick = onClear),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.search_clear),
                        tint = Text2,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────── section + row primitives ────────────────────────

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .height(1.dp)
                .background(Teal500)
                .size(width = 14.dp, height = 1.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = title.uppercase(),
            style = EyebrowStyle,
            color = Text3,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = count.toString(),
            style = TextStyle(
                fontFamily = IbmPlexMono,
                fontSize = 11.sp,
                color = Text3,
                fontFeatureSettings = "tnum, lnum",
            ),
        )
    }
}

/** Generic entity row: 36dp Surface4 icon tile + title + subtitle + optional trailing. */
@Composable
private fun EntityRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, trailing: String? = null) {
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line1, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface4),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Teal300,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Text1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = TextStyle(
                            fontFamily = IbmPlexMono,
                            fontSize = 11.sp,
                            color = Text3,
                            fontFeatureSettings = "tnum, lnum",
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.size(8.dp))
                Text(
                    text = trailing,
                    style = TextStyle(
                        fontFamily = InstrumentSerif,
                        fontSize = 16.sp,
                        color = Text1,
                        fontFeatureSettings = "tnum, lnum",
                    ),
                )
            }
        }
    }
}

/** Transaction-specific row: signed amount on the right, Income green / Expense coral. */
@Composable
private fun TransactionResultRow(txn: Transaction, onClick: () -> Unit) {
    val incomey = txn.type in setOf(
        TransactionType.INCOME,
        TransactionType.REFUND,
        TransactionType.CASHBACK,
        TransactionType.INTEREST,
        TransactionType.LOAN_RECEIVED,
        TransactionType.GIFT_RECEIVED,
    )
    val amountColor = if (incomey) Income else Expense
    val signed = (if (incomey) "" else "–") + "₹" + IndianNumberFormat.format(txn.amount).removePrefix("–₹").removePrefix("₹")

    Surface(
        color = Surface2,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line1, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface4),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Receipt,
                    contentDescription = null,
                    tint = Teal300,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = txn.description.ifBlank { "(no description)" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Text1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = DateFormatter.longDate(txn.date),
                    style = TextStyle(
                        fontFamily = IbmPlexMono,
                        fontSize = 11.sp,
                        color = Text3,
                        fontFeatureSettings = "tnum, lnum",
                    ),
                )
            }
            Text(
                text = signed,
                style = TextStyle(
                    fontFamily = InstrumentSerif,
                    fontSize = 16.sp,
                    color = amountColor,
                    fontFeatureSettings = "tnum, lnum",
                    letterSpacing = (-0.01).em,
                ),
            )
        }
    }
}

// ─────────────────────────── empty states ────────────────────────────────────

@Composable
private fun EmptyHint() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Bolt,
            contentDescription = null,
            tint = Text3,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.search_empty_title),
            style = MaterialTheme.typography.titleSmall,
            color = Text2,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.search_empty_body),
            style = MaterialTheme.typography.bodySmall,
            color = Text3,
        )
    }
}

@Composable
private fun NoResults(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = Text3,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.search_no_results_title, query),
            style = MaterialTheme.typography.titleSmall,
            color = Text2,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.search_no_results_body),
            style = MaterialTheme.typography.bodySmall,
            color = Text3,
        )
    }
}

@Suppress("unused")
private val keepCategoryRef = Category::class

@Suppress("unused")
private val keepTagRef = Tag::class

@Suppress("unused")
private val keepInvRef = Investment::class

@Suppress("unused")
private val keepInsRef = Insurance::class

@Suppress("unused")
private val keepPersonRef = Person::class

@Suppress("unused")
private val keepAccountRef = Account::class

@Suppress("unused")
private val keepCardRef = Card::class
