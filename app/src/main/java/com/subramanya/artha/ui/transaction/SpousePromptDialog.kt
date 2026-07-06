package com.subramanya.artha.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.subramanya.artha.R
import com.subramanya.artha.data.preferences.SpouseTransactionDefault
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.Surface3
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.IndianNumberFormat

/**
 * Implementation of PRD §7.5.1. Interrupts save when the user adds an expense with a
 * person tagged as `SPOUSE` and the persisted default is `ASK`. Two radio options
 * (Transfer / Expense) plus the matching "Don't ask again" checkboxes — checking one
 * persists that choice as the new permanent default before saving.
 *
 * Mutually-exclusive checkboxes mirror PRD copy exactly: ticking Transfer disables
 * Expense and vice-versa.
 */
@Composable
fun SpousePromptDialog(
    amount: Double,
    personName: String,
    onCancel: () -> Unit,
    onSave: (choice: SpouseChoice, persistDefault: SpouseTransactionDefault?) -> Unit,
) {
    var choice: SpouseChoice by remember { mutableStateOf(SpouseChoice.TRANSFER) }
    var dontAskTransfer by remember { mutableStateOf(false) }
    var dontAskExpense by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Surface3,
        titleContentColor = Text1,
        textContentColor = Text2,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(
                    R.string.spouse_prompt_title,
                    IndianNumberFormat.format(amount),
                    personName,
                ),
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.spouse_prompt_question),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))

                ChoiceRow(
                    selected = choice == SpouseChoice.TRANSFER,
                    onSelect = { choice = SpouseChoice.TRANSFER },
                    title = stringResource(R.string.spouse_prompt_transfer_title),
                    body = stringResource(R.string.spouse_prompt_transfer_body),
                )
                Spacer(Modifier.height(8.dp))
                ChoiceRow(
                    selected = choice == SpouseChoice.EXPENSE,
                    onSelect = { choice = SpouseChoice.EXPENSE },
                    title = stringResource(R.string.spouse_prompt_expense_title),
                    body = stringResource(R.string.spouse_prompt_expense_body),
                )

                Spacer(Modifier.height(16.dp))
                CheckboxRow(
                    checked = dontAskTransfer,
                    onChange = { v ->
                        dontAskTransfer = v
                        if (v) dontAskExpense = false
                    },
                    label = stringResource(R.string.spouse_prompt_dont_ask_transfer),
                )
                CheckboxRow(
                    checked = dontAskExpense,
                    onChange = { v ->
                        dontAskExpense = v
                        if (v) dontAskTransfer = false
                    },
                    label = stringResource(R.string.spouse_prompt_dont_ask_expense),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val persist = when {
                    dontAskTransfer -> SpouseTransactionDefault.TRANSFER
                    dontAskExpense -> SpouseTransactionDefault.EXPENSE
                    else -> null
                }
                onSave(choice, persist)
            }) { Text(stringResource(R.string.spouse_prompt_save), color = Teal300) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.spouse_prompt_cancel), color = Text2)
            }
        },
    )
}

enum class SpouseChoice { TRANSFER, EXPENSE }

@Composable
private fun ChoiceRow(selected: Boolean, onSelect: () -> Unit, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = Teal500,
                unselectedColor = Line1,
            ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = Text1)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = Text3,
            )
        }
    }
}

@Composable
private fun CheckboxRow(checked: Boolean, onChange: (Boolean) -> Unit, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = checked, onClick = { onChange(!checked) }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Teal500,
                uncheckedColor = Line1,
            ),
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Text2, modifier = Modifier.weight(1f))
    }
}
