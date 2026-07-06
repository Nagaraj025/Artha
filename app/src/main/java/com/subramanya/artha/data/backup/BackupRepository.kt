package com.subramanya.artha.data.backup

import androidx.room.withTransaction
import com.subramanya.artha.data.db.AppDatabase
import com.subramanya.artha.data.entity.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Reads the whole database into a [BackupData] snapshot and writes a [BackupData] back,
 * atomically. This is the database side of the D3 backup/restore feature; [BackupCodec]
 * is the (pure) JSON side. Keeping the two apart means the round-trip can be unit-tested
 * without Room, and the same [BackupData] shape flows through both export paths.
 */
class BackupRepository(private val db: AppDatabase) {

    /**
     * One-shot read of every Room table off the main thread. Uses each DAO's existing
     * `observeAll()` Flow and takes its first emission — no new query surface needed.
     */
    suspend fun snapshot(): BackupData = withContext(Dispatchers.IO) {
        BackupData(
            accounts = db.accountDao().observeAll().first(),
            cards = db.cardDao().observeAll().first(),
            categories = db.categoryDao().observeAll().first(),
            people = db.personDao().observeAll().first(),
            tags = db.tagDao().observeAll().first(),
            investments = db.investmentDao().observeAll().first(),
            insurances = db.insuranceDao().observeAll().first(),
            transactionRules = db.transactionRuleDao().observeAll().first(),
            budgets = db.budgetDao().observeAll().first(),
            goals = db.goalDao().observeAll().first(),
            subscriptions = db.subscriptionDao().observeAll().first(),
            recurringRules = db.recurringRuleDao().observeAll().first(),
            transactions = db.transactionDao().observeAll().first(),
            transactionPeople = db.transactionDao().observeAllPeopleLinks().first(),
            transactionTags = db.transactionDao().observeAllTagLinks().first(),
        )
    }

    /**
     * Replaces ALL current data with [data], atomically. The entire wipe + reinsert runs
     * inside a single Room [withTransaction] block: if ANY step throws (e.g. a corrupt
     * row), Room rolls the whole transaction back, so the user is never left with a
     * half-wiped database. This is the data-loss safety guarantee for restore.
     *
     * FK-safe order:
     *  - DELETE child -> parent (cross-refs, then transactions, then leaf entities, then
     *    parents) — handled by [com.subramanya.artha.data.dao.BackupDao].
     *  - INSERT parent -> child: accounts/categories/people/tags/insurances first, then
     *    cards (FK -> accounts) and investments (FK -> insurances), then transactions
     *    (no FK), then the cross-refs (FK -> transactions + people/tags) LAST.
     *  - Categories self-reference via parent_id, so they're inserted roots-before-children.
     */
    suspend fun restore(data: BackupData) = withContext(Dispatchers.IO) {
        db.withTransaction {
            // 1. Wipe everything (child -> parent).
            db.backupDao().apply {
                deleteAllTransactionTags()
                deleteAllTransactionPeople()
                deleteAllTransactions()
                deleteAllRecurringRules()
                deleteAllSubscriptions()
                deleteAllGoals()
                deleteAllBudgets()
                deleteAllTransactionRules()
                deleteAllInvestments()
                deleteAllInsurances()
                deleteAllCards()
                deleteAllTags()
                deleteAllPeople()
                deleteAllCategories()
                deleteAllAccounts()
            }

            // 2. Insert parents first.
            db.accountDao().upsertAll(data.accounts)
            insertCategoriesParentsFirst(data.categories)
            data.people.forEach { db.personDao().upsert(it) }
            data.tags.forEach { db.tagDao().upsert(it) }
            data.insurances.forEach { db.insuranceDao().upsert(it) }

            // 3. Children that FK into the parents above.
            db.cardDao().upsertAll(data.cards) // FK -> accounts
            db.investmentDao().upsertAll(data.investments) // FK -> insurances

            // 4. Independent leaf entities.
            data.transactionRules.forEach { db.transactionRuleDao().upsert(it) }
            data.budgets.forEach { db.budgetDao().upsert(it) }
            data.goals.forEach { db.goalDao().upsert(it) }
            data.subscriptions.forEach { db.subscriptionDao().upsert(it) }
            data.recurringRules.forEach { db.recurringRuleDao().upsert(it) }

            // 5. Transactions (no FK), then cross-refs LAST (FK -> transactions + people/tags).
            data.transactions.forEach { db.transactionDao().insertTransaction(it) }
            db.transactionDao().insertPeopleLinks(data.transactionPeople)
            db.transactionDao().insertTagLinks(data.transactionTags)
        }
    }

    /**
     * Categories can reference a parent category (self-FK). With FK enforcement on, a
     * child must be inserted after its parent. We insert in waves: roots (or rows whose
     * parent is already inserted) first, repeating until all are placed. Any row whose
     * parent id isn't present in the snapshot is treated as a root (the parent_id column
     * is SET_NULL on delete anyway, so a dangling reference is harmless).
     */
    private suspend fun insertCategoriesParentsFirst(categories: List<CategoryEntity>) {
        val dao = db.categoryDao()
        val present = categories.mapTo(HashSet()) { it.id }
        val inserted = HashSet<String>()
        var remaining = categories
        while (remaining.isNotEmpty()) {
            val ready = remaining.filter { cat ->
                val parent = cat.parentId
                parent == null || parent !in present || parent in inserted
            }
            if (ready.isEmpty()) {
                // Cyclic / unexpected — insert the rest as-is rather than loop forever.
                remaining.forEach { dao.upsert(it) }
                return
            }
            ready.forEach {
                dao.upsert(it)
                inserted.add(it.id)
            }
            remaining = remaining.filterNot { it.id in inserted }
        }
    }
}
