package com.subramanya.artha.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.domain.model.PendingSmsTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val app = context.applicationContext as ArthaApplication
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val sender = messages.first().originatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val receivedAt = messages.first().timestampMillis

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!app.settingsPreferences.smsAutoImportEnabled.first()) return@launch

                val parsed = BankSmsParser.parse(sender, body, receivedAt) ?: return@launch

                val rules = app.transactionRuleRepository.observeActive().first()
                val people = app.personRepository.observeAll().first()
                val ruleResult = suggestCategoryFor(parsed, rules, people)

                app.pendingTransactionRepository.insert(
                    PendingSmsTransaction(
                        id = UUID.randomUUID().toString(),
                        rawSmsBody = body,
                        sender = sender,
                        receivedAt = receivedAt,
                        direction = parsed.direction,
                        amount = parsed.amount,
                        accountHint = parsed.accountHint,
                        merchant = parsed.merchant,
                        suggestedCategoryId = ruleResult.transaction.categoryId,
                    ),
                )

                val count = app.pendingTransactionRepository.observeCount().first()
                PendingTransactionNotifier.update(context, count)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
