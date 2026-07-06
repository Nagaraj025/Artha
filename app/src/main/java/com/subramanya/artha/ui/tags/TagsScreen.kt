package com.subramanya.artha.ui.tags

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.domain.model.Tag
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Text1
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: TagsViewModel = viewModel(factory = TagsViewModelFactory(app.tagRepository))
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var formMode: TagFormMode? by remember { mutableStateOf(null) }
    var pendingDelete: Tag? by remember { mutableStateOf(null) }
    var blockedMessage: String? by remember { mutableStateOf(null) }

    LaunchedEffect(blockedMessage) {
        blockedMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            blockedMessage = null
        }
    }

    Surface(color = Surface1, modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Surface1,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { formMode = TagFormMode.Add },
                    containerColor = Teal700,
                    contentColor = Text1,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.tags_fab_add)) },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                com.subramanya.artha.ui.common.InlineTopBar(
                    title = stringResource(R.string.tags_title),
                    onBack = onBack,
                )
                if (state.tags.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.Sell,
                            title = stringResource(R.string.tags_empty),
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.tags, key = { it.id }) { tag ->
                            TagRow(
                                tag = tag,
                                onEdit = { formMode = TagFormMode.Edit(tag) },
                                onDelete = {
                                    scope.launch {
                                        val count = vm.usageCount(tag.id)
                                        if (count > 0) {
                                            blockedMessage = context.getString(R.string.tags_in_use_toast, count)
                                        } else {
                                            pendingDelete = tag
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

    val mode = formMode
    if (mode != null) {
        TagFormSheet(
            editing = (mode as? TagFormMode.Edit)?.tag,
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
            title = stringResource(R.string.tags_delete_confirm_title),
            text = stringResource(R.string.tags_delete_confirm_body),
            confirmLabel = stringResource(R.string.tags_delete_confirm_yes),
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

private sealed interface TagFormMode {
    data object Add : TagFormMode
    data class Edit(val tag: Tag) : TagFormMode
}

@Composable
private fun TagRow(tag: Tag, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(tag.color)),
            ) { }
        },
        headlineContent = { Text(tag.name) },
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.tags_action_edit))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tags_action_edit)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.tags_action_delete),
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
        },
    )
}
