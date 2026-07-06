package com.subramanya.artha.data.backup

import com.subramanya.artha.data.entity.AccountEntity
import com.subramanya.artha.data.entity.BudgetEntity
import com.subramanya.artha.data.entity.CardEntity
import com.subramanya.artha.data.entity.CategoryEntity
import com.subramanya.artha.data.entity.GoalEntity
import com.subramanya.artha.data.entity.InsuranceEntity
import com.subramanya.artha.data.entity.InvestmentEntity
import com.subramanya.artha.data.entity.PersonEntity
import com.subramanya.artha.data.entity.RecurringRuleEntity
import com.subramanya.artha.data.entity.SubscriptionEntity
import com.subramanya.artha.data.entity.TagEntity
import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.entity.TransactionPersonCrossRef
import com.subramanya.artha.data.entity.TransactionRuleEntity
import com.subramanya.artha.data.entity.TransactionTagCrossRef
import com.subramanya.artha.data.entity.enums.AccountType
import com.subramanya.artha.data.entity.enums.BudgetPeriod
import com.subramanya.artha.data.entity.enums.BudgetScope
import com.subramanya.artha.data.entity.enums.CardNetwork
import com.subramanya.artha.data.entity.enums.CardType
import com.subramanya.artha.data.entity.enums.CategoryType
import com.subramanya.artha.data.entity.enums.InsuranceType
import com.subramanya.artha.data.entity.enums.InvestmentType
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.PersonRelation
import com.subramanya.artha.data.entity.enums.PremiumFrequency
import com.subramanya.artha.data.entity.enums.RecurringFrequency
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.SubscriptionFrequency
import com.subramanya.artha.data.entity.enums.SubscriptionStatus
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.data.entity.enums.ValuationMode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The safety gate for D3 backup/restore. A wrong restore wipes the user's financial
 * data, so this proves encode -> decode is lossless for EVERY Room table and EVERY
 * column, exercising each nullable field both null and non-null across two rows.
 *
 * Data classes give structural equality, so a whole-object [assertEquals] on the
 * BackupData (after sorting nothing — the codec must preserve order) is the strongest
 * possible assertion: any dropped or mistyped column fails the test.
 */
class BackupCodecTest {

    @Test
    fun `round trip preserves every table and every field`() {
        val data = sampleBackupData()
        val json = BackupCodec.encode(data, exportedAt = 1_717_000_000_000L)
        val decoded = BackupCodec.decode(json)
        assertEquals(data, decoded)
    }

    @Test
    fun `decode tolerates missing arrays as empty lists`() {
        // An older/newer backup that only has accounts must not crash; absent tables
        // come back empty rather than throwing.
        val minimal = """{ "schema_version": 1, "exported_at": 0, "accounts": [] }"""
        val decoded = BackupCodec.decode(minimal)
        assertEquals(BackupData(), decoded)
    }

    @Test
    fun `encode stamps schema version and the provided timestamp`() {
        val json = BackupCodec.encode(BackupData(), exportedAt = 42L)
        val obj = org.json.JSONObject(json)
        assertEquals(BackupCodec.SCHEMA_VERSION, obj.getInt("schema_version"))
        assertEquals(42L, obj.getLong("exported_at"))
    }

