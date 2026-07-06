package com.subramanya.artha.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.PersonRelation
import com.subramanya.artha.domain.model.Person

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PersonPicker(
    people: List<Person>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onAddPerson: (name: String, relation: PersonRelation) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.txn_people_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            people.forEach { person ->
                FilterChip(
                    selected = person.id in selectedIds,
                    onClick = { onToggle(person.id) },
                    label = { Text(person.name) },
                )
            }
            AssistChip(
                onClick = { showDialog = true },
                label = { Text(stringResource(R.string.txn_people_add)) },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
            )
        }
    }

    if (showDialog) {
        AddPersonDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name, relation ->
                onAddPerson(name, relation)
                showDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddPersonDialog(onDismiss: () -> Unit, onConfirm: (String, PersonRelation) -> Unit) {
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf(PersonRelation.FRIEND) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.person_picker_add_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.person_picker_name_label)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.person_picker_relation_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PersonRelation.entries.forEach { rel ->
                        FilterChip(
                            selected = rel == relation,
                            onClick = { relation = rel },
                            label = { Text(rel.displayName()) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, relation) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun PersonRelation.displayName(): String = when (this) {
    PersonRelation.SPOUSE -> stringResource(R.string.person_relation_spouse)
    PersonRelation.PARENT -> stringResource(R.string.person_relation_parent)
    PersonRelation.SIBLING -> stringResource(R.string.person_relation_sibling)
    PersonRelation.CHILD -> stringResource(R.string.person_relation_child)
    PersonRelation.FRIEND -> stringResource(R.string.person_relation_friend)
    PersonRelation.COLLEAGUE -> stringResource(R.string.person_relation_colleague)
    PersonRelation.BUSINESS -> stringResource(R.string.person_relation_business)
    PersonRelation.OTHER -> stringResource(R.string.person_relation_other)
}
