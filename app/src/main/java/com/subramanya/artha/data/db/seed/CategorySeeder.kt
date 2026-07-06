package com.subramanya.artha.data.db.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.subramanya.artha.data.entity.CategoryEntity

/**
 * Seeds PRD §9 categories once, on first DB creation. Uses raw SupportSQLiteDatabase
 * inserts so the seed completes synchronously inside onCreate — no race against the
 * first DAO read.
 *
 * Column names must match the @ColumnInfo names on CategoryEntity exactly.
 */
internal class CategorySeederCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.beginTransaction()
        try {
            for (category in SeedCategories.all()) {
                db.insert("categories", SQLiteDatabase.CONFLICT_REPLACE, category.toContentValues())
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun CategoryEntity.toContentValues(): ContentValues = ContentValues().apply {
        put("id", id)
        put("name", name)
        put("parent_id", parentId)
        put("type", type.name)
        put("icon", icon)
        put("color", color)
        put("is_system", if (isSystem) 1 else 0)
        put("display_order", displayOrder)
    }
}
