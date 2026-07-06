package com.subramanya.artha.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.AccountWithBalance
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.utils.IndianNumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(modifier: Modifier = Modifier, onOpenAccount: (String) -> Unit = {}) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: AccountsViewModel = viewModel(
        factory = AccountsViewModelFactory(app.accountRepository),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val toastMsg by vm.toastMessage.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(toastMsg) {
        toastMsg?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            vm.consumeToast()
        }
    }
    var overflowOpen by remember { mutableStateOf(false) }

    /** Null = sheet closed. The Account value or sentinel decides Add vs Edit mode. */
    var formMode: FormMode? by remember { mutableStateOf(null) }

    /** Non-null when a delete confirmation dialog is pending for this account. */
    var pendingDelete: Account? by remember { mutableStateOf(null) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
                AccountsEditorialHeader(
                    view = state.view,
                    reorderMode = state.isReorderMode,
                    overflowOpen = overflowOpen,
                    onOverflowToggle = { overflowOpen = it },
                    onShowArchived = vm::showArchived,
                    onShowActive = vm::showActive,
                    onExitReorder = vm::exitReorderMode,
                )

                if (state.view == AccountsView.ACTIVE && state.activeAccounts.isNotEmpty()) {
                    com.subramanya.artha.ui.accounts.TotalLiquidCard(
                        accounts = state.activeAccounts,
                    )
                }

                val rows = state.shownRows
                if (rows.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.AccountBalance,
                            title = stringResource(
                                if (state.view == AccountsView.ACTIVE) {
                                    R.string.accounts_empty_active
                                } else {
                                    R.string.accounts_empty_archived
                                },
                            ),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 4.dp),
                    ) {
                        items(rows, key = { it.account.id }) { row ->
                            if (state.view == AccountsView.ACTIVE) {
                                ActiveAccountRow(
                                    row = row,
                                    reorderMode = state.isReorderMode,
                                    canMoveUp = rows.first() != row,
                                    canMoveDown = rows.last() != row,
                                    onTap = {
                                        if (!state.isReorderMode) onOpenAccount(row.account.id)
                                    },
                                    onLongPress = vm::enterReorderMode,
                                    onMoveUp = { vm.moveUp(row.account) },
                                    onMoveDown = { vm.moveDown(row.account) },
                                    onEdit = { formMode = FormMode.Edit(row.account) },
                                    onArchive = { vm.archive(row.account) },
                                    onDelete = { pendingDelete = row.account },
                                )
                            } else {
                                ArchivedAccountRow(
                                    row = row,
                                    onRestore = { vm.restore(row.account) },
                                    onDelete = { pendingDelete = row.account },
                                )
                            }
                        }
                        item { androidx.compose.foundation.layout.Spacer(Modifier.height(100.dp)) }
                    }
                }
            }

            if (state.view == AccountsView.ACTIVE && !state.isReorderMode) {
                // HANDOFF §2 — Extended FAB anchored 20dp above the bottom nav.
                // (Was bottom=110.dp — the JSX prototype value, which is the
                // distance from the *phone shell* bottom. In Compose Scaffold
                // already insets the body past the nav, so 110dp made the FAB
                // float ~80dp above where it belongs.)
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = { formMode = FormMode.Add },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    containerColor = com.subramanya.artha.ui.theme.Teal700,
                    contentColor = com.subramanya.artha.ui.theme.Text1,
                    icon = {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    },
                    text = {
                        Text(stringResource(R.string.accounts_fab_add))
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 20.dp),
                )
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        AccountFormSheet(
            editing = (mode as? FormMode.Edit)?.account,
            onDismiss = { formMode = null },
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = stringResource(R.string.account_delete_confirm_title),
            text = stringResource(R.string.account_delete_confirm_body),
            confirmLabel = stringResource(R.string.account_delete_confirm_yes),
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
    data class Edit(val account: Account) : FormMode
}

// ---------------- rows ----------------

