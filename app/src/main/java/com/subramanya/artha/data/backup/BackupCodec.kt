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
import org.json.JSONArray
import org.json.JSONObject

/**
 * The complete, single source of truth for serialising the Artha database to/from a
 * JSON backup. Covers EVERY Room table — all 15 entities incl. the two cross-ref
 * tables — so the plain and encrypted export paths can never drift apart again
 * (that drift was the D3 audit bug: investments/insurance/budgets/goals/subscriptions/
 * recurring/rules and the cross-refs were silently dropped, and there was no restore).
 *
 * Design constraints:
 *  - PURE: no Android, no Room, no clock. [encode] takes `exportedAt` as a parameter so
 *    the round-trip test is deterministic. JSON via `org.json` (the real lib in tests).
 *  - LOSSLESS: every column of every entity is written and read back. Enums go through
 *    `.name` / `valueOf`. Nullable fields are written as JSON null and restored as null.
 *  - TOLERANT on decode: a missing top-level array decodes to an empty list, so a backup
 *    written by an older or newer app version never crashes the importer.
 *
 * Schema discipline: bump [SCHEMA_VERSION] if a column is added/removed/renamed. The
 * decoder reads by key, so adding a NEW nullable column is backward compatible (old
 * backups lack the key -> we read null); adding a NEW required column needs a default
 * here. Do NOT reuse this format across an entity rename without a version bump.
 */
object BackupCodec {

    const val SCHEMA_VERSION = 1

    // ---- top-level encode/decode ----

    fun encode(data: BackupData, exportedAt: Long): String {
        val root = JSONObject()
        root.put("schema_version", SCHEMA_VERSION)
        root.put("exported_at", exportedAt)
        root.put("accounts", data.accounts.toArray(::accountToJson))
        root.put("cards", data.cards.toArray(::cardToJson))
        root.put("categories", data.categories.toArray(::categoryToJson))
        root.put("people", data.people.toArray(::personToJson))
        root.put("tags", data.tags.toArray(::tagToJson))
        root.put("investments", data.investments.toArray(::investmentToJson))
        root.put("insurances", data.insurances.toArray(::insuranceToJson))
        root.put("transaction_rules", data.transactionRules.toArray(::transactionRuleToJson))
        root.put("budgets", data.budgets.toArray(::budgetToJson))
        root.put("goals", data.goals.toArray(::goalToJson))
        root.put("subscriptions", data.subscriptions.toArray(::subscriptionToJson))
        root.put("recurring_rules", data.recurringRules.toArray(::recurringRuleToJson))
        root.put("transactions", data.transactions.toArray(::transactionToJson))
        root.put("transaction_people", data.transactionPeople.toArray(::transactionPersonToJson))
        root.put("transaction_tags", data.transactionTags.toArray(::transactionTagToJson))
        return root.toString(2)
    }

    fun decode(json: String): BackupData {
        val root = JSONObject(json)
        return BackupData(
            accounts = root.read("accounts", ::accountFromJson),
            cards = root.read("cards", ::cardFromJson),
            categories = root.read("categories", ::categoryFromJson),
            people = root.read("people", ::personFromJson),
            tags = root.read("tags", ::tagFromJson),
            investments = root.read("investments", ::investmentFromJson),
            insurances = root.read("insurances", ::insuranceFromJson),
            transactionRules = root.read("transaction_rules", ::transactionRuleFromJson),
            budgets = root.read("budgets", ::budgetFromJson),
            goals = root.read("goals", ::goalFromJson),
            subscriptions = root.read("subscriptions", ::subscriptionFromJson),
            recurringRules = root.read("recurring_rules", ::recurringRuleFromJson),
            transactions = root.read("transactions", ::transactionFromJson),
            transactionPeople = root.read("transaction_people", ::transactionPersonFromJson),
            transactionTags = root.read("transaction_tags", ::transactionTagFromJson),
        )
    }

    // ---- per-entity encoders ----

    private fun accountToJson(a: AccountEntity) = JSONObject().apply {
        put("id", a.id)
        put("name", a.name)
        put("type", a.type.name)
        putNullable("institution", a.institution)
        putNullable("account_number_last4", a.accountNumberLast4)
        put("opening_balance", a.openingBalance)
        put("currency", a.currency)
        put("icon", a.icon)
        put("color", a.color)
        put("is_archived", a.isArchived)
        put("display_order", a.displayOrder)
        put("created_at", a.createdAt)
    }