    private fun sampleBackupData(): BackupData = BackupData(
        accounts = listOf(
            // row 0: every nullable populated
            AccountEntity(
                id = "acct-1", name = "HDFC Savings", type = AccountType.SAVINGS,
                institution = "HDFC Bank", accountNumberLast4 = "1234",
                openingBalance = 10_000.50, currency = "INR", icon = "bank",
                color = 0xFF112233L, isArchived = false, displayOrder = 0, createdAt = 1_000L,
            ),
            // row 1: every nullable null
            AccountEntity(
                id = "acct-2", name = "Cash", type = AccountType.CASH,
                institution = null, accountNumberLast4 = null,
                openingBalance = 0.0, currency = "INR", icon = "wallet",
                color = 0xFF000000L, isArchived = true, displayOrder = 5, createdAt = 2_000L,
            ),
        ),
        cards = listOf(
            CardEntity(
                id = "card-1", name = "Amazon Pay", type = CardType.CREDIT, issuer = "ICICI",
                network = CardNetwork.VISA, cardNumberLast4 = "9999", creditLimit = 200_000.0,
                statementDayOfMonth = 5, dueDayOfMonth = 25, linkedAccountId = "acct-1",
                icon = "card", color = 0xFFAABBCCL, isArchived = false, displayOrder = 0, createdAt = 3_000L,
            ),
            CardEntity(
                id = "card-2", name = "Debit", type = CardType.DEBIT, issuer = null,
                network = CardNetwork.RUPAY, cardNumberLast4 = null, creditLimit = null,
                statementDayOfMonth = null, dueDayOfMonth = null, linkedAccountId = null,
                icon = "card", color = 0xFF010203L, isArchived = true, displayOrder = 1, createdAt = 4_000L,
            ),
        ),
        categories = listOf(
            CategoryEntity(
                id = "cat-1",
                name = "Food",
                parentId = null,
                type = CategoryType.EXPENSE,
                icon = "food",
                color = 0xFF445566L,
                isSystem = true,
                displayOrder = 0,
            ),
            CategoryEntity(
                id = "cat-2",
                name = "Groceries",
                parentId = "cat-1",
                type = CategoryType.EXPENSE,
                icon = "cart",
                color = 0xFF778899L,
                isSystem = false,
                displayOrder = 1,
            ),
        ),
        people = listOf(
            PersonEntity(
                id = "p-1",
                name = "Spouse",
                relation = PersonRelation.SPOUSE,
                contact = "98765",
                avatarUri = "content://x",
                createdAt = 5_000L,
            ),
            PersonEntity(
                id = "p-2",
                name = "Friend",
                relation = PersonRelation.FRIEND,
                contact = null,
                avatarUri = null,
                createdAt = 6_000L,
            ),
        ),
        tags = listOf(
            TagEntity(id = "tag-1", name = "Reimbursable", color = 0xFF123456L),
            TagEntity(id = "tag-2", name = "Tax", color = 0xFF654321L),
        ),
        transactions = listOf(
            // row 0: every nullable populated
            TransactionEntity(
                id = "txn-1", type = TransactionType.EXPENSE, amount = 250.75, currency = "INR",
                date = 7_000L, description = "Lunch", categoryId = "cat-1", subCategoryId = "cat-2",
                sourceType = SourceKind.ACCOUNT, sourceId = "acct-1",
                destinationType = SourceKind.EXTERNAL, destinationId = "ext-1",
                paymentApp = PaymentApp.GPAY, place = "Cafe", latitude = 12.97, longitude = 77.59,
                receiptUri = "content://r", notes = "with team", taxSection = "80C",
                recurringRuleId = "rr-1", isSplit = true, splitGroupId = "grp-1",
                source = TransactionSource.MANUAL, createdAt = 8_000L, updatedAt = 9_000L,
            ),
            // row 1: every nullable null
            TransactionEntity(
                id = "txn-2", type = TransactionType.INTEREST, amount = 10.0, currency = "INR",
                date = 10_000L, description = "FD interest", categoryId = null, subCategoryId = null,
                sourceType = SourceKind.INVESTMENT, sourceId = null,
                destinationType = null, destinationId = null,
                paymentApp = PaymentApp.OTHER, place = null, latitude = null, longitude = null,
                receiptUri = null, notes = null, taxSection = null,
                recurringRuleId = null, isSplit = false, splitGroupId = null,
                source = TransactionSource.RECURRING, createdAt = 11_000L, updatedAt = 12_000L,
            ),
        ),
        transactionPeople = listOf(
            TransactionPersonCrossRef(transactionId = "txn-1", personId = "p-1"),
            TransactionPersonCrossRef(transactionId = "txn-1", personId = "p-2"),
        ),
        transactionTags = listOf(
            TransactionTagCrossRef(transactionId = "txn-1", tagId = "tag-1"),
            TransactionTagCrossRef(transactionId = "txn-2", tagId = "tag-2"),
        ),
        investments = listOf(
            // row 0: every nullable populated
            InvestmentEntity(
                id = "inv-1", name = "HDFC RD", type = InvestmentType.RD, institution = "HDFC",
                currentValue = 65_000.0, valuationMode = ValuationMode.DERIVED,
                openingContribution = 60_000.0, units = 100.5, nav = 12.34,
                startDate = 13_000L, maturityDate = 14_000L, taxSection = "80C",
                icon = "rd", color = 0xFF222222L, linkedInsuranceId = "ins-1",
                isArchived = false, displayOrder = 0, createdAt = 15_000L,
            ),
            // row 1: every nullable null
            InvestmentEntity(
                id = "inv-2", name = "SIP", type = InvestmentType.SIP, institution = null,
                currentValue = 5_000.0, valuationMode = ValuationMode.MARKET,
                openingContribution = 0.0, units = null, nav = null,
                startDate = 16_000L, maturityDate = null, taxSection = null,
                icon = "sip", color = 0xFF333333L, linkedInsuranceId = null,
                isArchived = true, displayOrder = 1, createdAt = 17_000L,
            ),
        ),
        insurances = listOf(
            InsuranceEntity(
                id = "ins-1", name = "LIC Jeevan", type = InsuranceType.LIFE_ENDOWMENT,
                provider = "LIC", policyNumber = "POL-1", sumAssured = 1_000_000.0,
                premiumAmount = 25_000.0, premiumFrequency = PremiumFrequency.YEARLY,
                nextPremiumDate = 18_000L, startDate = 19_000L, endDate = 20_000L,
                nominee = "Spouse", agentContact = "9876", policyDocUri = "content://doc",
                taxSection = "80C", icon = "shield", color = 0xFF444444L,
                isArchived = false, createdAt = 21_000L,
            ),
            InsuranceEntity(
                id = "ins-2", name = "Health", type = InsuranceType.HEALTH,
                provider = "Star", policyNumber = null, sumAssured = 500_000.0,
                premiumAmount = 12_000.0, premiumFrequency = PremiumFrequency.MONTHLY,
                nextPremiumDate = null, startDate = 22_000L, endDate = null,
                nominee = null, agentContact = null, policyDocUri = null,
                taxSection = null, icon = "health", color = 0xFF555555L,
                isArchived = true, createdAt = 23_000L,
            ),
        ),
        transactionRules = listOf(
            TransactionRuleEntity(
                id = "tr-1",
                name = "Auto-tag Swiggy",
                conditionsJson = """{"merchant":"swiggy"}""",
                actionsJson = """{"category":"cat-1"}""",
                priority = 1,
                isActive = true,
                isSystem = true,
                createdAt = 24_000L,
            ),
            TransactionRuleEntity(
                id = "tr-2",
                name = "Custom",
                conditionsJson = "{}",
                actionsJson = "{}",
                priority = 99,
                isActive = false,
                isSystem = false,
                createdAt = 25_000L,
            ),
        ),
        budgets = listOf(
            BudgetEntity(
                id = "bud-1", name = "Monthly food", scope = BudgetScope.CATEGORY,
                categoryId = "cat-1", amount = 8_000.0, period = BudgetPeriod.MONTHLY,
                startDate = 26_000L, alertThresholdPercent = 80, isActive = true, createdAt = 27_000L,
            ),
            BudgetEntity(
                id = "bud-2", name = "Overall", scope = BudgetScope.OVERALL,
                categoryId = null, amount = 50_000.0, period = BudgetPeriod.MONTHLY,
                startDate = 28_000L, alertThresholdPercent = 90, isActive = false, createdAt = 29_000L,
            ),
        ),
        goals = listOf(
            GoalEntity(
                id = "goal-1", name = "Car", targetAmount = 1_500_000.0, targetDate = 30_000L,
                linkedAccountIdsJson = """["acct-1"]""", linkedInvestmentIdsJson = """["inv-1"]""",
                icon = "car", color = 0xFF666666L, isAchieved = false, createdAt = 31_000L,
            ),
            GoalEntity(
                id = "goal-2", name = "Emergency", targetAmount = 300_000.0, targetDate = null,
                linkedAccountIdsJson = "", linkedInvestmentIdsJson = "",
                icon = "umbrella", color = 0xFF777777L, isAchieved = true, createdAt = 32_000L,
            ),
        ),
        subscriptions = listOf(
            SubscriptionEntity(
                id = "sub-1", name = "Netflix", provider = "Netflix", amount = 649.0,
                frequency = SubscriptionFrequency.MONTHLY, nextDueDate = 33_000L,
                lastPaidDate = 32_500L, categoryId = "cat-1", paymentMethodType = "CARD",
                paymentMethodId = "card-1", status = SubscriptionStatus.ACTIVE,
                autoCharge = true, logoUri = "content://logo", color = 0xFF888888L, createdAt = 34_000L,
            ),
            SubscriptionEntity(
                id = "sub-2", name = "Gym", provider = null, amount = 1_000.0,
                frequency = SubscriptionFrequency.YEARLY, nextDueDate = 35_000L,
                lastPaidDate = null, categoryId = null, paymentMethodType = null,
                paymentMethodId = null, status = SubscriptionStatus.CANCELLED,
                autoCharge = false, logoUri = null, color = 0xFF999999L, createdAt = 36_000L,
            ),
        ),
        recurringRules = listOf(
            RecurringRuleEntity(
                id = "rr-1", name = "Rent", transactionTemplate = """{"amount":20000}""",
                frequency = RecurringFrequency.MONTHLY, dayOfPeriod = 1, nextRunDate = 37_000L,
                lastRunDate = 36_500L, autoConfirm = true, isActive = true, createdAt = 38_000L,
            ),
            RecurringRuleEntity(
                id = "rr-2", name = "Daily", transactionTemplate = "{}",
                frequency = RecurringFrequency.DAILY, dayOfPeriod = null, nextRunDate = 39_000L,
                lastRunDate = null, autoConfirm = false, isActive = false, createdAt = 40_000L,
            ),
        ),
    )
}