/**
 * Polished tile for an active account. Replaces the bare Material ListItem with a
 * Surface2 rounded card so the row reads as deliberate UI rather than a system list.
 * The Surface owns the click/long-press so the entire tile is the tap target.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ActiveAccountRow(
    row: AccountWithBalance,
    reorderMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
        color = com.subramanya.artha.ui.theme.Surface2,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.subramanya.artha.ui.theme.Line1),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountAvatar(color = row.account.color)
            androidx.compose.foundation.layout.Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.account.name,
                    color = com.subramanya.artha.ui.theme.Text1,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                )
                val subtitle = formatSubtitle(row.account)
                if (subtitle != null) {
                    androidx.compose.foundation.layout.Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = com.subramanya.artha.ui.theme.Text3,
                        maxLines = 1,
                    )
                }
                if (row.account.accountNumberLast4.isNullOrBlank()) {
                    androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.account_add_last_digits_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onEdit),
                    )
                }
            }

            if (reorderMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                        Icon(
                            Icons.Filled.ArrowUpward,
                            contentDescription = stringResource(R.string.accounts_action_move_up),
                        )
                    }
                    IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                        Icon(
                            Icons.Filled.ArrowDownward,
                            contentDescription = stringResource(R.string.accounts_action_move_down),
                        )
                    }
                }
            } else {
                Text(
                    text = IndianNumberFormat.format(row.currentBalance),
                    color = com.subramanya.artha.ui.theme.Text1,
                    style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = com.subramanya.artha.ui.theme.Text3,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.account_detail_action_edit)) },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.account_detail_action_archive)) },
                            onClick = {
                                menuOpen = false
                                onArchive()
                            },
                            leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.account_action_delete),
                                    color = com.subramanya.artha.ui.theme.Danger,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = com.subramanya.artha.ui.theme.Danger,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedAccountRow(row: AccountWithBalance, onRestore: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = com.subramanya.artha.ui.theme.Surface2,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.subramanya.artha.ui.theme.Line1),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.alpha(0.7f)) { AccountAvatar(color = row.account.color) }
            androidx.compose.foundation.layout.Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.account.name,
                    color = com.subramanya.artha.ui.theme.Text2,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
                val subtitle = formatSubtitle(row.account)
                if (subtitle != null) {
                    androidx.compose.foundation.layout.Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = com.subramanya.artha.ui.theme.Text3,
                        maxLines = 1,
                    )
                }
            }
            TextButton(onClick = onRestore) {
                Icon(Icons.Filled.Unarchive, contentDescription = null)
                Text(
                    text = stringResource(R.string.accounts_action_restore),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = null,
                        tint = com.subramanya.artha.ui.theme.Text3,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.account_action_delete),
                                color = com.subramanya.artha.ui.theme.Danger,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = com.subramanya.artha.ui.theme.Danger,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountAvatar(color: Long) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(color)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.AccountBalance,
            contentDescription = null,
            tint = Color.White,
        )
    }
}

private fun formatSubtitle(account: Account): String? {
    val pieces = buildList {
        if (!account.institution.isNullOrBlank()) add(account.institution!!)
        if (!account.accountNumberLast4.isNullOrBlank()) add("••${account.accountNumberLast4}")
    }
    return pieces.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

// ───────────────────────────── Editorial header + Hero ───────────────────────

@Composable
private fun AccountsEditorialHeader(
    view: AccountsView,
    reorderMode: Boolean,
    overflowOpen: Boolean,
    onOverflowToggle: (Boolean) -> Unit,
    onShowArchived: () -> Unit,
    onShowActive: () -> Unit,
    onExitReorder: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "WHERE YOUR MONEY SITS",
                style = com.subramanya.artha.ui.theme.EyebrowStyle,
                color = com.subramanya.artha.ui.theme.Text3,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    reorderMode -> stringResource(R.string.accounts_reorder_hint)
                    view == AccountsView.ARCHIVED -> stringResource(R.string.accounts_section_archived)
                    else -> stringResource(R.string.accounts_title)
                },
                fontFamily = com.subramanya.artha.ui.theme.InstrumentSerif,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                fontSize = androidx.compose.ui.unit.TextUnit(26f, androidx.compose.ui.unit.TextUnitType.Sp),
                lineHeight = androidx.compose.ui.unit.TextUnit(30f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (reorderMode) {
            TextButton(onClick = onExitReorder) {
                Icon(Icons.Filled.Done, contentDescription = null)
                Text(
                    text = stringResource(R.string.accounts_reorder_done),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        } else {
            Box {
                IconButton(onClick = { onOverflowToggle(true) }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = overflowOpen, onDismissRequest = { onOverflowToggle(false) }) {
                    if (view == AccountsView.ACTIVE) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.accounts_menu_show_archived)) },
                            onClick = {
                                onShowArchived()
                                onOverflowToggle(false)
                            },
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.accounts_menu_back_active)) },
                            onClick = {
                                onShowActive()
                                onOverflowToggle(false)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TotalLiquidCard(accounts: List<com.subramanya.artha.domain.model.AccountWithBalance>) {
    val total = accounts.sumOf { it.currentBalance }
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .background(com.subramanya.artha.ui.theme.Surface2)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            ),
    ) {
        com.subramanya.artha.ui.common.BandhaniOverlay(
            modifier = Modifier
                .matchParentSize()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp)),
            tint = com.subramanya.artha.ui.theme.Teal300,
            alpha = 0.04f,
        )
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "TOTAL LIQUID",
                style = com.subramanya.artha.ui.theme.EyebrowStyle,
                color = com.subramanya.artha.ui.theme.Text3,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
            com.subramanya.artha.ui.common.AutoShrinkAmountText(
                text = IndianNumberFormat.format(total),
                style = ArthaAmountStyles.hero.copy(
                    fontSize = androidx.compose.ui.unit.TextUnit(40f, androidx.compose.ui.unit.TextUnitType.Sp),
                    lineHeight = androidx.compose.ui.unit.TextUnit(44f, androidx.compose.ui.unit.TextUnitType.Sp),
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
            Text(
                text = "across ${accounts.size} account${if (accounts.size != 1) "s" else ""}",
                color = com.subramanya.artha.ui.theme.Text3,
                fontFamily = com.subramanya.artha.ui.theme.IbmPlexMono,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = com.subramanya.artha.ui.theme.IbmPlexMono,
                    fontFeatureSettings = "tnum",
                ),
            )
        }
    }
    androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
}
