package com.subramanya.artha.ui.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import com.subramanya.artha.data.entity.enums.AccountType
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.ui.common.ArthaSheetHandle
import com.subramanya.artha.ui.common.ArthaTextField
import com.subramanya.artha.ui.common.ColorSwatchRow
import com.subramanya.artha.ui.common.FieldRow
import com.subramanya.artha.ui.common.IconChipRow
import com.subramanya.artha.ui.common.IconChoice
import com.subramanya.artha.ui.common.PillOption
import com.subramanya.artha.ui.common.PillRadio
import com.subramanya.artha.ui.common.SavePrimaryButton
import com.subramanya.artha.ui.common.SheetTitle
import com.subramanya.artha.ui.common.SheetWindowInsets
import com.subramanya.artha.ui.theme.Surface3
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * HANDOFF sheets-extra.jsx · AddAccountSheet — Add/Edit Account.
 *
 * Pattern: SheetTitle → FieldRow("Account name") → PillRadio(type) →
 * FieldRow("Institution") → FieldRow("Last 4") → FieldRow("Opening balance") →
 * FieldRow("Card color") + FieldRow("Icon") → SavePrimaryButton.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormSheet(editing: Account?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var type by remember(editing) { mutableStateOf(editing?.type ?: AccountType.SAVINGS) }
    var institution by remember(editing) { mutableStateOf(editing?.institution.orEmpty()) }
    var last4 by remember(editing) { mutableStateOf(editing?.accountNumberLast4.orEmpty()) }
    var openingText by remember(editing) {
        mutableStateOf(editing?.openingBalance?.toPlainString() ?: "")
    }
    var icon by remember(editing) { mutableStateOf(editing?.icon ?: ICONS.first().key) }
    var color by remember(editing) { mutableStateOf(editing?.color ?: PALETTE.first()) }

    var showErrors by remember { mutableStateOf(false) }
    val parsedBalance = remember(openingText) {
        if (openingText.isBlank()) 0.0 else openingText.toDoubleOrNull()
    }
    val last4Valid = last4.isEmpty() || (last4.length == 4 && last4.all { it.isDigit() })
    val isValid = name.isNotBlank() && parsedBalance != null && last4Valid

    val typeOptions = listOf(
        PillOption(AccountType.SAVINGS, stringResource(R.string.onboarding_account_type_savings)),
        PillOption(AccountType.CURRENT, stringResource(R.string.onboarding_account_type_current)),
        PillOption(AccountType.CASH, stringResource(R.string.onboarding_account_type_cash)),
        PillOption(AccountType.WALLET, stringResource(R.string.onboarding_account_type_wallet)),
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
                    if (editing == null) R.string.account_form_add_title else R.string.account_form_edit_title,
                ),
            )

            FieldRow(label = stringResource(R.string.account_form_name_label)) {
                ArthaTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "HDFC Savings",
                    isError = showErrors && name.isBlank(),
                    supportingText = if (showErrors && name.isBlank()) {
                        stringResource(R.string.account_form_validation_name)
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
            }

            FieldRow(label = stringResource(R.string.account_form_type_label)) {
                PillRadio(value = type, options = typeOptions, onChange = { type = it })
            }

            FieldRow(
                label = stringResource(R.string.account_form_institution_label),
                optional = true,
            ) {
                ArthaTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    placeholder = "HDFC Bank",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
            }

            FieldRow(
                label = stringResource(R.string.account_form_last4_label),
                optional = true,
            ) {
                ArthaTextField(
                    value = last4,
                    onValueChange = { v -> last4 = v.filter { it.isDigit() }.take(4) },
                    placeholder = "7421",
                    isError = showErrors && !last4Valid,
                    supportingText = if (showErrors && !last4Valid) {
                        stringResource(R.string.account_form_validation_last4)
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                )
            }

            FieldRow(label = stringResource(R.string.account_form_opening_label)) {
                ArthaTextField(
                    value = openingText,
                    onValueChange = { v ->
                        openingText = v.filterIndexed { i, c ->
                            c.isDigit() || (c == '.' && v.indexOf('.') == i)
                        }
                    },
                    placeholder = "0",
                    suffix = "₹",
                    isError = showErrors && parsedBalance == null,
                    supportingText = if (showErrors && parsedBalance == null) {
                        stringResource(R.string.account_form_validation_balance)
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                )
            }

            FieldRow(label = stringResource(R.string.account_form_color_label)) {
                ColorSwatchRow(value = color, swatches = PALETTE, onChange = { color = it })
            }

            FieldRow(label = stringResource(R.string.account_form_icon_label)) {
                IconChipRow(value = icon, icons = ICONS, onChange = { icon = it })
            }

            Spacer(Modifier.height(28.dp))
            SavePrimaryButton(
                label = stringResource(R.string.account_form_save),
                onClick = {
                    if (!isValid) {
                        showErrors = true
                        return@SavePrimaryButton
                    }
                    val now = System.currentTimeMillis()
                    val resolved = Account(
                        id = editing?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        type = type,
                        institution = institution.trim().takeIf { it.isNotBlank() },
                        accountNumberLast4 = last4.takeIf { it.length == 4 },
                        openingBalance = parsedBalance ?: 0.0,
                        currency = editing?.currency ?: "INR",
                        icon = icon,
                        color = color,
                        isArchived = editing?.isArchived ?: false,
                        displayOrder = editing?.displayOrder ?: nextDisplayOrder(),
                        createdAt = editing?.createdAt ?: now,
                    )
                    scope.launch {
                        app.accountRepository.upsert(resolved)
                        onDismiss()
                    }
                },
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun nextDisplayOrder(): Int = (System.currentTimeMillis() / 1000L).toInt()

private fun Double.toPlainString(): String = if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

private val PALETTE: List<Long> = listOf(
    0xFF0F766EL, // acc-teal
    0xFF5260A8L, // acc-indigo
    0xFF2F8F6BL, // acc-emerald
    0xFFC97A2AL, // acc-saffron
    0xFFB14A6EL, // acc-magenta
    0xFF7D5BB8L, // acc-violet
)

private val ICONS: List<IconChoice> = listOf(
    IconChoice("account_balance", Icons.Filled.AccountBalance),
    IconChoice("account_balance_wallet", Icons.Filled.AccountBalanceWallet),
    IconChoice("payments", Icons.Filled.Payments),
    IconChoice("savings", Icons.Filled.Savings),
)
