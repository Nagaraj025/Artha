package com.subramanya.artha.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.subramanya.artha.data.dao.AccountDao
import com.subramanya.artha.data.dao.BackupDao
import com.subramanya.artha.data.dao.BudgetDao
import com.subramanya.artha.data.dao.CardDao
import com.subramanya.artha.data.dao.CategoryDao
import com.subramanya.artha.data.dao.GoalDao
import com.subramanya.artha.data.dao.InsuranceDao
import com.subramanya.artha.data.dao.InvestmentDao
import com.subramanya.artha.data.dao.PendingTransactionDao
import com.subramanya.artha.data.dao.PersonDao
import com.subramanya.artha.data.dao.RecurringRuleDao
import com.subramanya.artha.data.dao.SubscriptionDao
import com.subramanya.artha.data.dao.TagDao
import com.subramanya.artha.data.dao.TransactionDao
import com.subramanya.artha.data.dao.TransactionRuleDao
import com.subramanya.artha.data.entity.AccountEntity
import com.subramanya.artha.data.entity.BudgetEntity
import com.subramanya.artha.data.entity.CardEntity
import com.subramanya.artha.data.entity.CategoryEntity
import com.subramanya.artha.data.entity.GoalEntity
import com.subramanya.artha.data.entity.InsuranceEntity
import com.subramanya.artha.data.entity.InvestmentEntity
import com.subramanya.artha.data.entity.PendingSmsTransactionEntity
import com.subramanya.artha.data.entity.PersonEntity
import com.subramanya.artha.data.entity.RecurringRuleEntity
import com.subramanya.artha.data.entity.SubscriptionEntity
import com.subramanya.artha.data.entity.TagEntity
import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.entity.TransactionPersonCrossRef
import com.subramanya.artha.data.entity.TransactionRuleEntity
import com.subramanya.artha.data.entity.TransactionTagCrossRef

@Database(
    entities = [
        AccountEntity::class,
        CardEntity::class,
        CategoryEntity::class,
        PersonEntity::class,
        TagEntity::class,
        TransactionEntity::class,
        TransactionPersonCrossRef::class,
        TransactionTagCrossRef::class,
        // Phase 2 additions
        InvestmentEntity::class,
        InsuranceEntity::class,
        TransactionRuleEntity::class,
        // Phase 4 additions
        BudgetEntity::class,
        GoalEntity::class,
        SubscriptionEntity::class,
        RecurringRuleEntity::class,
        // Phase 6 — SMS auto-detect
        PendingSmsTransactionEntity::class,
    ],
    version = 6,
    // Schemas are exported to app/schemas (room.schemaLocation in build.gradle.kts) so
    // MigrationTestHelper can validate migrations against the generated schema.
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun cardDao(): CardDao
    abstract fun categoryDao(): CategoryDao
    abstract fun personDao(): PersonDao
    abstract fun tagDao(): TagDao
    abstract fun transactionDao(): TransactionDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun insuranceDao(): InsuranceDao
    abstract fun transactionRuleDao(): TransactionRuleDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun pendingTransactionDao(): PendingTransactionDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val DB_NAME = "artha.db"
    }
}
