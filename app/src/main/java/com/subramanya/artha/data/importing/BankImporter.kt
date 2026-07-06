package com.subramanya.artha.data.importing

import android.content.Context
import com.subramanya.artha.data.db.AppDatabase
import com.subramanya.artha.data.entity.AccountEntity
import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.entity.enums.AccountType
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.util.UUID

/**
 * Loads the bundled `assets/seed/bank_import.json` (generated offline by
 * `docs/Bank statement/import_to_artha.py --export-json`) and writes the
 * 3,913 categorised transactions + their two source accounts into Room.
 *
 * Idempotent — transaction IDs in the JSON are UUID5 hashes of
 * (bank, date, amount, normalised description). Re-running the importer
 * with the same asset is a no-op rather than a duplicate set, because
 * the bulk insert uses [OnConflictStrategy.IGNORE].
 *
 * The "Wipe imported bank data" tile in Settings cleans up by deleting
 * every transaction whose notes start with `Imported from ` plus the two
 * named accounts — so the round-trip (import → wipe → re-import) works.
 */
class BankImporter(private val context: Context, private val database: AppDatabase) {
    sealed interface Result {
        /** Asset bundled by the Python script not present in the APK build. */
        data object AssetMissing : Result

        /** Bundled JSON couldn't be parsed — should never happen in a release. */
        data class ParseError(val message: String) : Result

        /** Successful import — counts surfaced to the user via toast. */
        data class Success(
            val accountsCreated: Int,
            val accountsReused: Int,
            val transactionsInserted: Int,
            val transactionsSkipped: Int,
        ) : Result
    }

    suspend fun importBundled(): Result {
        val raw = runCatching {
            context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        }.getOrElse { return Result.AssetMissing }

        val root = runCatching { JSONObject(raw) }
            .getOrElse { return Result.ParseError(it.message ?: "invalid json") }

        val accountsJson = root.getJSONArray("accounts")
        val txnsJson = root.getJSONArray("transactions")

        // ---- accounts: reuse-by-name, else create ----
        val nameToId = mutableMapOf<String, String>()
        val acctDao = database.accountDao()
        val existing = acctDao.observeAll().first().associateBy { it.name }
        var accountsCreated = 0
        var accountsReused = 0
        val now = System.currentTimeMillis()
        for (i in 0 until accountsJson.length()) {
            val a = accountsJson.getJSONObject(i)
            val name = a.getString("name")
            val match = existing[name]
            if (match != null) {
                nameToId[name] = match.id
                accountsReused++
            } else {
                val id = UUID.randomUUID().toString()
                acctDao.upsert(
                    AccountEntity(
                        id = id,
                        name = name,
                        type = AccountType.valueOf(a.optString("type", "SAVINGS")),
                        institution = a.optString("institution").takeIf { it.isNotBlank() },
                        accountNumberLast4 = null,
                        openingBalance = a.optDouble("opening_balance", 0.0),
                        currency = "INR",
                        icon = a.optString("icon", "account_balance"),
                        color = a.getLong("color"),
                        isArchived = false,
                        displayOrder = (now / 1000).toInt(),
                        createdAt = now,
                    ),
                )
                nameToId[name] = id
                accountsCreated++
            }
        }

        // ---- transactions: bulk-insert with IGNORE so re-runs are no-ops ----
        val txnDao = database.transactionDao()
        val toInsert = ArrayList<TransactionEntity>(txnsJson.length())
        for (i in 0 until txnsJson.length()) {
            val t = txnsJson.getJSONObject(i)
            val sourceName = t.getString("source_account_name")
            val sourceId = nameToId[sourceName] ?: continue
            val destName = t.optString("destination_account_name").takeIf { it.isNotBlank() && it != "null" }
            val destId = destName?.let { nameToId[it] }
            toInsert.add(
                TransactionEntity(
                    id = t.getString("id"),
                    type = TransactionType.valueOf(t.getString("type")),
                    amount = t.getDouble("amount"),
                    currency = t.optString("currency", "INR"),
                    date = t.getLong("date_ms"),
                    description = t.optString("description"),
                    categoryId = t.optString("category_id").takeIf { it.isNotBlank() && it != "null" },
                    subCategoryId = t.optString("sub_category_id").takeIf { it.isNotBlank() && it != "null" },
                    sourceType = SourceKind.ACCOUNT,
                    sourceId = sourceId,
                    destinationType = if (destId != null) SourceKind.ACCOUNT else null,
                    destinationId = destId,
                    paymentApp = runCatching { PaymentApp.valueOf(t.optString("payment_app", "OTHER")) }
                        .getOrDefault(PaymentApp.OTHER),
                    place = null,
                    latitude = null,
                    longitude = null,
                    receiptUri = null,
                    notes = t.optString("notes").takeIf { it.isNotBlank() },
                    taxSection = null,
                    recurringRuleId = null,
                    isSplit = false,
                    splitGroupId = null,
                    source = TransactionSource.MANUAL,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }

        // Chunk to keep the SQLite statement size sane (default cursor window ~2 MB).
        // 500 rows × ~25 cols stays well under the limit and is still ~8x faster than
        // single inserts on a Pixel-class device.
        var inserted = 0
        var skipped = 0
        for (chunk in toInsert.chunked(500)) {
            val rowIds = txnDao.insertAllIgnore(chunk)
            // Room returns -1 for rows that were ignored.
            val added = rowIds.count { it != -1L }
            inserted += added
            skipped += chunk.size - added
        }

        return Result.Success(
            accountsCreated = accountsCreated,
            accountsReused = accountsReused,
            transactionsInserted = inserted,
            transactionsSkipped = skipped,
        )
    }

    private companion object {
        const val ASSET_PATH = "seed/bank_import.json"
    }
}