    private fun cardToJson(c: CardEntity) = JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("type", c.type.name)
        putNullable("issuer", c.issuer)
        put("network", c.network.name)
        putNullable("card_number_last4", c.cardNumberLast4)
        putNullable("credit_limit", c.creditLimit)
        putNullable("statement_day_of_month", c.statementDayOfMonth)
        putNullable("due_day_of_month", c.dueDayOfMonth)
        putNullable("linked_account_id", c.linkedAccountId)
        put("icon", c.icon)
        put("color", c.color)
        put("is_archived", c.isArchived)
        put("display_order", c.displayOrder)
        put("created_at", c.createdAt)
    }

    private fun categoryToJson(c: CategoryEntity) = JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        putNullable("parent_id", c.parentId)
        put("type", c.type.name)
        put("icon", c.icon)
        put("color", c.color)
        put("is_system", c.isSystem)
        put("display_order", c.displayOrder)
    }

    private fun personToJson(p: PersonEntity) = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("relation", p.relation.name)
        putNullable("contact", p.contact)
        putNullable("avatar_uri", p.avatarUri)
        put("created_at", p.createdAt)
    }

    private fun tagToJson(t: TagEntity) = JSONObject().apply {
        put("id", t.id)
        put("name", t.name)
        put("color", t.color)
    }

    private fun investmentToJson(i: InvestmentEntity) = JSONObject().apply {
        put("id", i.id)
        put("name", i.name)
        put("type", i.type.name)
        putNullable("institution", i.institution)
        put("current_value", i.currentValue)
        put("valuation_mode", i.valuationMode.name)
        put("opening_contribution", i.openingContribution)
        putNullable("units", i.units)
        putNullable("nav", i.nav)
        put("start_date", i.startDate)
        putNullable("maturity_date", i.maturityDate)
        putNullable("tax_section", i.taxSection)
        put("icon", i.icon)
        put("color", i.color)
        putNullable("linked_insurance_id", i.linkedInsuranceId)
        put("is_archived", i.isArchived)
        put("display_order", i.displayOrder)
        put("created_at", i.createdAt)
    }

    private fun insuranceToJson(i: InsuranceEntity) = JSONObject().apply {
        put("id", i.id)
        put("name", i.name)
        put("type", i.type.name)
        put("provider", i.provider)
        putNullable("policy_number", i.policyNumber)
        put("sum_assured", i.sumAssured)
        put("premium_amount", i.premiumAmount)
        put("premium_frequency", i.premiumFrequency.name)
        putNullable("next_premium_date", i.nextPremiumDate)
        put("start_date", i.startDate)
        putNullable("end_date", i.endDate)
        putNullable("nominee", i.nominee)
        putNullable("agent_contact", i.agentContact)
        putNullable("policy_doc_uri", i.policyDocUri)
        putNullable("tax_section", i.taxSection)
        put("icon", i.icon)
        put("color", i.color)
        put("is_archived", i.isArchived)
        put("created_at", i.createdAt)
    }

    private fun transactionRuleToJson(r: TransactionRuleEntity) = JSONObject().apply {
        put("id", r.id)
        put("name", r.name)
        put("conditions_json", r.conditionsJson)
        put("actions_json", r.actionsJson)
        put("priority", r.priority)
        put("is_active", r.isActive)
        put("is_system", r.isSystem)
        put("created_at", r.createdAt)
    }

    private fun budgetToJson(b: BudgetEntity) = JSONObject().apply {
        put("id", b.id)
        put("name", b.name)
        put("scope", b.scope.name)
        putNullable("category_id", b.categoryId)
        put("amount", b.amount)
        put("period", b.period.name)
        put("start_date", b.startDate)
        put("alert_threshold_percent", b.alertThresholdPercent)
        put("is_active", b.isActive)
        put("created_at", b.createdAt)
    }

    private fun goalToJson(g: GoalEntity) = JSONObject().apply {
        put("id", g.id)
        put("name", g.name)
        put("target_amount", g.targetAmount)
        putNullable("target_date", g.targetDate)
        put("linked_account_ids", g.linkedAccountIdsJson)
        put("linked_investment_ids", g.linkedInvestmentIdsJson)
        put("icon", g.icon)
        put("color", g.color)
        put("is_achieved", g.isAchieved)
        put("created_at", g.createdAt)
    }

    private fun subscriptionToJson(s: SubscriptionEntity) = JSONObject().apply {
        put("id", s.id)
        put("name", s.name)
        putNullable("provider", s.provider)
        put("amount", s.amount)
        put("frequency", s.frequency.name)
        put("next_due_date", s.nextDueDate)
        putNullable("last_paid_date", s.lastPaidDate)
        putNullable("category_id", s.categoryId)
        putNullable("payment_method_type", s.paymentMethodType)
        putNullable("payment_method_id", s.paymentMethodId)
        put("status", s.status.name)
        put("auto_charge", s.autoCharge)
        putNullable("logo_uri", s.logoUri)
        put("color", s.color)
        put("created_at", s.createdAt)
    }

    private fun recurringRuleToJson(r: RecurringRuleEntity) = JSONObject().apply {
        put("id", r.id)
        put("name", r.name)
        put("transaction_template", r.transactionTemplate)
        put("frequency", r.frequency.name)
        putNullable("day_of_period", r.dayOfPeriod)
        put("next_run_date", r.nextRunDate)
        putNullable("last_run_date", r.lastRunDate)
        put("auto_confirm", r.autoConfirm)
        put("is_active", r.isActive)
        put("created_at", r.createdAt)
    }

    private fun transactionToJson(t: TransactionEntity) = JSONObject().apply {
        put("id", t.id)
        put("type", t.type.name)
        put("amount", t.amount)
        put("currency", t.currency)
        put("date", t.date)
        put("description", t.description)
        putNullable("category_id", t.categoryId)
        putNullable("sub_category_id", t.subCategoryId)
        put("source_type", t.sourceType.name)
        putNullable("source_id", t.sourceId)
        putNullable("destination_type", t.destinationType?.name)
        putNullable("destination_id", t.destinationId)
        put("payment_app", t.paymentApp.name)
        putNullable("place", t.place)
        putNullable("latitude", t.latitude)
        putNullable("longitude", t.longitude)
        putNullable("receipt_uri", t.receiptUri)
        putNullable("notes", t.notes)
        putNullable("tax_section", t.taxSection)
        putNullable("recurring_rule_id", t.recurringRuleId)
        put("is_split", t.isSplit)
        putNullable("split_group_id", t.splitGroupId)
        put("source", t.source.name)
        put("created_at", t.createdAt)
        put("updated_at", t.updatedAt)
        put("excluded_from_expense_total", t.excludedFromExpenseTotal)
    }

    private fun transactionPersonToJson(x: TransactionPersonCrossRef) = JSONObject().apply {
        put("transaction_id", x.transactionId)
        put("person_id", x.personId)
    }

    private fun transactionTagToJson(x: TransactionTagCrossRef) = JSONObject().apply {
        put("transaction_id", x.transactionId)
        put("tag_id", x.tagId)
    }

    // ---- per-entity decoders ----

    private fun accountFromJson(o: JSONObject) = AccountEntity(
        id = o.getString("id"), name = o.getString("name"),
        type = enumValueOf<AccountType>(o.getString("type")),
        institution = o.stringOrNull("institution"),
        accountNumberLast4 = o.stringOrNull("account_number_last4"),
        openingBalance = o.getDouble("opening_balance"), currency = o.getString("currency"),
        icon = o.getString("icon"), color = o.getLong("color"),
        isArchived = o.getBoolean("is_archived"), displayOrder = o.getInt("display_order"),
        createdAt = o.getLong("created_at"),
    )

    private fun cardFromJson(o: JSONObject) = CardEntity(
        id = o.getString("id"), name = o.getString("name"),
        type = enumValueOf<CardType>(o.getString("type")), issuer = o.stringOrNull("issuer"),
        network = enumValueOf<CardNetwork>(o.getString("network")),
        cardNumberLast4 = o.stringOrNull("card_number_last4"),
        creditLimit = o.doubleOrNull("credit_limit"),
        statementDayOfMonth = o.intOrNull("statement_day_of_month"),
        dueDayOfMonth = o.intOrNull("due_day_of_month"),
        linkedAccountId = o.stringOrNull("linked_account_id"),
        icon = o.getString("icon"), color = o.getLong("color"),
        isArchived = o.getBoolean("is_archived"), displayOrder = o.getInt("display_order"),
        createdAt = o.getLong("created_at"),
    )

    private fun categoryFromJson(o: JSONObject) = CategoryEntity(
        id = o.getString("id"),
        name = o.getString("name"),
        parentId = o.stringOrNull("parent_id"),
        type = enumValueOf<CategoryType>(o.getString("type")),
        icon = o.getString("icon"),
        color = o.getLong("color"),
        isSystem = o.getBoolean("is_system"),
        displayOrder = o.getInt("display_order"),
    )

    private fun personFromJson(o: JSONObject) = PersonEntity(
        id = o.getString("id"),
        name = o.getString("name"),
        relation = enumValueOf<PersonRelation>(o.getString("relation")),
        contact = o.stringOrNull("contact"),
        avatarUri = o.stringOrNull("avatar_uri"),
        createdAt = o.getLong("created_at"),
    )

    private fun tagFromJson(o: JSONObject) = TagEntity(
        id = o.getString("id"),
        name = o.getString("name"),
        color = o.getLong("color"),
    )

    private fun investmentFromJson(o: JSONObject) = InvestmentEntity(
        id = o.getString("id"), name = o.getString("name"),
        type = enumValueOf<InvestmentType>(o.getString("type")),
        institution = o.stringOrNull("institution"),
        currentValue = o.getDouble("current_value"),
        valuationMode = enumValueOf<ValuationMode>(o.getString("valuation_mode")),
        openingContribution = o.getDouble("opening_contribution"),
        units = o.doubleOrNull("units"), nav = o.doubleOrNull("nav"),
        startDate = o.getLong("start_date"), maturityDate = o.longOrNull("maturity_date"),
        taxSection = o.stringOrNull("tax_section"),
        icon = o.getString("icon"), color = o.getLong("color"),
        linkedInsuranceId = o.stringOrNull("linked_insurance_id"),
        isArchived = o.getBoolean("is_archived"), displayOrder = o.getInt("display_order"),
        createdAt = o.getLong("created_at"),
    )

    private fun insuranceFromJson(o: JSONObject) = InsuranceEntity(
        id = o.getString("id"), name = o.getString("name"),
        type = enumValueOf<InsuranceType>(o.getString("type")),
        provider = o.getString("provider"), policyNumber = o.stringOrNull("policy_number"),
        sumAssured = o.getDouble("sum_assured"), premiumAmount = o.getDouble("premium_amount"),
        premiumFrequency = enumValueOf<PremiumFrequency>(o.getString("premium_frequency")),
        nextPremiumDate = o.longOrNull("next_premium_date"),
        startDate = o.getLong("start_date"), endDate = o.longOrNull("end_date"),
        nominee = o.stringOrNull("nominee"), agentContact = o.stringOrNull("agent_contact"),
        policyDocUri = o.stringOrNull("policy_doc_uri"), taxSection = o.stringOrNull("tax_section"),
        icon = o.getString("icon"), color = o.getLong("color"),
        isArchived = o.getBoolean("is_archived"), createdAt = o.getLong("created_at"),
    )

    private fun transactionRuleFromJson(o: JSONObject) = TransactionRuleEntity(
        id = o.getString("id"),
        name = o.getString("name"),
        conditionsJson = o.getString("conditions_json"),
        actionsJson = o.getString("actions_json"),
        priority = o.getInt("priority"),
        isActive = o.getBoolean("is_active"),
        isSystem = o.getBoolean("is_system"),
        createdAt = o.getLong("created_at"),
    )

    private fun budgetFromJson(o: JSONObject) = BudgetEntity(
        id = o.getString("id"), name = o.getString("name"),
        scope = enumValueOf<BudgetScope>(o.getString("scope")),
        categoryId = o.stringOrNull("category_id"), amount = o.getDouble("amount"),
        period = enumValueOf<BudgetPeriod>(o.getString("period")),
        startDate = o.getLong("start_date"),
        alertThresholdPercent = o.getInt("alert_threshold_percent"),
        isActive = o.getBoolean("is_active"), createdAt = o.getLong("created_at"),
    )

    private fun goalFromJson(o: JSONObject) = GoalEntity(
        id = o.getString("id"), name = o.getString("name"),
        targetAmount = o.getDouble("target_amount"), targetDate = o.longOrNull("target_date"),
        linkedAccountIdsJson = o.getString("linked_account_ids"),
        linkedInvestmentIdsJson = o.getString("linked_investment_ids"),
        icon = o.getString("icon"), color = o.getLong("color"),
        isAchieved = o.getBoolean("is_achieved"), createdAt = o.getLong("created_at"),
    )

    private fun subscriptionFromJson(o: JSONObject) = SubscriptionEntity(
        id = o.getString("id"), name = o.getString("name"), provider = o.stringOrNull("provider"),
        amount = o.getDouble("amount"),
        frequency = enumValueOf<SubscriptionFrequency>(o.getString("frequency")),
        nextDueDate = o.getLong("next_due_date"), lastPaidDate = o.longOrNull("last_paid_date"),
        categoryId = o.stringOrNull("category_id"),
        paymentMethodType = o.stringOrNull("payment_method_type"),
        paymentMethodId = o.stringOrNull("payment_method_id"),
        status = enumValueOf<SubscriptionStatus>(o.getString("status")),
        autoCharge = o.getBoolean("auto_charge"), logoUri = o.stringOrNull("logo_uri"),
        color = o.getLong("color"), createdAt = o.getLong("created_at"),
    )

    private fun recurringRuleFromJson(o: JSONObject) = RecurringRuleEntity(
        id = o.getString("id"), name = o.getString("name"),
        transactionTemplate = o.getString("transaction_template"),
        frequency = enumValueOf<RecurringFrequency>(o.getString("frequency")),
        dayOfPeriod = o.intOrNull("day_of_period"), nextRunDate = o.getLong("next_run_date"),
        lastRunDate = o.longOrNull("last_run_date"), autoConfirm = o.getBoolean("auto_confirm"),
        isActive = o.getBoolean("is_active"), createdAt = o.getLong("created_at"),
    )

    private fun transactionFromJson(o: JSONObject) = TransactionEntity(
        id = o.getString("id"), type = enumValueOf<TransactionType>(o.getString("type")),
        amount = o.getDouble("amount"), currency = o.getString("currency"),
        date = o.getLong("date"), description = o.getString("description"),
        categoryId = o.stringOrNull("category_id"), subCategoryId = o.stringOrNull("sub_category_id"),
        sourceType = enumValueOf<SourceKind>(o.getString("source_type")),
        sourceId = o.stringOrNull("source_id"),
        destinationType = o.stringOrNull("destination_type")?.let { enumValueOf<SourceKind>(it) },
        destinationId = o.stringOrNull("destination_id"),
        paymentApp = enumValueOf<PaymentApp>(o.getString("payment_app")),
        place = o.stringOrNull("place"), latitude = o.doubleOrNull("latitude"),
        longitude = o.doubleOrNull("longitude"), receiptUri = o.stringOrNull("receipt_uri"),
        notes = o.stringOrNull("notes"), taxSection = o.stringOrNull("tax_section"),
        recurringRuleId = o.stringOrNull("recurring_rule_id"),
        isSplit = o.getBoolean("is_split"), splitGroupId = o.stringOrNull("split_group_id"),
        source = enumValueOf<TransactionSource>(o.getString("source")),
        createdAt = o.getLong("created_at"), updatedAt = o.getLong("updated_at"),
        excludedFromExpenseTotal = o.optBoolean("excluded_from_expense_total", false),
    )

    private fun transactionPersonFromJson(o: JSONObject) = TransactionPersonCrossRef(
        transactionId = o.getString("transaction_id"),
        personId = o.getString("person_id"),
    )

    private fun transactionTagFromJson(o: JSONObject) = TransactionTagCrossRef(
        transactionId = o.getString("transaction_id"),
        tagId = o.getString("tag_id"),
    )

    // ---- JSON helpers ----

    private inline fun <T> List<T>.toArray(toJson: (T) -> JSONObject): JSONArray {
        val arr = JSONArray()
        for (item in this) arr.put(toJson(item))
        return arr
    }

    /** Reads a top-level array by [key], decoding each element. Missing key -> empty list. */
    private inline fun <T> JSONObject.read(key: String, fromJson: (JSONObject) -> T): List<T> {
        val arr = optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }

    /** Writes null as a real JSON null (org.json drops the key when given a Kotlin null,
     *  so we explicitly insert [JSONObject.NULL]). */
    private fun JSONObject.putNullable(key: String, value: Any?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else getString(key)

    private fun JSONObject.intOrNull(key: String): Int? = if (isNull(key)) null else getInt(key)

    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)

    private fun JSONObject.doubleOrNull(key: String): Double? = if (isNull(key)) null else getDouble(key)
}
