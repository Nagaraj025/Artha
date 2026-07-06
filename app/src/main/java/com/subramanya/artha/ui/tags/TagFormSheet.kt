package com.subramanya.artha.ui.tags

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subramanya.artha.R
import com.subramanya.artha.domain.model.Tag
import com.subramanya.artha.ui.common.ArthaSheetHandle
import com.subramanya.artha.ui.common.ArthaTextField
import com.subramanya.artha.ui.common.ColorSwatchRow
import com.subramanya.artha.ui.common.FieldRow
import com.subramanya.artha.ui.common.SavePrimaryButton
import com.subramanya.artha.ui.common.SheetTitle
import com.subramanya.artha.ui.common.SheetWindowInsets
import com.subramanya.artha.ui.theme.Surface3
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * HANDOFF sheets-extra.jsx · AddTagSheet — minimal Add/Edit Tag sheet:
 * SheetTitle("New tag") + FieldRow("Tag name") + FieldRow("Color") + Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagFormSheet(editing: Tag?, onSave: (Tag) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var color by remember(editing) { mutableStateOf(editing?.color ?: PALETTE.first()) }
    var showErrors by remember { mutableStateOf(false) }

    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext
        as com.subramanya.artha.ArthaApplication
    val pickScope = androidx.compose.runtime.rememberCoroutineScope()
    val customColours by app.settingsPreferences.customColours
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var pickingColour by remember { mutableStateOf(false) }

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
                    if (editing == null) R.string.tag_form_add_title else R.string.tag_form_edit_title,
                ),
            )

            FieldRow(label = stringResource(R.string.tag_form_name_label)) {
                ArthaTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "coffee",
                    isError = showErrors && name.isBlank(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                )
            }

            FieldRow(label = stringResource(R.string.tag_form_color_label)) {
                ColorSwatchRow(
                    value = color,
                    swatches = PALETTE + customColours,
                    onChange = { color = it },
                    onAdd = { pickingColour = true },
                )
            }

            Spacer(Modifier.height(28.dp))
            SavePrimaryButton(
                label = stringResource(R.string.tag_form_save),
                onClick = {
                    if (name.isBlank()) {
                        showErrors = true
                        return@SavePrimaryButton
                    }
                    onSave(
                        Tag(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            color = color,
                        ),
                    )
                },
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    if (pickingColour) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickingColour = false },
            title = { androidx.compose.material3.Text(stringResource(R.string.picklist_add_colour_title)) },
            text = {
                ColorSwatchRow(
                    value = 0L,
                    swatches = EXTRA_COLOURS.filter { it !in PALETTE && it !in customColours },
                    onChange = { picked ->
                        pickScope.launch { app.settingsPreferences.addCustomColour(picked) }
                        color = picked
                        pickingColour = false
                    },
                )
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pickingColour = false }) {
                    androidx.compose.material3.Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

private val EXTRA_COLOURS: List<Long> = listOf(
    0xFF2563EBL, 0xFFDC2626L, 0xFFEA580CL, 0xFFCA8A04L,
    0xFF16A34AL, 0xFF0891B2L, 0xFF7C3AEDL, 0xFFDB2777L,
    0xFF4B5563L, 0xFF65A30DL, 0xFF0D9488L, 0xFF9333EAL,
)

private val PALETTE: List<Long> = listOf(
    0xFF0F766EL, // acc-teal
    0xFF5260A8L, // acc-indigo
    0xFF2F8F6BL, // acc-emerald
    0xFFC97A2AL, // acc-saffron
    0xFFB14A6EL, // acc-magenta
    0xFF7D5BB8L, // acc-violet
)
