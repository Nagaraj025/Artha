package com.subramanya.artha.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.domain.model.SmsDirection
import com.subramanya.artha.ui.accounts.AccountFormSheet
import com.subramanya.artha.ui.cards.CardFormSheet
import com.subramanya.artha.ui.theme.Expense
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.transaction.AddTransactionSheet
import com.subramanya.artha.ui.transaction.AddTransactionViewModel
import com.subramanya.artha.ui.transaction.AddTransactionViewModelFactory
import com.subramanya.artha.utils.IndianNumberFormat

/**
 * Review tab: the list of SMS-detected transactions awaiting user action. Tapping a card
 * opens the existing Add Transaction sheet pre-filled from the SMS (mirrors how AI Quick
 * Entry pre-fills the same sheet on Dashboard); swiping a card away dismisses it directly
 * without ever opening the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: ReviewViewModel = viewModel(
        factory = ReviewViewModelFactory(
            app.pendingTransactionRepository,
            app.categoryRepository,
            app.accountRepository,
            app.cardRepository,
        ),
    )
    val state by vm.state.collectAsState()

    var showSheet by remember { mutableStateOf(false) }
    var pendingPrefill: ReviewItem? by remember { mutableStateOf(null) }
    var chooserForHint: String? by remember { mutableStateOf(null) }
    var addAccountLast4: String? by remember { mutableStateOf(null) }
    var addCardLast4: String? by remember { mutableStateOf(null) }

    if (showSheet) {
        val txnVm: AddTransactionViewModel = viewModel(
            factory = AddTransactionViewModelFactory(
                accountRepository = app.accountRepository,
                cardRepository = app.cardRepository,
                categoryRepository = app.categoryRepository,
                personRepository = app.personRepository,
                tagRepository = app.tagRepository,
                transactionRepository = app.transactionRepository,
                transactionRuleRepository = app.transactionRuleRepository,
                investmentRepository = app.investmentRepository,
                settingsPreferences = app.settingsPreferences,
            ),
        )
        pendingPrefill?.let { item ->
            LaunchedEffect(item) {
                txnVm.applyPendingSmsPrefill(item.pending, item.suggestedCategoryName, item.matchedFunds)
            }
        }

        // AddTransactionSheet's own internal LaunchedEffect(state.savedAndClose) resets the VM's
        // state back to defaults (flipping savedAndClose back to false) as part of closing the
        // sheet — racing against that from here by watching the same boolean is fragile. Instead
        // collect the VM's explicit one-shot `saveCompleted` signal, which is independent of
        // that reset and of onDismiss's timing: it fires exactly once per genuine save, so the
        // pending row is removed only then and never on a plain sheet dismiss.
        LaunchedEffect(txnVm) {
            txnVm.saveCompleted.collect {
                pendingPrefill?.let { item -> vm.dismiss(item.pending.id) }
            }
        }

        AddTransactionSheet(
            viewModel = txnVm,
            onDismiss = {
                showSheet = false
                pendingPrefill = null
            },
        )
    }

    chooserForHint?.let { hint ->
        AddAccountKindChooser(
            onChosen = { kind ->
                when (kind) {
                    AddAccountKind.ACCOUNT -> addAccountLast4 = hint
                    AddAccountKind.CARD -> addCardLast4 = hint
                }
                chooserForHint = null
            },
            onDismiss = { chooserForHint = null },
        )
    }

    addAccountLast4?.let { last4 ->
        AccountFormSheet(
            editing = null,
            onDismiss = { addAccountLast4 = null },
            prefillLast4 = last4,
        )
    }

    addCardLast4?.let { last4 ->
        CardFormSheet(
            editing = null,
            onDismiss = { addCardLast4 = null },
            prefillLast4 = last4,
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_review)) }) },
    ) { innerPadding ->
        if (state.items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.review_empty_state))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.items, key = { it.pending.id }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart ||
                                value == SwipeToDismissBoxValue.StartToEnd
                            ) {
                                vm.dismiss(item.pending.id)
                                true
                            } else {
                                false
                            }
                        },
                    )
                    SwipeToDismissBox(state = dismissState, backgroundContent = {}) {
                        Card(
                            modifier = Modifier.fillMaxSize().padding(vertical = 2.dp),
                            onClick = {
                                pendingPrefill = item
                                showSheet = true
                            },
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = IndianNumberFormat.format(item.pending.amount),
                                    color = if (item.pending.direction == SmsDirection.DEBIT) Expense else Income,
                                )
                                Text(text = item.pending.merchant ?: item.pending.sender)
                                item.suggestedCategoryName?.let { Text(text = it) }
                                if (item.hasUnmatchedHint) {
                                    val hint = item.pending.accountHint.orEmpty()
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.review_unmatched_account, hint),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { chooserForHint = hint },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
