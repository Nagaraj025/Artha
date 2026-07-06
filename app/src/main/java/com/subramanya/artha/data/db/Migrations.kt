package com.subramanya.artha.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v3 -> v4: investment valuation redesign.
 *
 * Adds two columns to `investments` and back-fills existing rows so no data is lost:
 *  - `opening_contribution` (REAL): seeded from each row's existing `current_value` so a
 *    DERIVED instrument's contribution base starts at its last-known value.
 *  - `valuation_mode` (TEXT): defaults to 'MARKET'; deposit-style instruments
 *    (FD/RD/PPF/EPF/BONDS) are switched to 'DERIVED' to match
 *    InvestmentType.defaultValuationMode().
 *
 * Columns are NOT NULL with defaults so the ALTER succeeds on existing rows, and the
 * UPDATEs run inside the same migration to give every pre-existing row a sensible value.
 * The `type` column stores InvestmentType.name (enum literals), so the IN-list matches the
 * stored strings exactly — getting these literals wrong would silently no-op the back-fill.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE investments ADD COLUMN opening_contribution REAL NOT NULL DEFAULT 0",
        )
        db.execSQL("UPDATE investments SET opening_contribution = current_value")
        db.execSQL(
            "ALTER TABLE investments ADD COLUMN valuation_mode TEXT NOT NULL DEFAULT 'MARKET'",
        )
        db.execSQL(
            "UPDATE investments SET valuation_mode = 'DERIVED' " +
                "WHERE type IN ('FD','RD','PPF','EPF','BONDS')",
        )
    }
}

/**
 * v4 -> v5: add `excluded_from_expense_total` to `transactions` so a rule's
 * ExcludeFromExpenseTotal action can be honored by the monthly aggregator. NOT NULL DEFAULT 0
 * (false) so existing rows are unaffected; matches the entity's `defaultValue = "0"`.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE transactions ADD COLUMN excluded_from_expense_total INTEGER NOT NULL DEFAULT 0",
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pending_sms_transactions` (
            `id` TEXT NOT NULL,
            `raw_sms_body` TEXT NOT NULL,
            `sender` TEXT NOT NULL,
            `received_at` INTEGER NOT NULL,
            `direction` TEXT NOT NULL,
            `amount` REAL NOT NULL,
            `account_hint` TEXT,
            `merchant` TEXT,
            `suggested_category_id` TEXT,
            PRIMARY KEY(`id`))
            """.trimIndent(),
        )
    }
}
