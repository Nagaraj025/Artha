# SMS Auto-Detect → Pending Transaction Review Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** detect bank debit/credit SMS locally, turn them into draft transactions that surface in a new "Review" bottom-nav tab (backed by an ongoing notification) until the user confirms or dismisses them.

**Architecture:** a manifest-registered `BroadcastReceiver` parses incoming SMS with a local regex heuristic (no network), runs a provisional `Transaction` through the existing `RuleEngine` for a suggested category, and persists a `PendingSmsTransactionEntity` row. A new bottom-nav "Review" tab lists pending rows; tapping one opens the existing Add Transaction sheet pre-filled (mirroring the AI Quick Entry prefill mechanism), and a successful save deletes the pending row. An ongoing notification tracks the pending count and deep-links into the Review tab.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose (Material3 BOM 2024.10.01), Room 2.6.1 (KSP), manual constructor injection (no DI framework), `kotlinx-datetime`.

**Full design:** `docs/plans/2026-07-03-sms-auto-detect-design.md` (read first — this plan implements every decision there verbatim).

## Global Constraints

- Kotlin 2.0.21, target JVM 17; minSdk 26, targetSdk 34, compileSdk 35 (`app/build.gradle.kts`).
- No DI framework (Hilt/Koin) — manual constructor injection only; every repository is a `by lazy` `val` on `ArthaApplication`.
- No `!!` operator anywhere. No `var` in data classes.
- All user-facing strings go in `app/src/main/res/values/strings.xml` — no hardcoded strings in Composables.
- All INR amounts render via `utils/IndianNumberFormat.kt` (`₹1,00,000` — never `Rs.`, never `100,000`).
- One ViewModel per screen, no SharedViewModel patterns.
- Room 2.6.1 migrations require a `MigrationTestHelper`-based instrumented test (`app/src/androidTest`), mirroring the existing `InvestmentMigrationTest.kt`.
- Run `./gradlew assembleDebug` after every task and confirm `BUILD SUCCESSFUL` before moving on; run `./gradlew spotlessApply` before the final commit.
- Detection is a generic keyword/regex heuristic (no per-bank catalogue), new-SMS-only (`RECEIVE_SMS`, no `READ_SMS` backfill) — see design doc §Decisions 1–2.
- Pending SMS drafts are intentionally excluded from the existing JSON backup/restore — they are ephemeral review items, not ledger data. Do not touch `BackupDao`/`BackupCodecTest`.

---

## Task 1: Room schema — entity, migration, DAO, migration test

**Files:**
- Create: `app/src/main/java/com/subramanya/artha/data/entity/PendingSmsTransactionEntity.kt`
- Create: `app/src/main/java/com/subramanya/artha/data/dao/PendingTransactionDao.kt`
- Modify: `app/src/main/java/com/subramanya/artha/data/db/Migrations.kt`
- Modify: `app/src/main/java/com/subramanya/artha/data/db/AppDatabase.kt`
- Modify: `app/src/main/java/com/subramanya/artha/data/db/DatabaseProvider.kt`
- Test: `app/src/androidTest/java/com/subramanya/artha/data/db/PendingSmsTransactionMigrationTest.kt`

**Interfaces:**
- Produces: `PendingSmsTransactionEntity(id, rawSmsBody, sender, receivedAt, direction, amount, accountHint, merchant, suggestedCategoryId)`; `PendingTransactionDao.observeAll(): Flow<List<PendingSmsTransactionEntity>>`, `.observeCount(): Flow<Int>`, `.getById(id: String): PendingSmsTransactionEntity?`, `.insert(entity)`, `.deleteById(id: String)`; `AppDatabase.pendingTransactionDao(): PendingTransactionDao`. Task 2 consumes all of these.

- [ ] **Step 1: Create the entity**

```kotlin
package com.subramanya.artha.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sms_transactions")
data class PendingSmsTransactionEntity(
    @PrimaryKey
    val id: String,
    val rawSmsBody: String,
    val sender: String,
    val receivedAt: Long,
    /** Stored as the enum name ("DEBIT"/"CREDIT") — see [com.subramanya.artha.domain.model.SmsDirection]. */
    val direction: String,
    val amount: Double,
    val accountHint: String?,
    val merchant: String?,
    val suggestedCategoryId: String?,
)
```

- [ ] **Step 2: Create the DAO**

```kotlin
package com.subramanya.artha.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.subramanya.artha.data.entity.PendingSmsTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {
    @Query("SELECT * FROM pending_sms_transactions ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<PendingSmsTransactionEntity>>

    @Query("SELECT COUNT(*) FROM pending_sms_transactions")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM pending_sms_transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PendingSmsTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingSmsTransactionEntity)

    @Query("DELETE FROM pending_sms_transactions WHERE id = :id")
    suspend fun deleteById(id: String)
}
```

- [ ] **Step 3: Add the migration**

Open `app/src/main/java/com/subramanya/artha/data/db/Migrations.kt` and append (after `MIGRATION_4_5`):

