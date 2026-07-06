package com.subramanya.artha.data.db.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.subramanya.artha.data.entity.TransactionRuleEntity

/**
 * Seeds the 10 PRD §10 rules on first DB creation. Same pattern as
 * [CategorySeederCallback] — raw inserts inside onCreate so the seed completes
 * synchronously before any DAO read can return an empty list.
 */
internal class RuleSeederCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            for (rule in SeedRules.all()) {
                db.insert(
                    "transaction_rules",
                    SQLiteDatabase.CONFLICT_REPLACE,
                    rule.copy(createdAt = now).toContentValues(),
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun TransactionRuleEntity.toContentValues(): ContentValues = ContentValues().apply {
        put("id", id)
        put("name", name)
        put("conditions_json", conditionsJson)
        put("actions_json", actionsJson)
        put("priority", priority)
        put("is_active", if (isActive) 1 else 0)
        put("is_system", if (isSystem) 1 else 0)
        put("created_at", createdAt)
    }
}
