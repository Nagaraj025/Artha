package com.subramanya.artha.ui.cards

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.CardNetwork
import com.subramanya.artha.data.entity.enums.CardType
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.ui.common.ArthaSheetHandle
import com.subramanya.artha.ui.common.ArthaTextField
import com.subramanya.artha.ui.common.ColorSwatchRow
import com.subramanya.artha.ui.common.FieldRow
import com.subramanya.artha.ui.common.PillOption
import com.subramanya.artha.ui.common.PillRadio
import com.subramanya.artha.ui.common.SavePrimaryButton
import com.subramanya.artha.ui.common.SheetTitle
import com.subramanya.artha.ui.common.SheetWindowInsets
import com.subramanya.artha.ui.theme.Surface3
import com.subramanya.artha.ui.theme.Text3
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * HANDOFF sheets-extra.jsx · AddCardSheet — Add/Edit Card.
 *
 * Field visibility flexes by type:
 *   CREDIT  → name + network + issuer + last4 + creditLimit + statementDay + dueDay
 *   DEBIT   → name + network + issuer + last4 + linkedAccount
 *   PREPAID → name + network + issuer + last4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardFormSheet(editing: Card?, onDismiss: () -> Unit, prefillLast4: String? = null) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val accounts by app.accountRepository.observeActive()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var type by remember(editing) { mutableStateOf(editing?.type ?: CardType.CREDIT) }
    var network by remember(editing) { mutableStateOf(editing?.network ?: CardNetwork.VISA) }
    var issuer by remember(editing) { mutableStateOf(editing?.issuer.orEmpty()) }
    var last4 by remember(editing) {
        mutableStateOf(editing?.cardNumberLast4 ?: prefillLast4.orEmpty())
    }
    var creditLimitText by remember(editing) {
        mutableStateOf(editing?.creditLimit?.toPlainString() ?: "")
    }
    var statementDayText by remember(editing) {
        mutableStateOf(editing?.statementDayOfMonth?.toString() ?: "")
    }
    var dueDayText by remember(editing) {
        mutableStateOf(editing?.dueDayOfMonth?.toString() ?: "")
    }
    var linkedAccountId by remember(editing) { mutableStateOf(editing?.linkedAccountId) }
    var color by remember(editing) { mutableStateOf(editing?.color ?: PALETTE.first()) }
    var showErrors by remember { mutableStateOf(false) }

    val parsedLimit = creditLimitText.toDoubleOrNull()
    val parsedStatement = statementDayText.toIntOrNull()
    val parsedDue = dueDayText.toIntOrNull()
    val last4Valid = last4.length in 3..6 && last4.all { it.isDigit() }

    val isValid: Boolean = name.isNotBlank() && last4Valid && when (type) {
        CardType.CREDIT -> (parsedLimit != null && parsedLimit > 0.0) &&
            parsedStatement in 1..31 && parsedDue in 1..31
        // DEBIT cards must point at a real account — that's how transactions
        // hit the account balance. Previously this validated as `true` so a
        // user could save a DEBIT card with no linkedAccountId, which then
        // floated outside the account ledger.
        CardType.DEBIT -> linkedAccountId != null
        CardType.PREPAID -> true
    }

    val typeOptions = listOf(
        PillOption(CardType.CREDIT, stringResource(R.string.card_form_type_credit)),
        PillOption(CardType.DEBIT, stringResource(R.string.card_form_type_debit)),
        PillOption(CardType.PREPAID, stringResource(R.string.card_form_type_prepaid)),
    )
    val networkOptions = listOf(
        PillOption(CardNetwork.VISA, stringResource(R.string.card_form_network_visa)),
        PillOption(CardNetwork.MASTERCARD, stringResource(R.string.card_form_network_mastercard)),
        PillOption(CardNetwork.RUPAY, stringResource(R.string.card_form_network_rupay)),
        PillOption(CardNetwork.AMEX, stringResource(R.string.card_form_network_amex)),
        PillOption(CardNetwork.DINERS, stringResource(R.string.card_form_network_diners)),
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
                    if (editing == null) R.string.card_form_add_title else R.string.card_form_edit_title,
                ),
            )

            FieldRow(label = stringResource(R.string.card_form_name_label)) {
                ArthaTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "HDFC Regalia",
                    isError = showErrors && name.isBlank(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
            }

            FieldRow(label = stringResource(R.string.card_form_type_label)) {
                PillRadio(value = type, options = typeOptions, onChange = { type = it })
            }
            FieldRow(label = stringResource(R.string.card_form_network_label)) {
                PillRadio(value = network, options = networkOptions, onChange = { network = it })
            }
            FieldRow(
                label = stringResource(R.string.card_form_issuer_label),
                optional = true,
            ) {
                ArthaTextField(
                    value = issuer,
                    onValueChange = { issuer = it },
                    placeholder = "HDFC Bank",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
            }
            FieldRow(
                label = stringResource(R.string.card_form_last4_label),
            ) {
                ArthaTextField(
                    value = last4,
                    onValueChange = { v -> last4 = v.filter { it.isDigit() }.take(6) },
                    placeholder = "8842",
                    isError = showErrors && !last4Valid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                )
            }

            if (type == CardType.CREDIT) {
                FieldRow(label = stringResource(R.string.card_form_credit_limit_label)) {
                    ArthaTextField(
                        value = creditLimitText,
                        onValueChange = { v ->
                            creditLimitText = v.filterIndexed { i, c ->
                                c.isDigit() || (c == '.' && v.indexOf('.') == i)
                            }
                        },
                        placeholder = "300000",
                        suffix = "₹",
                        isError = showErrors && (parsedLimit == null || parsedLimit <= 0.0),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                        FieldRow(label = stringResource(R.string.card_form_statement_day_label)) {
                            ArthaTextField(
                                value = statementDayText,
                                onValueChange = { v -> statementDayText = v.filter { it.isDigit() }.take(2) },
                                placeholder = "15",
                                isError = showErrors && parsedStatement !in 1..31,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next,
                                ),
                            )
                        }
                    }
                    androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                        FieldRow(label = stringResource(R.string.card_form_due_day_label)) {
                            ArthaTextField(
                                value = dueDayText,
                                onValueChange = { v -> dueDayText = v.filter { it.isDigit() }.take(2) },
                                placeholder = "3",
                                isError = showErrors && parsedDue !in 1..31,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done,
                                ),
                            )
                        }
                    }
                }
            }

            if (type == CardType.DEBIT) {
                FieldRow(label = stringResource(R.string.card_form_linked_account_label)) {
                    if (accounts.isEmpty()) {
                        Text(
                            text = stringResource(R.string.card_form_linked_account_none),
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = Text3,
                        )
                    } else {
                        val acctOptions = accounts.map { PillOption<String?>(it.id, it.name) }
                        PillRadio(
                            value = linkedAccountId,
                            options = acctOptions,
                            onChange = { linkedAccountId = it },
                        )
                    }
                }
            }

            FieldRow(label = stringResource(R.string.card_form_color_label)) {
                ColorSwatchRow(value = color, swatches = PALETTE, onChange = { color = it })
            }

            Spacer(Modifier.height(28.dp))
            SavePrimaryButton(
                label = stringResource(R.string.card_form_save),
                onClick = {
                    if (!isValid) {
                        showErrors = true
                        return@SavePrimaryButton
                    }
                    val now = System.currentTimeMillis()
                    val resolved = Card(
                        id = editing?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        type = type,
                        issuer = issuer.trim().takeIf { it.isNotBlank() },
                        network = network,
                        cardNumberLast4 = last4.takeIf { it.length in 3..6 },
                        creditLimit = parsedLimit?.takeIf { type == CardType.CREDIT },
                        statementDayOfMonth = parsedStatement?.takeIf { type == CardType.CREDIT },
                        dueDayOfMonth = parsedDue?.takeIf { type == CardType.CREDIT },
                        linkedAccountId = linkedAccountId?.takeIf { type == CardType.DEBIT },
                        icon = "credit_card",
                        color = color,
                        isArchived = editing?.isArchived ?: false,
                        displayOrder = editing?.displayOrder ?: nextDisplayOrder(),
                        createdAt = editing?.createdAt ?: now,
                    )
                    scope.launch {
                        app.cardRepository.upsert(resolved)
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
    0xFF1F2937L, // matte slate
    0xFF0F766EL, // acc-teal
    0xFF5260A8L, // acc-indigo
    0xFFB14A6EL, // acc-magenta
    0xFFC97A2AL, // acc-saffron
    0xFF7D5BB8L, // acc-violet
)
