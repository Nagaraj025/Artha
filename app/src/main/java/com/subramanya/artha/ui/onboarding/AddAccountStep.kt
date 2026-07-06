package com.subramanya.artha.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.AccountType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddAccountStep(
    draft: AccountDraft,
    pendingCount: Int,
    onNameChanged: (String) -> Unit,
    onTypeChanged: (AccountType) -> Unit,
    onInstitutionChanged: (String) -> Unit,
    onOpeningBalanceChanged: (String) -> Unit,
    onAddAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_account_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.onboarding_account_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        OutlinedTextField(
            value = draft.name,
            onValueChange = onNameChanged,
            singleLine = true,
            label = { Text(stringResource(R.string.onboarding_account_name_label)) },
            placeholder = { Text(stringResource(R.string.onboarding_account_name_placeholder)) },
            isError = draft.name.isBlank() && (draft.openingBalanceText.isNotBlank() || draft.institution.isNotBlank()),
            supportingText = {
                if (draft.name.isBlank() && (draft.openingBalanceText.isNotBlank() || draft.institution.isNotBlank())) {
                    Text(stringResource(R.string.onboarding_account_validation_name))
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_account_type_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountType.entries.forEach { type ->
                FilterChip(
                    selected = draft.type == type,
                    onClick = { onTypeChanged(type) },
                    label = { Text(type.displayLabel()) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = draft.institution,
            onValueChange = onInstitutionChanged,
            singleLine = true,
            label = { Text(stringResource(R.string.onboarding_account_institution_label)) },
            placeholder = { Text(stringResource(R.string.onboarding_account_institution_placeholder)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = draft.openingBalanceText,
            onValueChange = onOpeningBalanceChanged,
            singleLine = true,
            label = { Text(stringResource(R.string.onboarding_account_opening_label)) },
            prefix = { Text("₹") },
            isError = draft.openingBalanceText.isNotBlank() && draft.parsedBalance == null,
            supportingText = {
                if (draft.openingBalanceText.isNotBlank() && draft.parsedBalance == null) {
                    Text(stringResource(R.string.onboarding_account_validation_balance))
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onAddAnother,
            enabled = draft.isValid,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboarding_account_add_another))
        }

        if (pendingCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            val plural = pendingCount != 1
            val label = if (plural) {
                stringResource(R.string.onboarding_account_pending_count_plural, pendingCount)
            } else {
                stringResource(R.string.onboarding_account_pending_count, pendingCount)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccountType.displayLabel(): String = when (this) {
    AccountType.SAVINGS -> stringResource(R.string.onboarding_account_type_savings)
    AccountType.CURRENT -> stringResource(R.string.onboarding_account_type_current)
    AccountType.CASH -> stringResource(R.string.onboarding_account_type_cash)
    AccountType.WALLET -> stringResource(R.string.onboarding_account_type_wallet)
}
