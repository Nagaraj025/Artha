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
