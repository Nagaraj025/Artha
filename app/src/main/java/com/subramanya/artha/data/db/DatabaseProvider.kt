package com.subramanya.artha.data.db

import android.content.Context
import androidx.room.Room
import com.subramanya.artha.BuildConfig
import com.subramanya.artha.data.db.seed.CategorySeederCallback
import com.subramanya.artha.data.db.seed.RuleSeederCallback

/**
 * Process-wide singleton for AppDatabase. No DI framework in Phase 1; callers retrieve
 * the database via [get]. ArthaApplication wires the application context.
 */
object DatabaseProvider {

    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

    private fun build(appContext: Context): AppDatabase {
        val builder =
            Room.databaseBuilder(appContext, AppDatabase::class.java, AppDatabase.DB_NAME)
                .addCallback(CategorySeederCallback())
                .addCallback(RuleSeederCallback())
                // Real, data-preserving migrations run first. v3->v4 adds investment valuation
                // columns; v4->v5 adds transactions.excluded_from_expense_total; v5->v6 adds
                // pending_sms_transactions table (see Migrations.kt).
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)

        // Destructive fallback in DEBUG ONLY. During development a schema gap not covered by an
        // explicit migration resets the local DB instead of crashing — convenient for iteration.
        //
        // In RELEASE we deliberately omit it: a missing/failed migration then crashes loudly
        // rather than SILENTLY WIPING the user's financial data (there is no cloud backup).
        // So: always add a MIGRATION_n_n+1 to addMigrations(...) above BEFORE bumping the
        // @Database version, or release builds will fail to open the DB.
        if (BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration()
        }

        return builder.build()
    }
}
