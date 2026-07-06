package com.subramanya.artha.ui.transaction

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.CategoryType
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.ui.common.ArthaSheetHandle
import com.subramanya.artha.ui.theme.Danger
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.Indigo
import com.subramanya.artha.ui.theme.IndigoDeep
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface3
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.ui.theme.Text4
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * The full-height Add Transaction modal sheet. Hosts the tab row, all fields, and
 * the bottom Save button. State and persistence live in [AddTransactionViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(viewModel: AddTransactionViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val funds by viewModel.fundsCatalogue.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val people by viewModel.people.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showSubCategoryPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Close the sheet when the VM signals a successful save.
    LaunchedEffect(state.savedAndClose) {
        if (state.savedAndClose) {
            viewModel.acknowledgeClose()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface3,
        contentWindowInsets = com.subramanya.artha.ui.common.SheetWindowInsets,
        dragHandle = { ArthaSheetHandle() },
    ) {
        SheetBody(
            state = state,
            funds = funds,
            people = people,
            tags = tags,
            viewModel = viewModel,
            onChooseCategory = { showCategoryPicker = true },
            onChooseSubCategory = { showSubCategoryPicker = true },
            onChooseDate = { showDatePicker = true },
            onChooseTime = { showTimePicker = true },
            onCancel = onDismiss,
        )
    }

    state.pendingSpousePrompt?.let { prompt ->
        SpousePromptDialog(
            amount = prompt.amount,
            personName = prompt.personName,
            onCancel = viewModel::cancelSpousePrompt,
            onSave = { choice, persistDefault ->
                viewModel.respondToSpousePrompt(choice, persistDefault)
            },
        )
    }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            categories = categories,
            type = categoryTypeForTab(state.tab),
            onSelected = {
                viewModel.onCategorySelected(it)
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
        )
    }

    if (showSubCategoryPicker && state.categoryId != null) {
        val children = remember(state.categoryId, categories) {
            categories.filter { it.parentId == state.categoryId && it.type == categoryTypeForTab(state.tab) }
        }
        // If the parent has no children, the picker is meaningless — close immediately.
        LaunchedEffect(children.isEmpty()) {
            if (children.isEmpty()) showSubCategoryPicker = false
        }
        if (children.isNotEmpty()) {
            CategoryPickerSheet(
                categories = children,
                type = categoryTypeForTab(state.tab),
                onSelected = {
                    viewModel.onSubCategorySelected(it)
                    showSubCategoryPicker = false
                },
                onDismiss = { showSubCategoryPicker = false },
            )
        }
    }

    if (showDatePicker) {
        DatePickerSheet(
            initialMillis = state.dateTimeMillis,
            onConfirm = { millis ->
                viewModel.onDateTimeChanged(mergeDateKeepingTime(millis, state.dateTimeMillis))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialMillis = state.dateTimeMillis,
            onConfirm = { hours, minutes ->
                viewModel.onDateTimeChanged(mergeTimeKeepingDate(hours, minutes, state.dateTimeMillis))
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SheetBody(
    state: AddTransactionUiState,
    funds: List<FundsEndpoint>,
    people: List<com.subramanya.artha.domain.model.Person>,
    tags: List<com.subramanya.artha.domain.model.Tag>,
    viewModel: AddTransactionViewModel,
    onChooseCategory: () -> Unit,
    onChooseSubCategory: () -> Unit,
    onChooseDate: () -> Unit,
    onChooseTime: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            // Lift the sheet body when the soft keyboard appears so the
            // description / notes fields aren't covered. Without this the
            // user can't see what they're typing once focus reaches the
            // lower-half fields.
            .imePadding(),
    ) {
        // ----- header -----
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    if (state.isEditing) R.string.txn_title_edit else R.string.txn_title_new,
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.txn_cancel))
            }
        }

        // ----- segmented tabs (HANDOFF §3.8: Surface2 track, 4dp inset, Surface4 active pill) -----
        SegmentedTabs(
            selected = state.tab,
            onSelect = viewModel::onTabChanged,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            // ----- centered editorial amount input (HANDOFF §3.8) -----
            AmountInput(
                value = state.amountText,
                onValueChange = viewModel::onAmountChanged,
                showError = state.showValidationErrors && state.parsedAmount == null,
                tab = state.tab,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ----- date & time -----
            DateTimeRow(
                millis = state.dateTimeMillis,
                onChooseDate = onChooseDate,
                onChooseTime = onChooseTime,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ----- from / to pickers -----
            FundsPickers(
                state = state,
                funds = funds,
                viewModel = viewModel,
            )

            // ----- card-payment auto-flag hint -----
            if (state.showCardPaymentHint) {
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.txn_card_payment_hint)) },
                    leadingIcon = { Icon(Icons.Filled.CreditCard, contentDescription = null) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                )
            }

            // ----- category + sub-category (not for Transfer / Invest) -----
            // INVEST flows save as INVESTMENT_BUY with no category — the destination
            // investment already represents what the money went into.
            if (state.tab != TransactionTab.TRANSFER && state.tab != TransactionTab.INVEST) {
                Spacer(modifier = Modifier.height(20.dp))
                CategoryField(
                    state = state,
                    viewModel = viewModel,
                    onChooseCategory = onChooseCategory,
                    onChooseSubCategory = onChooseSubCategory,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ----- description -----
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChanged,
                singleLine = true,
                label = { Text(stringResource(R.string.txn_description_label)) },
                placeholder = { Text(stringResource(R.string.txn_description_placeholder)) },
                isError = state.showValidationErrors && state.description.isBlank(),
                supportingText = {
                    if (state.showValidationErrors && state.description.isBlank()) {
                        Text(stringResource(R.string.txn_validation_description))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ----- notes (kept right under the Merchant field) -----
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChanged,
                label = { Text(stringResource(R.string.txn_notes_label)) },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ----- payment app -----
            PaymentAppPicker(
                selected = state.paymentApp,
                onSelected = viewModel::onPaymentAppChanged,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ----- people -----
            PersonPicker(
                people = people,
                selectedIds = state.peopleIds,
                onToggle = viewModel::togglePerson,
                onAddPerson = viewModel::addPersonInline,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ----- place (with disabled GPS button) -----
            PlaceField(
                value = state.place,
                onValueChange = viewModel::onPlaceChanged,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ----- tags -----
            TagPicker(
                tags = tags,
                selectedIds = state.tagIds,
                onToggle = viewModel::toggleTag,
                onAddTag = viewModel::addTagInline,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ----- receipt -----
            ReceiptPicker(
                uri = state.receiptUri,
                onPicked = viewModel::onReceiptPicked,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ----- tab-tinted save button (HANDOFF §3.8 — "Save expense · ₹420") -----
        TabTintedSaveButton(
            tab = state.tab,
            amount = state.parsedAmount,
            enabled = state.isValid && !state.isSaving,
            isEditing = state.isEditing,
            onClick = { viewModel.trySave() },
        )
    }
}

// --------- field composables (kept private to this file for cohesion) ---------

/**
 * HANDOFF §3.8 — segmented tabs with Surface2 track, 4dp inset, Surface4 active pill,
 * tab-tinted active label (Text1 expense, Income sage, Indigo transfer).
 */
@Composable
private fun SegmentedTabs(selected: TransactionTab, onSelect: (TransactionTab) -> Unit) {
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TransactionTab.entries.forEach { tab ->
                val isActive = tab == selected
                val activeTint = when (tab) {
                    TransactionTab.EXPENSE -> Text1
                    TransactionTab.INCOME -> Income
                    TransactionTab.TRANSFER -> Indigo
                    TransactionTab.INVEST -> Teal500
                }
                Surface(
                    color = if (isActive) Surface4 else Color.Transparent,
                    contentColor = if (isActive) activeTint else Text2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable { onSelect(tab) },
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = tab.label(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * HANDOFF §3.8 — centered editorial amount entry: 32sp ₹ in Text3 +
 * 64sp Instrument Serif weight-300 amount in tab-tinted color. Tabular numerals,
 * tight letterspacing (-0.02em). No box, no underline — pure typography.
 */
@Composable
private fun AmountInput(value: String, onValueChange: (String) -> Unit, showError: Boolean, tab: TransactionTab) {
    val tint = when (tab) {
        TransactionTab.EXPENSE -> Text1
        TransactionTab.INCOME -> Income
        TransactionTab.TRANSFER -> Indigo
        TransactionTab.INVEST -> Teal500
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.txn_amount_eyebrow).uppercase(),
            style = EyebrowStyle,
            color = Text3,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "₹",
                color = Text3,
                style = TextStyle(
                    fontFamily = InstrumentSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    letterSpacing = (-0.02).em,
                    fontFeatureSettings = "tnum, lnum",
                ),
                modifier = Modifier.padding(end = 6.dp, bottom = 8.dp),
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(tint),
                textStyle = TextStyle(
                    fontFamily = InstrumentSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 64.sp,
                    lineHeight = 72.sp,
                    letterSpacing = (-0.02).em,
                    color = tint,
                    textAlign = TextAlign.Center,
                    fontFeatureSettings = "tnum, lnum",
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (value.isEmpty()) {
                            Text(
                                text = stringResource(R.string.txn_amount_placeholder),
                                color = Text4,
                                style = TextStyle(
                                    fontFamily = InstrumentSerif,
                                    fontSize = 64.sp,
                                    lineHeight = 72.sp,
                                    letterSpacing = (-0.02).em,
                                    textAlign = TextAlign.Center,
                                    fontFeatureSettings = "tnum, lnum",
                                ),
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.widthIn(min = 80.dp, max = 280.dp),
            )
        }
        if (showError) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.txn_validation_amount),
                color = Danger,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * HANDOFF §3.8 — bottom Save button tinted by tab. Label format:
 * "Save expense · ₹420" using IndianNumberFormat. Disabled state stays muted.
 */
@Composable
private fun TabTintedSaveButton(tab: TransactionTab, amount: Double?, enabled: Boolean, isEditing: Boolean, onClick: () -> Unit) {
    val (container, content) = when (tab) {
        TransactionTab.EXPENSE -> Teal700 to Text1
        TransactionTab.INCOME -> Income to Color(0xFF06281C)
        TransactionTab.TRANSFER -> IndigoDeep to Text1
        TransactionTab.INVEST -> Teal500 to Color(0xFF06281C)
    }
    val labelRes = when {
        isEditing -> R.string.txn_update_fmt
        tab == TransactionTab.EXPENSE -> R.string.txn_save_expense_fmt
        tab == TransactionTab.INCOME -> R.string.txn_save_income_fmt
        tab == TransactionTab.TRANSFER -> R.string.txn_save_transfer_fmt
        else -> R.string.txn_save_invest_fmt
    }
    // IndianNumberFormat.format already prefixes ₹ — don't double it.
    val priceText = amount?.let { IndianNumberFormat.format(it) } ?: "₹0"
    Surface(color = Surface1, modifier = Modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = Line1, thickness = Dp.Hairline)
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = container,
                    contentColor = content,
                    disabledContainerColor = Surface3,
                    disabledContentColor = Text3,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 24.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(labelRes, priceText),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum, lnum",
                    ),
                )
            }
        }
    }
}

@Composable
private fun DateTimeRow(millis: Long, onChooseDate: () -> Unit, onChooseTime: () -> Unit) {
    val (dateLabel, timeLabel) = remember(millis) { humanDateTime(millis) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.txn_date_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AssistChip(
                onClick = onChooseDate,
                label = { Text(dateLabel) },
                leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
            )
            AssistChip(
                onClick = onChooseTime,
                label = { Text(timeLabel) },
                leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FundsPickers(state: AddTransactionUiState, funds: List<FundsEndpoint>, viewModel: AddTransactionViewModel) {
    val sourceLabel = when (state.tab) {
        TransactionTab.EXPENSE, TransactionTab.TRANSFER, TransactionTab.INVEST -> R.string.txn_from_label
        TransactionTab.INCOME -> R.string.txn_to_label
    }
    // The source list never contains investments — you don't pay an expense FROM an
    // RD, and an Invest-tab "from" is the bank account funding the SIP/RD/FD.
    val sourceOptions = funds.filter { it.kind != SourceKind.INVESTMENT }
    Text(stringResource(sourceLabel), style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(8.dp))
    if (sourceOptions.isEmpty()) {
        Text(
            text = stringResource(R.string.txn_no_accounts),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sourceOptions.forEach { ep ->
                FilterChip(
                    selected = state.source?.id == ep.id && state.source.kind == ep.kind,
                    onClick = { viewModel.onSourceSelected(ep) },
                    label = { Text(ep.displayName) },
                    leadingIcon = if (ep.kind == SourceKind.CARD) {
                        { Icon(Icons.Filled.CreditCard, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
    if (state.showValidationErrors && state.source == null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.txn_validation_source),
            style = MaterialTheme.typography.bodySmall,
            color = com.subramanya.artha.ui.theme.Danger,
        )
    }

    if (state.tab == TransactionTab.TRANSFER || state.tab == TransactionTab.INVEST) {
        Spacer(modifier = Modifier.height(16.dp))
        val destLabel = if (state.tab == TransactionTab.INVEST) {
            R.string.txn_invest_destination_label
        } else {
            R.string.txn_to_label
        }
        Text(stringResource(destLabel), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        // Invest tab restricts destinations to investments; Transfer tab restricts to
        // accounts and cards (an investment can't be the destination of a vanilla transfer).
        val destOptions = when (state.tab) {
            TransactionTab.INVEST -> funds.filter { it.kind == SourceKind.INVESTMENT }
            else -> funds.filter { it.kind != SourceKind.INVESTMENT }
        }
        if (destOptions.isEmpty()) {
            val emptyMsg = if (state.tab == TransactionTab.INVEST) {
                R.string.txn_invest_no_investments
            } else {
                R.string.txn_no_accounts
            }
            Text(
                text = stringResource(emptyMsg),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                destOptions.forEach { ep ->
                    FilterChip(
                        selected = state.destination?.id == ep.id && state.destination.kind == ep.kind,
                        onClick = { viewModel.onDestinationSelected(ep) },
                        label = { Text(ep.displayName) },
                        leadingIcon = if (ep.kind == SourceKind.CARD) {
                            { Icon(Icons.Filled.CreditCard, contentDescription = null) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        if (state.showValidationErrors && state.destination == null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.txn_validation_destination),
                style = MaterialTheme.typography.bodySmall,
                color = com.subramanya.artha.ui.theme.Danger,
            )
        }
        // Same-source-destination check
        if (state.source != null &&
            state.destination != null &&
            state.source.kind == state.destination.kind &&
            state.source.id == state.destination.id
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.txn_validation_same_source_dest),
                style = MaterialTheme.typography.bodySmall,
                color = com.subramanya.artha.ui.theme.Danger,
            )
        }
    }
}

@Composable
private fun CategoryField(
    state: AddTransactionUiState,
    viewModel: AddTransactionViewModel,
    onChooseCategory: () -> Unit,
    onChooseSubCategory: () -> Unit,
) {
    val children = viewModel.childrenOf(state.categoryId, categoryTypeForTab(state.tab))
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.txn_category_label), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onChooseCategory, modifier = Modifier.fillMaxWidth()) {
            Text(state.categoryDisplay ?: stringResource(R.string.txn_category_choose))
        }
        if (state.showValidationErrors && state.categoryId == null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.txn_validation_category),
                style = MaterialTheme.typography.bodySmall,
                color = com.subramanya.artha.ui.theme.Danger,
            )
        }
        if (state.categoryId != null && children.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.txn_subcategory_optional),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onChooseSubCategory, modifier = Modifier.fillMaxWidth()) {
                Text(state.subCategoryDisplay ?: stringResource(R.string.txn_subcategory_label))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaymentAppPicker(selected: PaymentApp, onSelected: (PaymentApp) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.txn_payment_app_label), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentApp.entries.forEach { app ->
                FilterChip(
                    selected = app == selected,
                    onClick = { onSelected(app) },
                    label = { Text(app.displayLabel()) },
                )
            }
        }
    }
}

@Composable
private fun PlaceField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = { Text(stringResource(R.string.txn_place_label)) },
        placeholder = { Text(stringResource(R.string.txn_place_placeholder)) },
        trailingIcon = {
            // GPS is intentionally disabled per playbook; renders as a non-interactive icon.
            Box(modifier = Modifier.alpha(0.35f)) {
                Icon(Icons.Filled.LocationOn, contentDescription = stringResource(R.string.txn_place_gps_todo))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ReceiptPicker(uri: String?, onPicked: (String?) -> Unit) {
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { result -> if (result != null) onPicked(result.toString()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.txn_receipt_label), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    // Camera capture needs FileProvider plumbing — flagged for follow-up.
                    Toast.makeText(context, R.string.txn_receipt_camera_todo, Toast.LENGTH_SHORT).show()
                },
            ) {
                Icon(Icons.Filled.Camera, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.txn_receipt_camera))
            }
            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            ) {
                Icon(Icons.Filled.Image, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.txn_receipt_gallery))
            }
            if (uri != null) {
                TextButton(onClick = { onPicked(null) }) {
                    Text(stringResource(R.string.txn_receipt_remove))
                }
            }
        }
        if (uri != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.txn_receipt_attached),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(initialMillis: Long, onConfirm: (Long) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { pickerState.selectedDateMillis?.let(onConfirm) }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initialMillis: Long, onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val initial = remember(initialMillis) {
        Instant.fromEpochMilliseconds(initialMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    }
    val pickerState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
        text = { TimePicker(state = pickerState) },
    )
}

// --------- helpers ---------

@Composable
private fun TransactionTab.label(): String = when (this) {
    TransactionTab.EXPENSE -> stringResource(R.string.txn_tab_expense)
    TransactionTab.INCOME -> stringResource(R.string.txn_tab_income)
    TransactionTab.TRANSFER -> stringResource(R.string.txn_tab_transfer)
    TransactionTab.INVEST -> stringResource(R.string.txn_tab_invest)
}

@Composable
private fun PaymentApp.displayLabel(): String = when (this) {
    PaymentApp.GPAY -> stringResource(R.string.payment_app_gpay)
    PaymentApp.PHONEPE -> stringResource(R.string.payment_app_phonepe)
    PaymentApp.PAYTM -> stringResource(R.string.payment_app_paytm)
    PaymentApp.CRED -> stringResource(R.string.payment_app_cred)
    PaymentApp.BHIM -> stringResource(R.string.payment_app_bhim)
    PaymentApp.BANK_APP -> stringResource(R.string.payment_app_bank_app)
    PaymentApp.CARD_SWIPE -> stringResource(R.string.payment_app_card_swipe)
    PaymentApp.CASH -> stringResource(R.string.payment_app_cash)
    PaymentApp.NETBANKING -> stringResource(R.string.payment_app_netbanking)
    PaymentApp.OTHER -> stringResource(R.string.payment_app_other)
}

private fun categoryTypeForTab(tab: TransactionTab): CategoryType = when (tab) {
    TransactionTab.EXPENSE -> CategoryType.EXPENSE
    TransactionTab.INCOME -> CategoryType.INCOME
    TransactionTab.TRANSFER -> CategoryType.TRANSFER
    // INVEST flows don't show the category picker so the type doesn't matter —
    // fall through to TRANSFER so the helper stays total.
    TransactionTab.INVEST -> CategoryType.TRANSFER
}

/** Returns ("Thu, 21 May", "14:30") for the date+time chips. */
private fun humanDateTime(millis: Long): Pair<String, String> {
    val ldt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    val date = DateFormatter.shortDate(ldt.date)
    val time = "%02d:%02d".format(ldt.hour, ldt.minute)
    return date to time
}

/** Replace only the date portion of [existingMillis] with the chosen date. */
private fun mergeDateKeepingTime(chosenDateMillis: Long, existingMillis: Long): Long {
    val tz = TimeZone.currentSystemDefault()
    val newDate = Instant.fromEpochMilliseconds(chosenDateMillis).toLocalDateTime(tz).date
    val oldLdt = Instant.fromEpochMilliseconds(existingMillis).toLocalDateTime(tz)
    val merged = LocalDateTime(
        year = newDate.year,
        monthNumber = newDate.monthNumber,
        dayOfMonth = newDate.dayOfMonth,
        hour = oldLdt.hour,
        minute = oldLdt.minute,
        second = oldLdt.second,
        nanosecond = oldLdt.nanosecond,
    )
    return merged.toInstant(tz).toEpochMilliseconds()
}

/** Replace only the time portion of [existingMillis] with the chosen hour/minute. */
private fun mergeTimeKeepingDate(hour: Int, minute: Int, existingMillis: Long): Long {
    val tz = TimeZone.currentSystemDefault()
    val ldt = Instant.fromEpochMilliseconds(existingMillis).toLocalDateTime(tz)
    val merged = LocalDateTime(
        year = ldt.year,
        monthNumber = ldt.monthNumber,
        dayOfMonth = ldt.dayOfMonth,
        hour = hour,
        minute = minute,
        second = 0,
        nanosecond = 0,
    )
    return merged.toInstant(tz).toEpochMilliseconds()
}
