package com.subramanya.artha.ui.categories

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.CategoryType
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: CategoriesViewModel = viewModel(factory = CategoriesViewModelFactory(app.categoryRepository))
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var formMode: FormMode? by remember { mutableStateOf(null) }
    var pendingDelete: Category? by remember { mutableStateOf(null) }

    /** A pending toast for "blocked delete" cases (in-use or system). */
    var blockedDeleteMessage: String? by remember { mutableStateOf(null) }

    LaunchedEffect(blockedDeleteMessage) {
        blockedDeleteMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            blockedDeleteMessage = null
        }
    }

    Surface(color = Surface1, modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Surface1,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { formMode = FormMode.Add },
                    containerColor = Teal700,
                    contentColor = Text1,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.categories_fab_add)) },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                com.subramanya.artha.ui.common.InlineTopBar(
                    title = stringResource(R.string.categories_title),
                    onBack = onBack,
                )
                TypeTabs(current = state.type, onSelect = vm::onTypeSelected)
                if (state.parents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.Category,
                            title = stringResource(R.string.categories_empty),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 4.dp, bottom = 100.dp),
                    ) {
                        state.parents.forEach { parent ->
                            val children = state.childrenByParent[parent.id].orEmpty()
                            val expanded = parent.id in state.expandedParentIds
                            item(key = "p-${parent.id}") {
                                ParentRow(
                                    parent = parent,
                                    hasChildren = children.isNotEmpty(),
                                    childCount = children.size,
                                    expanded = expanded,
                                    onToggle = { vm.toggleExpanded(parent.id) },
                                    onEdit = { formMode = FormMode.Edit(parent) },
                                    onDelete = {
                                        scope.launch {
                                            when {
                                                parent.isSystem ->
                                                    blockedDeleteMessage =
                                                        context.getString(R.string.categories_system_delete_toast)
                                                vm.usageCount(parent.id) > 0 ->
                                                    blockedDeleteMessage =
                                                        context.getString(R.string.categories_in_use_toast, vm.usageCount(parent.id))
                                                else -> pendingDelete = parent
                                            }
                                        }
                                    },
                                )
                            }
                            if (expanded) {
                                items(children, key = { "c-${it.id}" }) { child ->
                                    ChildRow(
                                        child = child,
                                        onEdit = { formMode = FormMode.Edit(child) },
                                        onDelete = {
                                            scope.launch {
                                                when {
                                                    child.isSystem ->
                                                        blockedDeleteMessage =
                                                            context.getString(R.string.categories_system_delete_toast)
                                                    vm.usageCount(child.id) > 0 ->
                                                        blockedDeleteMessage =
                                                            context.getString(R.string.categories_in_use_toast, vm.usageCount(child.id))
                                                    else -> pendingDelete = child
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        CategoryFormSheet(
            editing = (mode as? FormMode.Edit)?.category,
            defaultType = state.type,
            parentCandidates = state.parents,
            onSave = { saved ->
                vm.upsert(saved)
                formMode = null
            },
            onDismiss = { formMode = null },
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = stringResource(R.string.categories_delete_confirm_title),
            text = stringResource(R.string.categories_delete_confirm_body),
            confirmLabel = stringResource(R.string.categories_delete_confirm_yes),
            confirmDestructive = true,
            onConfirm = {
                vm.delete(toDelete)
                pendingDelete = null
            },
            cancelLabel = stringResource(R.string.common_cancel),
            onCancel = { pendingDelete = null },
        )
    }
}

private sealed interface FormMode {
    data object Add : FormMode
    data class Edit(val category: Category) : FormMode
}

// ---------------- type tabs ----------------

@Composable
private fun TypeTabs(current: CategoryType, onSelect: (CategoryType) -> Unit) {
    // horizontalScroll instead of a plain Row so the four chips never wrap onto
    // a second line on narrow phones — same fix as Transactions filter chips.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TypeChip(current, CategoryType.EXPENSE, R.string.categories_filter_expense, onSelect)
        TypeChip(current, CategoryType.INCOME, R.string.categories_filter_income, onSelect)
        TypeChip(current, CategoryType.TRANSFER, R.string.categories_filter_transfer, onSelect)
        TypeChip(current, CategoryType.INVESTMENT, R.string.categories_filter_investment, onSelect)
    }
}

@Composable
private fun TypeChip(current: CategoryType, target: CategoryType, labelRes: Int, onSelect: (CategoryType) -> Unit) {
    FilterChip(
        selected = current == target,
        onClick = { onSelect(target) },
        label = { Text(stringResource(labelRes)) },
    )
}

// ---------------- rows ----------------

@Composable
private fun ParentRow(
    parent: Category,
    hasChildren: Boolean,
    childCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (hasChildren) it.clickable(onClick = onToggle) else it },
        color = com.subramanya.artha.ui.theme.Surface2,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.subramanya.artha.ui.theme.Line1),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryAvatar(color = parent.color, icon = parent.icon)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = parent.name,
                    color = Text1,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                )
                val parts = buildList {
                    if (parent.isSystem) add(stringResource(R.string.categories_system_chip))
                    if (childCount > 0) {
                        add(pluralCount(childCount, R.string.categories_subcount_one, R.string.categories_subcount_many))
                    }
                }
                if (parts.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = parts.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = com.subramanya.artha.ui.theme.Text3,
                        maxLines = 1,
                    )
                }
            }
            if (hasChildren) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = com.subramanya.artha.ui.theme.Text3,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.categories_action_edit),
                        tint = com.subramanya.artha.ui.theme.Text3,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.categories_action_edit)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.categories_action_delete),
                                color = com.subramanya.artha.ui.theme.Danger,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = com.subramanya.artha.ui.theme.Danger)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChildRow(child: Category, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    // Children are indented 24dp and use Surface3 to step down a layer from the parent
    // tile — same hierarchy idea as the Reports / Cards detail screens.
    Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = com.subramanya.artha.ui.theme.Surface3,
            shape = RoundedCornerShape(14.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryAvatar(color = child.color, icon = child.icon, small = true)
                Spacer(Modifier.size(12.dp))
                Text(
                    text = child.name,
                    color = Text2,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.categories_action_edit),
                            tint = com.subramanya.artha.ui.theme.Text3,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.categories_action_edit)) },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.categories_action_delete),
                                    color = com.subramanya.artha.ui.theme.Danger,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = com.subramanya.artha.ui.theme.Danger)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun pluralCount(count: Int, oneRes: Int, manyRes: Int): String =
    if (count == 1) stringResource(oneRes) else stringResource(manyRes, count)

@Composable
private fun CategoryAvatar(color: Long, icon: String? = null, small: Boolean = false) {
    val dim = if (small) 28.dp else 36.dp
    Box(
        modifier = Modifier
            .size(dim)
            .clip(CircleShape)
            .background(Color(color)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = com.subramanya.artha.utils.MaterialIcons.resolve(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(if (small) 16.dp else 20.dp),
        )
    }
}
