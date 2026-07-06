package com.subramanya.artha.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
        factory = ReviewViewModelFactory(app.pendingTransactionRepository, app.categoryRepository),
    )
    val state by vm.state.collectAsState()

    var showSheet by remember { mutableStateOf(false) }
    var pendingPrefill: ReviewItem? by remember { mutableStateOf(null) }

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
                txnVm.applyPendingSmsPrefill(item.pending, item.suggestedCategoryName)
            }
        }

        // NOTE: AddTransactionSheet's own internal LaunchedEffect(state.savedAndClose) calls
        // viewModel.acknowledgeClose() (which resets the VM's state back to defaults, flipping
        // savedAndClose back to false) BEFORE it invokes onDismiss(). So checking
        // `txnVm.state.value.savedAndClose` from inside onDismiss would always read false —
        // the reset already happened. Instead we collect the state here ourselves and react to
        // the true -> ... transition directly, independent of when/whether onDismiss fires, so
        // the pending row is removed only on a genuine save and never on a plain sheet dismiss.
        val txnState by txnVm.state.collectAsState()
        LaunchedEffect(txnState.savedAndClose) {
            if (txnState.savedAndClose) {
                pendingPrefill?.let { vm.dismiss(it.pending.id) }
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
                            }
                        }
                    }
                }
            }
        }
    }
}
