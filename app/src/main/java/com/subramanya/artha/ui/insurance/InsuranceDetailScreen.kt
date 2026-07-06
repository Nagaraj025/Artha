package com.subramanya.artha.ui.insurance

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.subramanya.artha.data.entity.enums.PremiumFrequency
import com.subramanya.artha.domain.model.Insurance
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsuranceDetailScreen(
    insuranceId: String,
    onBack: () -> Unit,
    onOpenInvestment: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: InsuranceDetailViewModel = viewModel(
        factory = InsuranceDetailViewModelFactory(
            insuranceId = insuranceId,
            insuranceRepository = app.insuranceRepository,
            investmentRepository = app.investmentRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var editing: Insurance? by remember { mutableStateOf(null) }

    Surface(
        color = com.subramanya.artha.ui.theme.Surface1,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val ins = state.insurance
            com.subramanya.artha.ui.common.InlineTopBar(
                title = ins?.name.orEmpty(),
                onBack = onBack,
                trailing = {
                    if (ins != null) {
                        IconButton(onClick = { editing = ins }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.account_detail_action_edit),
                                tint = com.subramanya.artha.ui.theme.Text2,
                            )
                        }
                        if (ins.isArchived) {
                            IconButton(onClick = { vm.restore(onRestored = onBack) }) {
                                Icon(
                                    Icons.Filled.Unarchive,
                                    contentDescription = stringResource(R.string.account_detail_action_restore),
                                    tint = com.subramanya.artha.ui.theme.Text2,
                                )
                            }
                        } else {
                            IconButton(onClick = { vm.archive(onArchived = onBack) }) {
                                Icon(
                                    Icons.Filled.Archive,
                                    contentDescription = stringResource(R.string.account_detail_action_archive),
                                    tint = com.subramanya.artha.ui.theme.Text2,
                                )
                            }
                        }
                        IconButton(onClick = vm::requestDelete) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.account_action_delete),
                                tint = com.subramanya.artha.ui.theme.Danger,
                            )
                        }
                    }
                },
            )
            if (ins == null) {
                com.subramanya.artha.ui.common.LoadingPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    HeroBlock(insurance = ins)
                    MetaBlock(insurance = ins)

                    state.linkedInvestment?.let { inv ->
                        Spacer(Modifier.height(8.dp))
                        LinkedInvestmentCard(name = inv.name, onClick = { onOpenInvestment(inv.id) })
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    val editingNow = editing
    if (editingNow != null) {
        InsuranceFormSheet(editing = editingNow, onDismiss = { editing = null })
    }

    if (state.showDeleteConfirm) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = vm::dismissDeleteConfirm,
            title = stringResource(R.string.insurance_delete_confirm_title),
            text = stringResource(R.string.insurance_delete_confirm_body),
            confirmLabel = stringResource(R.string.insurance_delete_confirm_yes),
            confirmDestructive = true,
            onConfirm = { vm.confirmDelete(onDeleted = onBack) },
            cancelLabel = stringResource(R.string.common_cancel),
            onCancel = vm::dismissDeleteConfirm,
        )
    }
}

// ---------------- pieces ----------------

@Composable
private fun HeroBlock(insurance: Insurance) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(insurance.color)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(
                        text = insurance.type.displayName(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = insurance.provider,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.insurance_detail_sum_assured_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = IndianNumberFormat.format(insurance.sumAssured),
                style = ArthaAmountStyles.display,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Text(
                    text = stringResource(R.string.insurance_detail_premium_label) + ": " +
                        IndianNumberFormat.format(insurance.premiumAmount) + " / " +
                        insurance.premiumFrequency.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun MetaBlock(insurance: Insurance) {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
        insurance.policyNumber?.takeIf { it.isNotBlank() }?.let {
            MetaRow(stringResource(R.string.insurance_detail_policy_number), it)
        }
        MetaRow(stringResource(R.string.insurance_detail_start), DateFormatter.longDate(insurance.startDate))
        insurance.nextPremiumDate?.let {
            MetaRow(stringResource(R.string.insurance_detail_next_due), DateFormatter.longDate(it))
        }
        insurance.endDate?.let {
            MetaRow(stringResource(R.string.insurance_detail_end), DateFormatter.longDate(it))
        }
        insurance.nominee?.takeIf { it.isNotBlank() }?.let {
            MetaRow(stringResource(R.string.insurance_detail_nominee), it)
        }
        insurance.taxSection?.takeIf { it.isNotBlank() }?.let {
            MetaRow(stringResource(R.string.insurance_detail_tax_section), it)
        }
        insurance.agentContact?.takeIf { it.isNotBlank() }?.let { contact ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.insurance_detail_agent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(contact, style = MaterialTheme.typography.bodyMedium)
                    val phoneLike = contact.filter { it.isDigit() || it == '+' }
                    if (phoneLike.length >= 7) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$phoneLike")
                            }
                            runCatching { context.startActivity(intent) }
                        }) {
                            Icon(Icons.Filled.Call, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LinkedInvestmentCard(name: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Savings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.insurance_detail_linked_investment_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun PremiumFrequency.displayName(): String = when (this) {
    PremiumFrequency.MONTHLY -> stringResource(R.string.premium_frequency_monthly)
    PremiumFrequency.QUARTERLY -> stringResource(R.string.premium_frequency_quarterly)
    PremiumFrequency.HALF_YEARLY -> stringResource(R.string.premium_frequency_half_yearly)
    PremiumFrequency.YEARLY -> stringResource(R.string.premium_frequency_yearly)
    PremiumFrequency.SINGLE -> stringResource(R.string.premium_frequency_single)
}
