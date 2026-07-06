package com.subramanya.artha.ui.investments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.InvestmentType
import com.subramanya.artha.data.entity.enums.ValuationMode
import com.subramanya.artha.data.entity.enums.defaultValuationMode
import com.subramanya.artha.domain.model.Investment
import com.subramanya.artha.ui.common.ArthaSheetHandle
import com.subramanya.artha.ui.common.ArthaTextField
import com.subramanya.artha.ui.common.ColorSwatchRow
import com.subramanya.artha.ui.common.FieldRow
import com.subramanya.artha.ui.common.PillOption
import com.subramanya.artha.ui.common.PillRadio
import com.subramanya.artha.ui.common.SavePrimaryButton
import com.subramanya.artha.ui.common.SheetChip
import com.subramanya.artha.ui.common.SheetTitle
import com.subramanya.artha.ui.common.SheetWindowInsets
import com.subramanya.artha.ui.theme.Surface3
import com.subramanya.artha.utils.DateFormatter
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * HANDOFF sheets-extra.jsx · AddInvestmentSheet — Add/Edit Investment.
 *
 * Type drives which optional fields surface:
 *   - FD / RD / Bonds → maturity date matters
 *   - MUTUAL_FUND / EQUITY / SIP / Gold-Digital → units + NAV
 *   - PPF / EPF / NPS / ULIP / OTHER → just core fields
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentFormSheet(editing: Investment?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var type by remember(editing) { mutableStateOf(editing?.type ?: InvestmentType.MUTUAL_FUND) }
    var institution by remember(editing) { mutableStateOf(editing?.institution.orEmpty()) }
    var currentValueText by remember(editing) {
        mutableStateOf(editing?.currentValue?.toPlainString() ?: "")
    }
    // Opening contribution: for DERIVED deposits this is "what's in the deposit now",
    // for MARKET instruments it's the optional "invested so far" amount.
    var openingContributionText by remember(editing) {
        mutableStateOf(editing?.openingContribution?.toPlainString() ?: "")
    }
    var unitsText by remember(editing) {
        mutableStateOf(editing?.units?.toPlainString() ?: "")
    }
    var navText by remember(editing) {
        mutableStateOf(editing?.nav?.toPlainString() ?: "")
    }
    var startDate by remember(editing) {
        mutableStateOf(editing?.startDate ?: System.currentTimeMillis())
    }
    var maturityDate by remember(editing) { mutableStateOf(editing?.maturityDate) }
    var taxSection by remember(editing) { mutableStateOf(editing?.taxSection.orEmpty()) }
    var color by remember(editing) { mutableStateOf(editing?.color ?: PALETTE.first()) }

    var showErrors by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingMaturity by remember { mutableStateOf(false) }

    val parsedCurrentValue = remember(currentValueText) {
        if (currentValueText.isBlank()) 0.0 else currentValueText.toDoubleOrNull()
    }
    val parsedUnits = remember(unitsText) {
        if (unitsText.isBlank()) null else unitsText.toDoubleOrNull()
    }
    val parsedNav = remember(navText) {
        if (navText.isBlank()) null else navText.toDoubleOrNull()
    }
    val parsedOpeningContribution = remember(openingContributionText) {
        if (openingContributionText.isBlank()) 0.0 else openingContributionText.toDoubleOrNull()
    }

    // Valuation mode is derived from the selected type and re-derives whenever the
    // type changes — DERIVED deposits compute their value from contributions + interest,
    // MARKET instruments use the manually-entered current value.
    val valuationMode = remember(type) { type.defaultValuationMode() }
    val isDerived = valuationMode == ValuationMode.DERIVED

    // For DERIVED, the opening-contribution field is the primary amount and must parse;
    // for MARKET, the current-value field is the one that must parse.
    val isValid = name.isNotBlank() &&
        if (isDerived) parsedOpeningContribution != null else parsedCurrentValue != null

    val showUnitsAndNav = type in setOf(
        InvestmentType.SIP,
        InvestmentType.MUTUAL_FUND,
        InvestmentType.EQUITY,
        InvestmentType.GOLD_DIGITAL,
    )
    val showMaturity = type in setOf(InvestmentType.FD, InvestmentType.RD, InvestmentType.BONDS)

    val typeOptions = listOf(
        PillOption(InvestmentType.FD, stringResource(R.string.investment_type_fd)),
        PillOption(InvestmentType.RD, stringResource(R.string.investment_type_rd)),
        PillOption(InvestmentType.SIP, stringResource(R.string.investment_type_sip)),
        PillOption(InvestmentType.MUTUAL_FUND, stringResource(R.string.investment_type_mutual_fund)),
        PillOption(InvestmentType.EQUITY, stringResource(R.string.investment_type_equity)),
        PillOption(InvestmentType.GOLD_PHYSICAL, stringResource(R.string.investment_type_gold_physical)),
        PillOption(InvestmentType.GOLD_DIGITAL, stringResource(R.string.investment_type_gold_digital)),
        PillOption(InvestmentType.BONDS, stringResource(R.string.investment_type_bonds)),
        PillOption(InvestmentType.PPF, stringResource(R.string.investment_type_ppf)),
        PillOption(InvestmentType.EPF, stringResource(R.string.investment_type_epf)),
        PillOption(InvestmentType.NPS, stringResource(R.string.investment_type_nps)),
        PillOption(InvestmentType.ULIP, stringResource(R.string.investment_type_ulip)),
        PillOption(InvestmentType.OTHER, stringResource(R.string.investment_type_other)),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface3,
        contentWindowInsets = SheetWindowInsets,
        dragHandle = { ArthaSheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            SheetTitle(
                title = stringResource(
                    if (editing == null) {
                        R.string.investment_form_add_title
                    } else {
                        R.string.investment_form_edit_title
                    },
                ),
            )

            FieldRow(label = stringResource(R.string.investment_form_name_label)) {
                ArthaTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Nifty 50 Index Fund",
                    isError = showErrors && name.isBlank(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
            }

            FieldRow(label = stringResource(R.string.investment_form_type_label)) {
                PillRadio(value = type, options = typeOptions, onChange = { type = it })
            }

            FieldRow(
                label = stringResource(R.string.investment_form_institution_label),
                optional = true,
            ) {
                ArthaTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    placeholder = "UTI Mutual Fund",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
            }

            // Type-aware value field:
            //   DERIVED (FD/RD/PPF/EPF/Bonds) → one "amount in the deposit" field bound to
            //     openingContribution; the displayed value is computed elsewhere from
            //     contributions + interest, so we never ask for a manual current value.
            //   MARKET (everything else) → the manual "Current value" field plus an optional
            //     "Invested so far" field used only to show a return %.
            if (isDerived) {
                FieldRow(label = stringResource(R.string.investment_form_opening_contribution_label)) {
                    ArthaTextField(
                        value = openingContributionText,
                        onValueChange = { v ->
                            openingContributionText = v.filterIndexed { i, c ->
                                c.isDigit() || (c == '.' && v.indexOf('.') == i)
                            }
                        },
                        placeholder = "60000",
                        suffix = "₹",
                        isError = showErrors && parsedOpeningContribution == null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                    )
                }
            } else {
                FieldRow(label = stringResource(R.string.investment_form_current_value_label)) {
                    ArthaTextField(
                        value = currentValueText,
                        onValueChange = { v ->
                            currentValueText = v.filterIndexed { i, c ->
                                c.isDigit() || (c == '.' && v.indexOf('.') == i)
                            }
                        },
                        placeholder = "124300",
                        suffix = "₹",
                        isError = showErrors && parsedCurrentValue == null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                    )
                }
                FieldRow(
                    label = stringResource(R.string.investment_form_invested_so_far_label),
                    optional = true,
                ) {
                    ArthaTextField(
                        value = openingContributionText,
                        onValueChange = { v ->
                            openingContributionText = v.filterIndexed { i, c ->
                                c.isDigit() || (c == '.' && v.indexOf('.') == i)
                            }
                        },
                        placeholder = "100000",
                        suffix = "₹",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                    )
                }
            }

            if (showUnitsAndNav) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        FieldRow(label = stringResource(R.string.investment_form_units_label)) {
                            ArthaTextField(
                                value = unitsText,
                                onValueChange = { v ->
                                    unitsText = v.filterIndexed { i, c ->
                                        c.isDigit() || (c == '.' && v.indexOf('.') == i)
                                    }
                                },
                                placeholder = "120.5",
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next,
                                ),
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        FieldRow(label = stringResource(R.string.investment_form_nav_label)) {
                            ArthaTextField(
                                value = navText,
                                onValueChange = { v ->
                                    navText = v.filterIndexed { i, c ->
                                        c.isDigit() || (c == '.' && v.indexOf('.') == i)
                                    }
                                },
                                placeholder = "1024",
                                suffix = "₹",
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next,
                                ),
                            )
                        }
                    }
                }
            }

            FieldRow(
                label = stringResource(R.string.investment_form_start_date_label),
                optional = true,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetChip(
                        label = DateFormatter.longDate(startDate),
                        leading = Icons.Filled.CalendarMonth,
                        onClick = { pickingStart = true },
                    )
                    if (showMaturity) {
                        SheetChip(
                            label = maturityDate?.let { DateFormatter.longDate(it) }
                                ?: "+ " + stringResource(R.string.investment_form_maturity_date_label),
                            leading = Icons.Filled.CalendarMonth,
                            onClick = { pickingMaturity = true },
                        )
                    }
                }
                if (showMaturity && maturityDate != null) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { maturityDate = null }) {
                        Text(stringResource(R.string.investment_form_clear_maturity))
                    }
                }
            }

            FieldRow(
                label = stringResource(R.string.investment_form_tax_section_label),
                optional = true,
            ) {
                ArthaTextField(
                    value = taxSection,
                    onValueChange = { taxSection = it.uppercase() },
                    placeholder = "80C",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
            }

            FieldRow(label = stringResource(R.string.investment_form_color_label)) {
                ColorSwatchRow(value = color, swatches = PALETTE, onChange = { color = it })
            }

            Spacer(Modifier.height(28.dp))
            SavePrimaryButton(
                label = stringResource(R.string.investment_form_save),
                onClick = {
                    if (!isValid) {
                        showErrors = true
                        return@SavePrimaryButton
                    }
                    val now = System.currentTimeMillis()
                    val openingContribution = parsedOpeningContribution ?: 0.0
                    val resolved = Investment(
                        id = editing?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        type = type,
                        institution = institution.trim().takeIf { it.isNotBlank() },
                        // DERIVED rows compute their value, so currentValue is unused for
                        // display — we mirror openingContribution into it (harmless). MARKET
                        // rows keep the manually-entered current value.
                        currentValue = if (isDerived) openingContribution else (parsedCurrentValue ?: 0.0),
                        valuationMode = valuationMode,
                        openingContribution = openingContribution,
                        units = if (showUnitsAndNav) parsedUnits else editing?.units,
                        nav = if (showUnitsAndNav) parsedNav else editing?.nav,
                        startDate = startDate,
                        maturityDate = if (showMaturity) maturityDate else editing?.maturityDate,
                        taxSection = taxSection.trim().takeIf { it.isNotBlank() },
                        icon = editing?.icon ?: "savings",
                        color = color,
                        linkedInsuranceId = editing?.linkedInsuranceId,
                        isArchived = editing?.isArchived ?: false,
                        displayOrder = editing?.displayOrder ?: nextDisplayOrder(),
                        createdAt = editing?.createdAt ?: now,
                    )
                    scope.launch {
                        app.investmentRepository.upsert(resolved)
                        onDismiss()
                    }
                },
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    if (pickingStart) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { pickingStart = false },
            confirmButton = {
                // Disable Confirm when no date is selected — used to close
                // silently with no feedback if the user tapped Save without
                // picking anything.
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { startDate = it }
                        pickingStart = false
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingStart = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }

    if (pickingMaturity) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = maturityDate ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingMaturity = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { maturityDate = it }
                        pickingMaturity = false
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingMaturity = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }
}

private fun nextDisplayOrder(): Int = (System.currentTimeMillis() / 1000L).toInt()

private fun Double.toPlainString(): String = if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

private val PALETTE: List<Long> = listOf(
    0xFF0F766EL,
    0xFF5260A8L,
    0xFF2F8F6BL,
    0xFFC97A2AL,
    0xFFB14A6EL,
    0xFF7D5BB8L,
)
