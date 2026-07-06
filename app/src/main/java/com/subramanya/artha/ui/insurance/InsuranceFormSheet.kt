package com.subramanya.artha.ui.insurance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.InsuranceType
import com.subramanya.artha.data.entity.enums.InvestmentType
import com.subramanya.artha.data.entity.enums.PremiumFrequency
import com.subramanya.artha.data.entity.enums.defaultValuationMode
import com.subramanya.artha.domain.model.Insurance
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
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface3
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.DateFormatter
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * HANDOFF sheets-extra.jsx · AddInsuranceSheet — Add/Edit Insurance policy.
 *
 * PRD §7.12 — endowment/ULIP policies offer a one-tap "create linked
 * Investment" toggle on new rows so cost-basis tracking carries through.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsuranceFormSheet(editing: Insurance?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var type by remember(editing) { mutableStateOf(editing?.type ?: InsuranceType.HEALTH) }
    var provider by remember(editing) { mutableStateOf(editing?.provider.orEmpty()) }
    var policyNumber by remember(editing) { mutableStateOf(editing?.policyNumber.orEmpty()) }
    var sumAssuredText by remember(editing) {
        mutableStateOf(editing?.sumAssured?.toPlainString() ?: "")
    }
    var premiumText by remember(editing) {
        mutableStateOf(editing?.premiumAmount?.toPlainString() ?: "")
    }
    var frequency by remember(editing) {
        mutableStateOf(editing?.premiumFrequency ?: PremiumFrequency.YEARLY)
    }
    var startDate by remember(editing) {
        mutableStateOf(editing?.startDate ?: System.currentTimeMillis())
    }
    var endDate by remember(editing) { mutableStateOf(editing?.endDate) }
    var nextDueDate by remember(editing) { mutableStateOf(editing?.nextPremiumDate) }
    var nominee by remember(editing) { mutableStateOf(editing?.nominee.orEmpty()) }
    var agent by remember(editing) { mutableStateOf(editing?.agentContact.orEmpty()) }
    var taxSection by remember(editing) {
        mutableStateOf(editing?.taxSection.orEmpty())
    }
    var color by remember(editing) { mutableStateOf(editing?.color ?: PALETTE.first()) }

    var createLinkedInvestment by remember { mutableStateOf(false) }

    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    var pickingDue by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val parsedSum = sumAssuredText.toDoubleOrNull()
    val parsedPremium = premiumText.toDoubleOrNull()
    val isValid =
        name.isNotBlank() && provider.isNotBlank() && parsedSum != null && parsedPremium != null

    val typeOptions = listOf(
        PillOption(InsuranceType.HEALTH, stringResource(R.string.insurance_type_health)),
        PillOption(InsuranceType.VEHICLE, stringResource(R.string.insurance_type_vehicle)),
        PillOption(InsuranceType.LIFE_TERM, stringResource(R.string.insurance_type_life_term)),
        PillOption(InsuranceType.LIFE_ENDOWMENT, stringResource(R.string.insurance_type_life_endowment)),
        PillOption(InsuranceType.TRAVEL, stringResource(R.string.insurance_type_travel)),
        PillOption(InsuranceType.HOME, stringResource(R.string.insurance_type_home)),
        PillOption(InsuranceType.OTHER, stringResource(R.string.insurance_type_other)),
    )
    val freqOptions = listOf(
        PillOption(PremiumFrequency.MONTHLY, stringResource(R.string.premium_frequency_monthly)),
        PillOption(PremiumFrequency.QUARTERLY, stringResource(R.string.premium_frequency_quarterly)),
        PillOption(PremiumFrequency.HALF_YEARLY, stringResource(R.string.premium_frequency_half_yearly)),
        PillOption(PremiumFrequency.YEARLY, stringResource(R.string.premium_frequency_yearly)),
        PillOption(PremiumFrequency.SINGLE, stringResource(R.string.premium_frequency_single)),
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
                        R.string.insurance_form_add_title
                    } else {
                        R.string.insurance_form_edit_title
                    },
                ),
            )

            FieldRow(label = stringResource(R.string.insurance_form_name_label)) {
                ArthaTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "HDFC Life Click 2 Protect",
                    isError = showErrors && name.isBlank(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
            }

            FieldRow(label = stringResource(R.string.insurance_form_type_label)) {
                PillRadio(value = type, options = typeOptions, onChange = { type = it })
            }

            // Linked-investment toggle: endowment-only, new rows only.
            if (editing == null && type == InsuranceType.LIFE_ENDOWMENT) {
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface2, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.insurance_form_link_investment_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Text1,
                        )
                        Text(
                            text = stringResource(R.string.insurance_form_link_investment_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = Text3,
                        )
                    }
                    Switch(
                        checked = createLinkedInvestment,
                        onCheckedChange = { createLinkedInvestment = it },
                    )
                }
            }

            FieldRow(label = stringResource(R.string.insurance_form_provider_label)) {
                ArthaTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    placeholder = "HDFC Life",
                    isError = showErrors && provider.isBlank(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
            }
            FieldRow(
                label = stringResource(R.string.insurance_form_policy_number_label),
                optional = true,
            ) {
                ArthaTextField(
                    value = policyNumber,
                    onValueChange = { policyNumber = it },
                    placeholder = "POL-123456",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
            }

            FieldRow(label = stringResource(R.string.insurance_form_sum_assured_label)) {
                ArthaTextField(
                    value = sumAssuredText,
                    onValueChange = { v ->
                        sumAssuredText = v.filterIndexed { i, c ->
                            c.isDigit() || (c == '.' && v.indexOf('.') == i)
                        }
                    },
                    placeholder = "1500000",
                    suffix = "₹",
                    isError = showErrors && parsedSum == null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                )
            }
            FieldRow(label = stringResource(R.string.insurance_form_premium_label)) {
                ArthaTextField(
                    value = premiumText,
                    onValueChange = { v ->
                        premiumText = v.filterIndexed { i, c ->
                            c.isDigit() || (c == '.' && v.indexOf('.') == i)
                        }
                    },
                    placeholder = "18400",
                    suffix = "₹",
                    isError = showErrors && parsedPremium == null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                )
            }
            FieldRow(label = stringResource(R.string.insurance_form_frequency_label)) {
                PillRadio(value = frequency, options = freqOptions, onChange = { frequency = it })
            }

            FieldRow(label = stringResource(R.string.insurance_form_start_label)) {
                SheetChip(
                    label = DateFormatter.longDate(startDate),
                    leading = Icons.Filled.CalendarMonth,
                    onClick = { pickingStart = true },
                )
            }
            FieldRow(
                label = stringResource(R.string.insurance_form_next_due_label),
                optional = true,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetChip(
                        label = nextDueDate?.let { DateFormatter.longDate(it) } ?: "+ Date",
                        leading = Icons.Filled.CalendarMonth,
                        onClick = { pickingDue = true },
                    )
                    if (nextDueDate != null) {
                        TextButton(onClick = { nextDueDate = null }) {
                            Text(stringResource(R.string.insurance_form_clear_due))
                        }
                    }
                }
            }
            FieldRow(
                label = stringResource(R.string.insurance_form_end_label),
                optional = true,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetChip(
                        label = endDate?.let { DateFormatter.longDate(it) } ?: "+ Date",
                        leading = Icons.Filled.CalendarMonth,
                        onClick = { pickingEnd = true },
                    )
                    if (endDate != null) {
                        TextButton(onClick = { endDate = null }) {
                            Text(stringResource(R.string.insurance_form_clear_end))
                        }
                    }
                }
            }

            FieldRow(
                label = stringResource(R.string.insurance_form_nominee_label),
                optional = true,
            ) {
                ArthaTextField(
                    value = nominee,
                    onValueChange = { nominee = it },
                    placeholder = "Nominee name",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
            }
            FieldRow(
                label = stringResource(R.string.insurance_form_agent_label),
                optional = true,
            ) {
                ArthaTextField(
                    value = agent,
                    onValueChange = { agent = it },
                    placeholder = "Agent contact",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
            }
            FieldRow(
                label = stringResource(R.string.insurance_form_tax_section_label),
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
                label = stringResource(R.string.insurance_form_save),
                onClick = {
                    if (!isValid) {
                        showErrors = true
                        return@SavePrimaryButton
                    }
                    val now = System.currentTimeMillis()
                    val resolvedId = editing?.id ?: UUID.randomUUID().toString()
                    val resolvedTax = taxSection.trim().takeIf { it.isNotBlank() }
                        ?: defaultTaxSectionFor(type)
                    val resolved = Insurance(
                        id = resolvedId,
                        name = name.trim(),
                        type = type,
                        provider = provider.trim(),
                        policyNumber = policyNumber.trim().takeIf { it.isNotBlank() },
                        sumAssured = parsedSum ?: 0.0,
                        premiumAmount = parsedPremium ?: 0.0,
                        premiumFrequency = frequency,
                        nextPremiumDate = nextDueDate,
                        startDate = startDate,
                        endDate = endDate,
                        nominee = nominee.trim().takeIf { it.isNotBlank() },
                        agentContact = agent.trim().takeIf { it.isNotBlank() },
                        policyDocUri = editing?.policyDocUri,
                        taxSection = resolvedTax,
                        icon = editing?.icon ?: "shield",
                        color = color,
                        isArchived = editing?.isArchived ?: false,
                        createdAt = editing?.createdAt ?: now,
                    )
                    scope.launch {
                        app.insuranceRepository.upsert(resolved)
                        if (editing == null && createLinkedInvestment) {
                            val linkedId = UUID.randomUUID().toString()
                            app.investmentRepository.upsert(
                                Investment(
                                    id = linkedId,
                                    name = name.trim(),
                                    type = InvestmentType.ULIP,
                                    institution = provider.trim().takeIf { it.isNotBlank() },
                                    // Seed the linked ULIP's value from the first premium so it
                                    // doesn't show ₹0 forever (MARKET value is the manual figure;
                                    // the user updates it as the fund grows).
                                    currentValue = resolved.premiumAmount,
                                    valuationMode = InvestmentType.ULIP.defaultValuationMode(),
                                    openingContribution = resolved.premiumAmount,
                                    units = null,
                                    nav = null,
                                    startDate = startDate,
                                    maturityDate = endDate,
                                    taxSection = resolvedTax,
                                    icon = "savings",
                                    color = color,
                                    linkedInsuranceId = resolvedId,
                                    isArchived = false,
                                    displayOrder = (now / 1000).toInt(),
                                    createdAt = now,
                                ),
                            )
                        }
                        onDismiss()
                    }
                },
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    if (pickingStart) {
        DatePickerSheet(
            initialEpoch = startDate,
            onConfirm = {
                startDate = it
                pickingStart = false
            },
            onDismiss = { pickingStart = false },
        )
    }
    if (pickingDue) {
        DatePickerSheet(
            initialEpoch = nextDueDate ?: System.currentTimeMillis(),
            onConfirm = {
                nextDueDate = it
                pickingDue = false
            },
            onDismiss = { pickingDue = false },
        )
    }
    if (pickingEnd) {
        DatePickerSheet(
            initialEpoch = endDate ?: System.currentTimeMillis(),
            onConfirm = {
                endDate = it
                pickingEnd = false
            },
            onDismiss = { pickingEnd = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(initialEpoch: Long, onConfirm: (Long) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialEpoch)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            // Disable Confirm when no date is selected — used to close silently
            // (falling through to onDismiss) which felt like the dialog was
            // ignoring the user.
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let(onConfirm) ?: onDismiss()
                },
                enabled = pickerState.selectedDateMillis != null,
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    ) { DatePicker(state = pickerState) }
}

private fun defaultTaxSectionFor(type: InsuranceType): String? = when (type) {
    InsuranceType.HEALTH -> "80D"
    InsuranceType.LIFE_TERM, InsuranceType.LIFE_ENDOWMENT -> "80C"
    else -> null
}

private fun Double.toPlainString(): String = if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

private val PALETTE: List<Long> = listOf(
    0xFF0F766EL,
    0xFF5260A8L,
    0xFF2F8F6BL,
    0xFFC97A2AL,
    0xFFB14A6EL,
    0xFF7D5BB8L,
)
