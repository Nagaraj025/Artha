package com.subramanya.artha.ui.categories

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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import com.subramanya.artha.data.entity.enums.CategoryType
import com.subramanya.artha.domain.model.Category
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
import com.subramanya.artha.ui.theme.Text3
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * HANDOFF sheets-extra.jsx · AddCategorySheet — Add/Edit Category.
 * System categories keep their fixed `type` (the chip row stays disabled);
 * name and color remain editable per PRD §7.12.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormSheet(
    editing: Category?,
    defaultType: CategoryType,
    parentCandidates: List<Category>,
    onSave: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var type by remember(editing) { mutableStateOf(editing?.type ?: defaultType) }
    var parentId by remember(editing) { mutableStateOf(editing?.parentId) }
    var icon by remember(editing) { mutableStateOf(editing?.icon ?: ICONS.first().key) }
    var color by remember(editing) { mutableStateOf(editing?.color ?: PALETTE.first()) }
    var showErrors by remember { mutableStateOf(false) }

    // User-configurable colour palette (Phase 1): built-ins + the user's saved custom swatches.
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext
        as com.subramanya.artha.ArthaApplication
    val pickScope = androidx.compose.runtime.rememberCoroutineScope()
    val customColours by app.settingsPreferences.customColours
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var pickingColour by remember { mutableStateOf(false) }
    val customIcons by app.settingsPreferences.customIcons
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var pickingIcon by remember { mutableStateOf(false) }
    val iconChoices = ICONS + customIcons.map {
        IconChoice(it, com.subramanya.artha.utils.MaterialIcons.resolve(it))
    }

    val isSystem = editing?.isSystem == true
    val isValid = name.isNotBlank()

    val typeOptions = CategoryType.entries.map { t ->
        PillOption(t, t.name.lowercase().replaceFirstChar { it.titlecase() })
    }

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
                    if (editing == null) R.string.category_form_add_title else R.string.category_form_edit_title,
                ),
                sub = if (isSystem) stringResource(R.string.category_form_system_locked_hint) else null,
            )

            FieldRow(label = stringResource(R.string.category_form_name_label)) {
                ArthaTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Coffee shops",
                    isError = showErrors && name.isBlank(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
            }

            if (!isSystem) {
                FieldRow(label = stringResource(R.string.category_form_type_label)) {
                    PillRadio(
                        value = type,
                        options = typeOptions,
                        onChange = { type = it },
                    )
                }
            }

            val availableParents = parentCandidates.filter {
                it.type == type && it.parentId == null && it.id != editing?.id
            }
            if (availableParents.isNotEmpty()) {
                val parentOptions = listOf(
                    PillOption<String?>(null, stringResource(R.string.category_form_parent_none)),
                ) + availableParents.map { PillOption<String?>(it.id, it.name) }
                FieldRow(
                    label = stringResource(R.string.category_form_parent_label),
                    optional = true,
                ) {
                    PillRadio(
                        value = parentId,
                        options = parentOptions,
                        onChange = { parentId = it },
                    )
                }
            }

            FieldRow(label = stringResource(R.string.category_form_icon_label)) {
                IconChipRow(
                    value = icon,
                    icons = iconChoices,
                    onChange = { icon = it },
                    onAdd = { pickingIcon = true },
                )
            }
            FieldRow(label = stringResource(R.string.category_form_color_label)) {
                ColorSwatchRow(
                    value = color,
                    swatches = PALETTE + customColours,
                    onChange = { color = it },
                    onAdd = { pickingColour = true },
                )
            }

            Spacer(Modifier.height(28.dp))
            SavePrimaryButton(
                label = stringResource(R.string.category_form_save),
                onClick = {
                    if (!isValid) {
                        showErrors = true
                        return@SavePrimaryButton
                    }
                    val displayOrder = editing?.displayOrder ?: nextDisplayOrder()
                    onSave(
                        Category(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            parentId = parentId,
                            type = if (isSystem) (editing?.type ?: type) else type,
                            icon = icon,
                            color = color,
                            isSystem = editing?.isSystem ?: false,
                            displayOrder = displayOrder,
                        ),
                    )
                },
            )
            Spacer(Modifier.height(20.dp))

            @Suppress("UNUSED_EXPRESSION")
            Text3
        }
    }

    if (pickingColour) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickingColour = false },
            title = { Text(stringResource(R.string.picklist_add_colour_title)) },
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
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (pickingIcon) {
        val already = iconChoices.map { it.key }.toSet()
        val catalogue = com.subramanya.artha.utils.MaterialIcons.keys
            .filter { it !in already }
            .map { IconChoice(it, com.subramanya.artha.utils.MaterialIcons.resolve(it)) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickingIcon = false },
            title = { Text(stringResource(R.string.picklist_add_icon_title)) },
            text = {
                IconChipRow(
                    value = "",
                    icons = catalogue,
                    onChange = { picked ->
                        pickScope.launch { app.settingsPreferences.addCustomIcon(picked) }
                        icon = picked
                        pickingIcon = false
                    },
                )
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pickingIcon = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

private fun nextDisplayOrder(): Int = (System.currentTimeMillis() / 1000L).toInt()

/** Extra colours offered when the user taps "+" to add a custom swatch. */
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

private val ICONS: List<IconChoice> = listOf(
    IconChoice("category", Icons.Filled.Category),
    IconChoice("shopping_bag", Icons.Filled.ShoppingBag),
    IconChoice("restaurant", Icons.Filled.Restaurant),
    IconChoice("directions_car", Icons.Filled.DirectionsCar),
    IconChoice("receipt_long", Icons.Filled.ReceiptLong),
    IconChoice("medical_services", Icons.Filled.LocalHospital),
    IconChoice("celebration", Icons.Filled.Celebration),
    IconChoice("school", Icons.Filled.School),
    IconChoice("home", Icons.Filled.Home),
)
