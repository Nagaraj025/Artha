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
                    (id, raw_sms_body, sender, received_at, direction, amount, account_hint, merchant, suggested_category_id)
                VALUES
                    ('p1', 'Rs.500 debited', 'HDFCBK', 1700000000000, 'DEBIT', 500.0, '1234', 'Swiggy', NULL)
                """.trimIndent(),
            )
            migratedDb.query("SELECT * FROM pending_sms_transactions WHERE id = 'p1'").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("HDFCBK", cursor.getString(cursor.getColumnIndexOrThrow("sender")))
                assertEquals(500.0, cursor.getDouble(cursor.getColumnIndexOrThrow("amount")), 0.0001)
                assertEquals("1234", cursor.getString(cursor.getColumnIndexOrThrow("account_hint")))
                assertEquals(
                    1700000000000,
                    cursor.getLong(cursor.getColumnIndexOrThrow("received_at")),
                )
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
