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
                // Best-effort background parse: any unexpected failure (bad DataStore read,
                // Room insert failure, unexpected null) should just mean "no pending row created
                // this time", not an uncaught exception crashing the app process in the
                // background. runCatching swallows it; pendingResult.finish() still always runs.
                runCatching {
                    if (!app.settingsPreferences.smsAutoImportEnabled.first()) return@runCatching

                    val parsed = BankSmsParser.parse(sender, body, receivedAt) ?: return@runCatching

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
                    // Notification update is handled process-wide by ArthaApplication's
                    // observeCount() collector (see ArthaApplication.onCreate) — no need to
                    // update it here too, that would just double-update on every SMS.
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