```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pending_sms_transactions` (
            `id` TEXT NOT NULL,
            `rawSmsBody` TEXT NOT NULL,
            `sender` TEXT NOT NULL,
            `receivedAt` INTEGER NOT NULL,
            `direction` TEXT NOT NULL,
            `amount` REAL NOT NULL,
            `accountHint` TEXT,
            `merchant` TEXT,
            `suggestedCategoryId` TEXT,
            PRIMARY KEY(`id`))
            """.trimIndent(),
        )
    }
}
```

- [ ] **Step 4: Wire the entity + migration into `AppDatabase.kt`**

In the `@Database(entities = [...])` list, add a new group after the `// Phase 4 additions` block:

```kotlin
        // Phase 6 — SMS auto-detect
        PendingSmsTransactionEntity::class,
    ],
    version = 6,
```

(replace the existing `version = 5,` line with `version = 6,`, and add `PendingSmsTransactionEntity::class,` as the last entry in the `entities` list, importing `com.subramanya.artha.data.entity.PendingSmsTransactionEntity`).

Add the DAO accessor next to `abstract fun recurringRuleDao(): RecurringRuleDao`:

```kotlin
    abstract fun pendingTransactionDao(): PendingTransactionDao
```

(import `com.subramanya.artha.data.dao.PendingTransactionDao`).

- [ ] **Step 5: Register the migration in `DatabaseProvider.kt`**

Change:
```kotlin
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
```
to:
```kotlin
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
```

- [ ] **Step 6: Build once to export the v6 schema JSON**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`, and a new `app/schemas/com.subramanya.artha.data.db.AppDatabase/6.json` file is generated (confirm with `ls app/schemas/com.subramanya.artha.data.db.AppDatabase/`).

- [ ] **Step 7: Write the migration test**

```kotlin
package com.subramanya.artha.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Verifies the v5 -> v6 migration creates the new `pending_sms_transactions` table.
 * There is no pre-existing data to preserve (brand-new table), so this only needs
 * to confirm the table exists and is writable/readable after migration.
 */
@RunWith(AndroidJUnit4::class)
class PendingSmsTransactionMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrate5To6_createsPendingSmsTransactionsTable() {
        helper.createDatabase(TEST_DB, 5).close()

        helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6).use { migratedDb ->
            migratedDb.execSQL(
                """
                INSERT INTO pending_sms_transactions
                    (id, rawSmsBody, sender, receivedAt, direction, amount, accountHint, merchant, suggestedCategoryId)
                VALUES
                    ('p1', 'Rs.500 debited', 'HDFCBK', 1700000000000, 'DEBIT', 500.0, '1234', 'Swiggy', NULL)
                """.trimIndent(),
            )
            migratedDb.query("SELECT * FROM pending_sms_transactions WHERE id = 'p1'").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("HDFCBK", cursor.getString(cursor.getColumnIndexOrThrow("sender")))
                assertEquals(500.0, cursor.getDouble(cursor.getColumnIndexOrThrow("amount")), 0.0001)
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
```

- [ ] **Step 8: Run the migration test (requires a connected device/emulator)**

Run: `./gradlew connectedDebugAndroidTest --tests "*PendingSmsTransactionMigrationTest"`
Expected: `BUILD SUCCESSFUL`, 1 test passed. **Known limitation:** this dev machine currently has no emulator/AVD or connected device set up (only command-line SDK tools) — if none is available, skip running this and rely on Step 6's successful schema export plus code review; run it before merging on a machine/CI that has a device.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/subramanya/artha/data/entity/PendingSmsTransactionEntity.kt \
        app/src/main/java/com/subramanya/artha/data/dao/PendingTransactionDao.kt \
        app/src/main/java/com/subramanya/artha/data/db/Migrations.kt \
        app/src/main/java/com/subramanya/artha/data/db/AppDatabase.kt \
        app/src/main/java/com/subramanya/artha/data/db/DatabaseProvider.kt \
        app/src/androidTest/java/com/subramanya/artha/data/db/PendingSmsTransactionMigrationTest.kt \
        app/schemas/com.subramanya.artha.data.db.AppDatabase/6.json
git commit -m "feat(sms): add pending_sms_transactions table (schema v6)"
```

---

## Task 2: Domain model, mapper, repository, `ArthaApplication` wiring

**Files:**
- Create: `app/src/main/java/com/subramanya/artha/domain/model/PendingSmsTransaction.kt`
- Create: `app/src/main/java/com/subramanya/artha/data/mapper/PendingSmsTransactionMapper.kt`
- Create: `app/src/main/java/com/subramanya/artha/data/repository/PendingTransactionRepository.kt`
- Modify: `app/src/main/java/com/subramanya/artha/ArthaApplication.kt`
- Test: `app/src/test/java/com/subramanya/artha/data/mapper/PendingSmsTransactionMapperTest.kt`

**Interfaces:**
- Consumes: `PendingSmsTransactionEntity`, `PendingTransactionDao` (Task 1).
- Produces: `enum class SmsDirection { DEBIT, CREDIT }`; `data class PendingSmsTransaction(id, rawSmsBody, sender, receivedAt, direction: SmsDirection, amount, accountHint, merchant, suggestedCategoryId)`; `PendingTransactionRepository.observeAll(): Flow<List<PendingSmsTransaction>>`, `.observeCount(): Flow<Int>`, `.insert(pending: PendingSmsTransaction)`, `.dismiss(id: String)`; `ArthaApplication.pendingTransactionRepository`. Tasks 4, 5, 7 consume these.

- [ ] **Step 1: Create the domain model**

```kotlin
package com.subramanya.artha.domain.model

enum class SmsDirection { DEBIT, CREDIT }

data class PendingSmsTransaction(
    val id: String,
    val rawSmsBody: String,
    val sender: String,
    val receivedAt: Long,
    val direction: SmsDirection,
    val amount: Double,
    val accountHint: String?,
    val merchant: String?,
    val suggestedCategoryId: String?,
)
```

- [ ] **Step 2: Write the failing mapper test**

```kotlin
package com.subramanya.artha.data.mapper

import com.subramanya.artha.data.entity.PendingSmsTransactionEntity
import com.subramanya.artha.domain.model.PendingSmsTransaction
import com.subramanya.artha.domain.model.SmsDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class PendingSmsTransactionMapperTest {

    @Test
    fun `entity round-trips through domain and back unchanged`() {
        val entity = PendingSmsTransactionEntity(
            id = "p1",
            rawSmsBody = "Rs.500 debited from A/c XX1234 at SWIGGY",
            sender = "HDFCBK",
            receivedAt = 1_700_000_000_000L,
            direction = "DEBIT",
            amount = 500.0,
            accountHint = "1234",
            merchant = "SWIGGY",
            suggestedCategoryId = null,
        )

        val domain = entity.toDomain()
        assertEquals(SmsDirection.DEBIT, domain.direction)
        assertEquals("SWIGGY", domain.merchant)

        val roundTripped = domain.toEntity()
        assertEquals(entity, roundTripped)
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*PendingSmsTransactionMapperTest"`
Expected: FAIL — `toDomain()`/`toEntity()` are unresolved references (mapper doesn't exist yet).

- [ ] **Step 4: Implement the mapper**

```kotlin
package com.subramanya.artha.data.mapper

import com.subramanya.artha.data.entity.PendingSmsTransactionEntity
import com.subramanya.artha.domain.model.PendingSmsTransaction
import com.subramanya.artha.domain.model.SmsDirection

fun PendingSmsTransactionEntity.toDomain(): PendingSmsTransaction = PendingSmsTransaction(
    id = id,
    rawSmsBody = rawSmsBody,
    sender = sender,
    receivedAt = receivedAt,
    direction = SmsDirection.valueOf(direction),
    amount = amount,
    accountHint = accountHint,
    merchant = merchant,
    suggestedCategoryId = suggestedCategoryId,
)

fun PendingSmsTransaction.toEntity(): PendingSmsTransactionEntity = PendingSmsTransactionEntity(
    id = id,
    rawSmsBody = rawSmsBody,
    sender = sender,
    receivedAt = receivedAt,
    direction = direction.name,
    amount = amount,
    accountHint = accountHint,
    merchant = merchant,
    suggestedCategoryId = suggestedCategoryId,
)
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*PendingSmsTransactionMapperTest"`
Expected: `BUILD SUCCESSFUL`, 1 test passed.

- [ ] **Step 6: Create the repository (no dedicated test — thin pass-through, same as `TagRepository`/`CategoryRepository` which also have none)**

```kotlin
package com.subramanya.artha.data.repository

import com.subramanya.artha.data.dao.PendingTransactionDao
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.PendingSmsTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PendingTransactionRepository(private val dao: PendingTransactionDao) {

    fun observeAll(): Flow<List<PendingSmsTransaction>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun insert(pending: PendingSmsTransaction) = dao.insert(pending.toEntity())

    suspend fun dismiss(id: String) = dao.deleteById(id)
}
```

- [ ] **Step 7: Wire it into `ArthaApplication.kt`**

Add, next to `recurringRuleRepository`:

```kotlin
    val pendingTransactionRepository: PendingTransactionRepository by lazy {
        PendingTransactionRepository(database.pendingTransactionDao())
    }
```

(import `com.subramanya.artha.data.repository.PendingTransactionRepository`).

- [ ] **Step 8: Build to confirm everything compiles**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/subramanya/artha/domain/model/PendingSmsTransaction.kt \
        app/src/main/java/com/subramanya/artha/data/mapper/PendingSmsTransactionMapper.kt \
        app/src/main/java/com/subramanya/artha/data/repository/PendingTransactionRepository.kt \
        app/src/main/java/com/subramanya/artha/ArthaApplication.kt \
        app/src/test/java/com/subramanya/artha/data/mapper/PendingSmsTransactionMapperTest.kt
git commit -m "feat(sms): add PendingSmsTransaction domain model, mapper, repository"
```

---

## Task 3: `BankSmsParser` — local SMS detection heuristic (TDD)

**Files:**
- Create: `app/src/main/java/com/subramanya/artha/sms/BankSmsParser.kt`
- Test: `app/src/test/java/com/subramanya/artha/sms/BankSmsParserTest.kt`

**Interfaces:**
- Consumes: `com.subramanya.artha.domain.model.SmsDirection` (Task 2).
- Produces: `data class ParsedBankSms(sender, receivedAt, direction: SmsDirection, amount: Double, accountHint: String?, merchant: String?)`; `object BankSmsParser { fun parse(sender: String, body: String, receivedAt: Long): ParsedBankSms? }`. Tasks 4 and 5 consume this.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.subramanya.artha.sms

import com.subramanya.artha.domain.model.SmsDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BankSmsParserTest {

    @Test
    fun `parses a standard debit alert`() {
        val body = "Rs.500.00 debited from A/c XX1234 on 03-07-26 at SWIGGY. Avl Bal Rs.10,000.00"
        val result = BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(SmsDirection.DEBIT, result.direction)
        assertEquals(500.0, result.amount, 0.001)
        assertEquals("1234", result.accountHint)
    }

    @Test
    fun `parses a standard credit alert`() {
        val body = "INR 25,000.00 credited to your A/c XX5678 on 03-07-26. Info: SALARY"
        val result = BankSmsParser.parse("SBIINB", body, 1_700_000_000_000L)
        requireNotNull(result)
        assertEquals(SmsDirection.CREDIT, result.direction)
        assertEquals(25000.0, result.amount, 0.001)
    }

    @Test
    fun `ignores an OTP message even if it mentions a debit`() {
        val body = "Your OTP for a debit card transaction of Rs.500 is 123456. Do not share it."
        assertNull(BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L))
    }

    @Test
    fun `ignores a promotional message`() {
        val body = "Get a cashback offer of Rs.100 on your next credit card spend! T&C apply."
        assertNull(BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L))
    }

    @Test
    fun `returns null when no amount can be parsed`() {
        val body = "Your account was debited for a transaction. Contact support for details."
        assertNull(BankSmsParser.parse("HDFCBK", body, 1_700_000_000_000L))
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "*BankSmsParserTest"`
Expected: FAIL to compile — `BankSmsParser` doesn't exist yet.

- [ ] **Step 3: Implement the parser**

```kotlin
package com.subramanya.artha.sms

import com.subramanya.artha.domain.model.SmsDirection

data class ParsedBankSms(
    val sender: String,
    val receivedAt: Long,
    val direction: SmsDirection,
    val amount: Double,
    val accountHint: String?,
    val merchant: String?,
)

/**
 * Generic keyword+regex heuristic for Indian bank debit/credit SMS — deliberately not a
 * per-bank catalogue (docs/plans/2026-07-03-sms-auto-detect-design.md, Decision 1). Broad
 * coverage over per-bank precision. Returns null for anything not confidently a bank
 * transaction alert (OTP, promo, unparseable amount).
 */
object BankSmsParser {

    private val EXCLUDE_KEYWORDS = listOf(
        "otp", "one time password", "offer", "cashback offer", "sale", "discount",
    )
    private val DEBIT_KEYWORDS = listOf("debited", "debit", "spent", "withdrawn")
    private val CREDIT_KEYWORDS = listOf("credited", "credit")
    private val AMOUNT_REGEX = Regex("""(?:Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val ACCOUNT_REGEX = Regex(
        """(?:a/c|acct|account|card)[^\d]{0,10}(?:no\.?)?\s*[xX*]*(\d{3,6})""",
        RegexOption.IGNORE_CASE,
    )
    private val MERCHANT_REGEX = Regex("""(?:at|to)\s+([A-Za-z0-9 &.'-]{3,30})""", RegexOption.IGNORE_CASE)

    fun parse(sender: String, body: String, receivedAt: Long): ParsedBankSms? {
        val lower = body.lowercase()
        if (EXCLUDE_KEYWORDS.any { lower.contains(it) }) return null

        val direction = when {
            DEBIT_KEYWORDS.any { lower.contains(it) } -> SmsDirection.DEBIT
            CREDIT_KEYWORDS.any { lower.contains(it) } -> SmsDirection.CREDIT
            else -> return null
        }

        val amountMatch = AMOUNT_REGEX.find(body) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        val accountHint = ACCOUNT_REGEX.find(body)?.groupValues?.get(1)
        val merchant = MERCHANT_REGEX.find(body)?.groupValues?.get(1)?.trim()

        return ParsedBankSms(
            sender = sender,
            receivedAt = receivedAt,
            direction = direction,
            amount = amount,
            accountHint = accountHint,
            merchant = merchant,
        )
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "*BankSmsParserTest"`
Expected: `BUILD SUCCESSFUL`, 5 tests passed. If a specific regex doesn't match as expected, adjust `AMOUNT_REGEX`/`ACCOUNT_REGEX`/`MERCHANT_REGEX` and re-run — this is expected TDD iteration, not a design change.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/subramanya/artha/sms/BankSmsParser.kt \
        app/src/test/java/com/subramanya/artha/sms/BankSmsParserTest.kt
git commit -m "feat(sms): add BankSmsParser generic debit/credit heuristic"
```

---

## Task 4: Rule-based category suggestion (TDD)

**Files:**
- Create: `app/src/main/java/com/subramanya/artha/sms/SmsRuleSuggestion.kt`
- Test: `app/src/test/java/com/subramanya/artha/sms/SmsRuleSuggestionTest.kt`

**Interfaces:**
- Consumes: `ParsedBankSms` (Task 3); `com.subramanya.artha.domain.rules.RuleEngine.apply(candidate: Transaction, rules: List<TransactionRule>, knownPeople: List<Person>, timeZone: TimeZone): RuleEngineResult` (existing).
- Produces: `fun suggestCategoryFor(parsed: ParsedBankSms, rules: List<TransactionRule>, people: List<Person>): RuleEngineResult`. Task 5 consumes this.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.subramanya.artha.sms

import com.subramanya.artha.domain.model.SmsDirection
import com.subramanya.artha.domain.model.TransactionRule
import com.subramanya.artha.domain.rules.ConditionLogic
import com.subramanya.artha.domain.rules.RuleAction
import com.subramanya.artha.domain.rules.RuleActions
import com.subramanya.artha.domain.rules.RuleCondition
import com.subramanya.artha.domain.rules.RuleConditions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsRuleSuggestionTest {

    private val swiggyRule = TransactionRule(
        id = "rule-1",
        name = "Swiggy -> Food",
        conditions = RuleConditions(
            logic = ConditionLogic.ALL,
            items = listOf(RuleCondition.DescriptionContains(text = "SWIGGY", ignoreCase = true)),
        ),
        actions = RuleActions(items = listOf(RuleAction.SetCategory(categoryId = "cat-food"))),
        priority = 0,
        isActive = true,
        isSystem = false,
        createdAt = 0L,
    )

    @Test
    fun `suggests a category when a rule matches the merchant`() {
        val parsed = ParsedBankSms(
            sender = "HDFCBK",
            receivedAt = 1_700_000_000_000L,
            direction = SmsDirection.DEBIT,
            amount = 500.0,
            accountHint = "1234",
            merchant = "SWIGGY",
        )
        val result = suggestCategoryFor(parsed, rules = listOf(swiggyRule), people = emptyList())
        assertEquals("cat-food", result.transaction.categoryId)
    }

    @Test
    fun `leaves category null when no rule matches`() {
        val parsed = ParsedBankSms(
            sender = "HDFCBK",
            receivedAt = 1_700_000_000_000L,
            direction = SmsDirection.DEBIT,
            amount = 500.0,
            accountHint = "1234",
            merchant = "UNKNOWN MERCHANT",
        )
        val result = suggestCategoryFor(parsed, rules = listOf(swiggyRule), people = emptyList())
        assertNull(result.transaction.categoryId)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "*SmsRuleSuggestionTest"`
Expected: FAIL to compile — `suggestCategoryFor` doesn't exist yet.

- [ ] **Step 3: Implement `suggestCategoryFor`**

```kotlin
package com.subramanya.artha.sms

import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Person
import com.subramanya.artha.domain.model.SmsDirection
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.domain.model.TransactionRule
import com.subramanya.artha.domain.rules.RuleEngine
import com.subramanya.artha.domain.rules.RuleEngineResult
import kotlinx.datetime.TimeZone

/**
 * Builds a throwaway [Transaction] candidate from a parsed SMS purely so [RuleEngine.apply]
 * can suggest a category — never persisted as-is. Only `result.transaction.categoryId` /
 * `.subCategoryId` are read back by the caller.
 */
fun suggestCategoryFor(
    parsed: ParsedBankSms,
    rules: List<TransactionRule>,
    people: List<Person>,
): RuleEngineResult {
    val candidate = Transaction(
        id = "sms-candidate",
        type = if (parsed.direction == SmsDirection.DEBIT) TransactionType.EXPENSE else TransactionType.INCOME,
        amount = parsed.amount,
        currency = "INR",
        date = parsed.receivedAt,
        description = parsed.merchant ?: parsed.sender,
        categoryId = null,
        subCategoryId = null,
        sourceType = SourceKind.ACCOUNT,
        sourceId = null,
        destinationType = null,
        destinationId = null,
        paymentApp = PaymentApp.OTHER,
        place = null,
        latitude = null,
        longitude = null,
        peopleIds = emptyList(),
        tagIds = emptyList(),
        receiptUri = null,
        notes = null,
        taxSection = null,
        recurringRuleId = null,
        isSplit = false,
        splitGroupId = null,
        source = TransactionSource.SMS,
        createdAt = parsed.receivedAt,
        updatedAt = parsed.receivedAt,
    )
    return RuleEngine.apply(candidate, rules, people, TimeZone.currentSystemDefault())
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "*SmsRuleSuggestionTest"`
Expected: `BUILD SUCCESSFUL`, 2 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/subramanya/artha/sms/SmsRuleSuggestion.kt \
        app/src/test/java/com/subramanya/artha/sms/SmsRuleSuggestionTest.kt
git commit -m "feat(sms): suggest a category for parsed SMS via the existing RuleEngine"
```

---

## Task 5: Manifest permissions, `SmsReceiver`, ongoing notification

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/subramanya/artha/ArthaApplication.kt`
- Create: `app/src/main/java/com/subramanya/artha/sms/SmsReceiver.kt`
- Create: `app/src/main/java/com/subramanya/artha/sms/PendingTransactionNotifier.kt`
- Create: `app/src/main/res/drawable/ic_notification.xml`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `SettingsPreferences.smsAutoImportEnabled` (existing), `BankSmsParser.parse` (Task 3), `suggestCategoryFor` (Task 4), `ArthaApplication.pendingTransactionRepository/transactionRuleRepository/personRepository` (Task 2 / existing).
- Produces: `PendingTransactionNotifier.EXTRA_OPEN_REVIEW: String` constant, `.ensureChannel(context)`, `.update(context, pendingCount: Int)`. Task 8 consumes `EXTRA_OPEN_REVIEW`.

- [ ] **Step 1: Add the notification icon**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M19,3H5C3.9,3 3,3.9 3,5v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V5C21,3.9 20.1,3 19,3zM10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8L10,17z" />
</vector>
```

- [ ] **Step 2: Add strings**

Add to `app/src/main/res/values/strings.xml`, next to the existing `settings_security_sms*` entries:

```xml
    <string name="nav_review">Review</string>
    <string name="review_empty_state">No pending transactions</string>
    <string name="review_notification_channel_name">Pending transaction review</string>
    <plurals name="review_notification_title">
        <item quantity="one">%d transaction to review</item>
        <item quantity="other">%d transactions to review</item>
    </plurals>
    <string name="settings_security_sms_permission_denied">SMS auto-import needs SMS and notification permissions to work.</string>
```

- [ ] **Step 3: Create `PendingTransactionNotifier`**

```kotlin
package com.subramanya.artha.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.subramanya.artha.MainActivity
import com.subramanya.artha.R

/** Owns the single ongoing "N transactions to review" notification — updated in place
 *  (same ID) rather than stacking one notification per detected SMS. */
object PendingTransactionNotifier {

    private const val CHANNEL_ID = "pending_sms_review"
    private const val NOTIFICATION_ID = 1001
    const val EXTRA_OPEN_REVIEW = "com.subramanya.artha.extra.OPEN_REVIEW"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.review_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun update(context: Context, pendingCount: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (pendingCount <= 0) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_REVIEW, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                context.resources.getQuantityString(R.plurals.review_notification_title, pendingCount, pendingCount),
            )
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}
```

- [ ] **Step 4: Create `SmsReceiver`**

```kotlin
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
```

- [ ] **Step 5: Call `ensureChannel` on app start**

Add an `onCreate()` override to `ArthaApplication.kt` (there is none today):

```kotlin
    override fun onCreate() {
        super.onCreate()
        com.subramanya.artha.sms.PendingTransactionNotifier.ensureChannel(this)
    }
```

- [ ] **Step 6: Add permissions + receiver to the manifest**

In `app/src/main/AndroidManifest.xml`, add before `<application ...>`:

```xml
    <uses-permission android:name="android.permission.RECEIVE_SMS" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

```

and inside `<application>`, after the `</activity>` closing tag and before the `<!-- FileProvider -->` comment:

```xml
        <receiver
            android:name=".sms.SmsReceiver"
            android:exported="true"
            android:permission="android.permission.BROADCAST_SMS">
            <intent-filter>
                <action android:name="android.provider.Telephony.SMS_RECEIVED" />
            </intent-filter>
        </receiver>

```

- [ ] **Step 7: Build to confirm everything compiles**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Manual verification (requires a device/emulator — none is set up on this dev machine yet)**

There is no automated test for `SmsReceiver`/`PendingTransactionNotifier` (Android framework `BroadcastReceiver`/`NotificationManager`, no Robolectric in this project — see design doc "Testing" section; the parsing/rule logic they call is already fully covered by Tasks 3–4). To verify manually once a device or AVD emulator is available and the SMS toggle is on (Task 6):

```bash
adb emu sms send HDFCBK "Rs.500 debited from A/c XX1234 at SWIGGY. Avl Bal Rs 9500"
```

Expected: an ongoing "1 transaction to review" notification appears, and a row appears in the Review tab (Task 7) once built.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/AndroidManifest.xml \
        app/src/main/java/com/subramanya/artha/ArthaApplication.kt \
        app/src/main/java/com/subramanya/artha/sms/SmsReceiver.kt \
        app/src/main/java/com/subramanya/artha/sms/PendingTransactionNotifier.kt \
        app/src/main/res/drawable/ic_notification.xml \
        app/src/main/res/values/strings.xml
git commit -m "feat(sms): add SmsReceiver, ongoing review notification, manifest wiring"
```

---

## Task 6: Settings permission-request flow

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/subramanya/artha/MainActivity.kt`

**Interfaces:**
- Consumes: `SettingsViewModel.onSmsAutoImportChanged(Boolean)` (existing), `SettingsPreferences.smsAutoImportEnabled/setSmsAutoImportEnabled` (existing).
- Produces: nothing new consumed by later tasks (leaf feature).

- [ ] **Step 1: Gate the Settings toggle behind a runtime permission request**

In `SettingsScreen.kt`, add these imports if not already present:

```kotlin
import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
```

Above the `SecuritySection(...)` call site (around the existing lines 160-180), add:

```kotlin
val context = LocalContext.current
val smsPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
) { grants ->
    val smsGranted = grants[Manifest.permission.RECEIVE_SMS] == true
    val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        grants[Manifest.permission.POST_NOTIFICATIONS] == true
    } else {
        true
    }
    if (smsGranted && notifGranted) {
        vm.onSmsAutoImportChanged(true)
    } else {
        Toast.makeText(context, R.string.settings_security_sms_permission_denied, Toast.LENGTH_LONG).show()
    }
}
```

Then change the `onSmsImportChanged` argument passed into `SecuritySection(...)` from `vm::onSmsAutoImportChanged` to:

```kotlin
                    onSmsImportChanged = { enabled ->
                        if (enabled) {
                            val permissions = buildList {
                                add(Manifest.permission.RECEIVE_SMS)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            smsPermissionLauncher.launch(permissions.toTypedArray())
                        } else {
                            vm.onSmsAutoImportChanged(false)
                        }
                    },
```

- [ ] **Step 2: Detect revoked permission on app resume**

In `MainActivity.kt`, add these imports:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
```

In `ArthaRoot()`, after the existing `collectAsState` lines, add:

```kotlin
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    val enabled = app.settingsPreferences.smsAutoImportEnabled.first()
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECEIVE_SMS,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (enabled && !granted) {
                        app.settingsPreferences.setSmsAutoImportEnabled(false)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
```

(`kotlinx.coroutines.flow.first` is very likely already imported in this file for the startup logic — add it if not.)

- [ ] **Step 3: Build to confirm everything compiles**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual verification**

Install the debug build, open Settings → toggle "SMS auto-import" on → confirm a system permission dialog appears for SMS (and notifications on API 33+). Deny it → confirm the toggle stays off and a toast appears. Grant it → confirm the toggle turns on. Then, in system Settings → Apps → Artha → Permissions, revoke SMS manually, return to the app → confirm the Settings toggle flips back off.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/subramanya/artha/ui/settings/SettingsScreen.kt \
        app/src/main/java/com/subramanya/artha/MainActivity.kt
git commit -m "feat(sms): request RECEIVE_SMS/POST_NOTIFICATIONS on toggle, detect revocation"
```

---

## Task 7: Review tab — nav destination, badge, screen, Add Transaction prefill

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/ui/navigation/ArthaDestinations.kt`
- Modify: `app/src/main/java/com/subramanya/artha/ui/common/ArthaBottomBar.kt`
- Modify: `app/src/main/java/com/subramanya/artha/ui/navigation/ArthaNavHost.kt`
- Modify: `app/src/main/java/com/subramanya/artha/MainActivity.kt`
- Modify: `app/src/main/java/com/subramanya/artha/ui/transaction/AddTransactionViewModel.kt`
- Create: `app/src/main/java/com/subramanya/artha/ui/review/ReviewViewModel.kt`
- Create: `app/src/main/java/com/subramanya/artha/ui/review/ReviewScreen.kt`

**Interfaces:**
- Consumes: `ArthaApplication.pendingTransactionRepository/categoryRepository` (existing/Task 2), `AddTransactionViewModelFactory` (existing), `AddTransactionSheet` (existing).
- Produces: `ArthaDestination.Review`; `ReviewScreen()` composable, registered at route `ArthaDestination.Review.route`. Task 8 navigates to this route.

- [ ] **Step 1: Add the `Review` destination**

In `ArthaDestinations.kt`, insert `Review` between `Cards` and `More`:

```kotlin
    Cards(route = "cards", labelRes = R.string.nav_cards, icon = Icons.Filled.CreditCard),
    Review(route = "review", labelRes = R.string.nav_review, icon = Icons.Filled.PendingActions),
    More(route = "more", labelRes = R.string.nav_more, icon = Icons.Filled.MoreHoriz),
```

(add `import androidx.compose.material.icons.filled.PendingActions` if not already imported).

- [ ] **Step 2: Add a badge to the bottom bar**

In `ArthaBottomBar.kt`, add a `reviewBadgeCount: Int = 0` parameter to `ArthaBottomBar` and thread a `badgeCount` through to `BottomTabItem`:

```kotlin
@Composable
fun ArthaBottomBar(
    currentDestination: ArthaDestination?,
    onItemSelected: (ArthaDestination) -> Unit,
    modifier: Modifier = Modifier,
    reviewBadgeCount: Int = 0,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface0.copy(alpha = 0.92f))
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = Line1, thickness = Dp.Hairline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArthaDestination.bottomNav.forEach { dest ->
                BottomTabItem(
                    destination = dest,
                    selected = dest == currentDestination,
                    onClick = { onItemSelected(dest) },
                    badgeCount = if (dest == ArthaDestination.Review) reviewBadgeCount else 0,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    destination: ArthaDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
) {
    val interaction = remember { MutableInteractionSource() }
    val pillBg = if (selected) Teal900 else Color.Transparent
    val tint = if (selected) Teal300 else Text2

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(pillBg),
            contentAlignment = Alignment.Center,
        ) {
            if (badgeCount > 0) {
                androidx.compose.material3.BadgedBox(
                    badge = {
                        androidx.compose.material3.Badge {
                            Text(text = if (badgeCount > 99) "99+" else badgeCount.toString())
                        }
                    },
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes),
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = stringResource(destination.labelRes),
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Text(
            text = stringResource(destination.labelRes),
            color = tint,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
```

- [ ] **Step 3: Add `applyPendingSmsPrefill` to `AddTransactionViewModel`**

Add next to `applyAiPrefill` (lines 126-138):

```kotlin
    fun applyPendingSmsPrefill(
        pending: com.subramanya.artha.domain.model.PendingSmsTransaction,
        suggestedCategoryName: String?,
    ) {
        _state.update { current ->
            current.copy(
                tab = if (pending.direction == com.subramanya.artha.domain.model.SmsDirection.DEBIT) {
                    TransactionTab.EXPENSE
                } else {
                    TransactionTab.INCOME
                },
                amountText = pending.amount.toString(),
                description = pending.merchant ?: pending.sender,
                dateTimeMillis = pending.receivedAt,
                categoryId = pending.suggestedCategoryId ?: current.categoryId,
                categoryDisplay = suggestedCategoryName ?: current.categoryDisplay,
            )
        }
    }
```

- [ ] **Step 4: Create `ReviewViewModel`**

```kotlin
package com.subramanya.artha.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.PendingTransactionRepository
import com.subramanya.artha.domain.model.PendingSmsTransaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReviewItem(
    val pending: PendingSmsTransaction,
    val suggestedCategoryName: String?,
)

data class ReviewUiState(val items: List<ReviewItem> = emptyList())

class ReviewViewModel(
    private val pendingTransactionRepository: PendingTransactionRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val state: StateFlow<ReviewUiState> = pendingTransactionRepository.observeAll()
        .map { pendingList ->
            ReviewUiState(
                items = pendingList.map { pending ->
                    val categoryName = pending.suggestedCategoryId
                        ?.let { categoryRepository.getById(it)?.name }
                    ReviewItem(pending, categoryName)
                },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    fun dismiss(id: String) {
        viewModelScope.launch { pendingTransactionRepository.dismiss(id) }
    }
}

class ReviewViewModelFactory(
    private val pendingTransactionRepository: PendingTransactionRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ReviewViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return ReviewViewModel(pendingTransactionRepository, categoryRepository) as T
    }
}
```

- [ ] **Step 5: Create `ReviewScreen`**

```kotlin
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
        AddTransactionSheet(
            viewModel = txnVm,
            onDismiss = {
                if (txnVm.state.value.savedAndClose) {
                    pendingPrefill?.let { vm.dismiss(it.pending.id) }
                }
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
```

- [ ] **Step 6: Register the route in `ArthaNavHost.kt`**

Add, after the `composable(ArthaDestination.Cards.route) { ... }` block:

```kotlin
        composable(ArthaDestination.Review.route) {
            ReviewScreen()
        }
```

(import `com.subramanya.artha.ui.review.ReviewScreen`).

- [ ] **Step 7: Collect the pending count in `MainActivity.kt`'s `MainApp`**

Add, next to the existing `userName` collection:

```kotlin
    val app = LocalContext.current.applicationContext as ArthaApplication
    val pendingReviewCount by app.pendingTransactionRepository.observeCount().collectAsState(initial = 0)
```

and pass it into the existing `ArthaBottomBar(...)` call:

```kotlin
            ArthaBottomBar(
                currentDestination = currentDestination,
                reviewBadgeCount = pendingReviewCount,
                onItemSelected = { destination -> ... },
            )
```

(`import com.subramanya.artha.ArthaApplication` if not already present in this file — it likely already is, since `ArthaRoot`/`ArthaInner` already use it).

- [ ] **Step 8: Build to confirm everything compiles**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Manual verification**

Insert a row directly via `adb shell` (or wait for Task 8/a real SMS) and confirm: the Review tab shows a badge with the count, tapping the tab shows the card, tapping the card opens Add Transaction pre-filled with amount/description/date/category, saving removes it from the list, and swiping a card away also removes it without opening Add Transaction.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/subramanya/artha/ui/navigation/ArthaDestinations.kt \
        app/src/main/java/com/subramanya/artha/ui/common/ArthaBottomBar.kt \
        app/src/main/java/com/subramanya/artha/ui/navigation/ArthaNavHost.kt \
        app/src/main/java/com/subramanya/artha/MainActivity.kt \
        app/src/main/java/com/subramanya/artha/ui/transaction/AddTransactionViewModel.kt \
        app/src/main/java/com/subramanya/artha/ui/review/ReviewViewModel.kt \
        app/src/main/java/com/subramanya/artha/ui/review/ReviewScreen.kt
git commit -m "feat(sms): add Review tab with badge, swipe-to-dismiss, Add Transaction prefill"
```

---

## Task 8: Notification tap → Review tab deep link, final verification

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/subramanya/artha/MainActivity.kt`

**Interfaces:**
- Consumes: `PendingTransactionNotifier.EXTRA_OPEN_REVIEW` (Task 5), `ArthaDestination.Review` (Task 7).

- [ ] **Step 1: Make `MainActivity` reuse a single instance**

In `AndroidManifest.xml`, add `android:launchMode="singleTop"` to the `<activity android:name=".MainActivity" ...>` element.

- [ ] **Step 2: Handle the notification's intent**

In `MainActivity.kt`, change the class to hold and update an intent-derived signal, and thread it down to `ArthaRoot`/`ArthaInner`/`MainApp`:

```kotlin
class MainActivity : FragmentActivity() {
    private val openReviewRequested = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent { ArthaRoot(openReviewRequested) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(PendingTransactionNotifier.EXTRA_OPEN_REVIEW, false) == true) {
            openReviewRequested.value = true
        }
    }
}
```

Add imports: `android.content.Intent`, `com.subramanya.artha.sms.PendingTransactionNotifier`, `kotlinx.coroutines.flow.MutableStateFlow`.

Update `ArthaRoot`, `ArthaInner`, and `MainApp` signatures to thread the flow through:

```kotlin
@Composable
private fun ArthaRoot(openReviewRequested: MutableStateFlow<Boolean>) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    // ...existing themeMode/useDynamicColor/biometricLock/permission-revocation code unchanged...
    ArthaTheme(themeMode = themeMode, useDynamicColor = useDynamicColor) {
        if (biometricLock) {
            BiometricLockGate { ArthaInner(app, openReviewRequested) }
        } else {
            ArthaInner(app, openReviewRequested)
        }
    }
}

@Composable
private fun ArthaInner(app: ArthaApplication, openReviewRequested: MutableStateFlow<Boolean>) {
    // ...existing produceState/startup logic unchanged...
    when (val state = current) {
        StartupState.Loading -> SplashScreen()
        StartupState.NeedsOnboarding -> {
            // ...unchanged...
        }
        is StartupState.Ready -> MainApp(
            settingsPreferences = app.settingsPreferences,
            initialName = state.userName,
            openReviewRequested = openReviewRequested,
        )
    }
}

@Composable
private fun MainApp(
    settingsPreferences: SettingsPreferences,
    initialName: String,
    openReviewRequested: MutableStateFlow<Boolean>,
) {
    // ...existing navController/backStackEntry/pendingReviewCount code unchanged...
    val shouldOpenReview by openReviewRequested.collectAsState()
    LaunchedEffect(shouldOpenReview) {
        if (shouldOpenReview) {
            navController.navigate(ArthaDestination.Review.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                launchSingleTop = true
            }
            openReviewRequested.value = false
        }
    }
    // ...existing Scaffold unchanged...
}
```

- [ ] **Step 3: Build to confirm everything compiles**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual end-to-end verification (requires a device/emulator)**

1. Enable SMS auto-import in Settings (grants permissions).
2. Send a simulated bank SMS: `adb emu sms send HDFCBK "Rs.750 debited from A/c XX4321 at BIGBASKET. Avl Bal Rs 5000"`.
3. Confirm the ongoing notification appears with "1 transaction to review".
4. Kill the app to the background, tap the notification → confirm the app opens directly on the Review tab.
5. Tap the card → confirm Add Transaction opens pre-filled (₹750, BIGBASKET, Expense tab) → Save → confirm the notification updates to 0 and cancels, and the Review tab is empty.

- [ ] **Step 5: Run the full test suite and format**

Run: `./gradlew test spotlessApply assembleDebug`
Expected: `BUILD SUCCESSFUL` for all three.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/subramanya/artha/MainActivity.kt
git commit -m "feat(sms): deep-link notification tap into the Review tab"
```

---

## Self-Review Notes

- **Spec coverage:** all 7 design decisions (detection strategy, scope, notification style, nav placement, confirm flow, dismiss/duplicates, permission flow) are implemented — Decisions 1/2 in Task 3, Decision 3 in Task 5, Decision 4 in Task 7, Decision 5 in Task 7, Decision 6 in Task 7 (swipe-to-dismiss; duplicates explicitly not deduped, matching the design), Decision 7 in Task 6. "Out of scope" items (per-bank catalogue, inbox backfill, auto-dedup, inline confirm) are correctly absent.
- **Type consistency check:** `SmsDirection` (Task 2) is used identically in `BankSmsParser`/`ParsedBankSms` (Task 3), `suggestCategoryFor` (Task 4), `PendingSmsTransaction` (Task 2), and `applyPendingSmsPrefill` (Task 7) — no renamed variants. `PendingTransactionRepository.observeCount()`/`.observeAll()`/`.insert()`/`.dismiss()` names match every call site across Tasks 5, 7, 8.
- **No device/emulator on this dev machine:** every task that needs one for full verification (Task 1's instrumented migration test, Task 5's manual SMS simulation, Task 8's end-to-end check) says so explicitly rather than silently assuming it — flagged, not hidden.
