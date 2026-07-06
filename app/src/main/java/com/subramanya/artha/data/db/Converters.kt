package com.subramanya.artha.data.db

import androidx.room.TypeConverter
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

class Converters {
    @TypeConverter fun fromAccountType(v: AccountType): String = v.name

    @TypeConverter fun toAccountType(v: String): AccountType = AccountType.valueOf(v)

    @TypeConverter fun fromCardType(v: CardType): String = v.name

    @TypeConverter fun toCardType(v: String): CardType = CardType.valueOf(v)

    @TypeConverter fun fromCardNetwork(v: CardNetwork): String = v.name

    @TypeConverter fun toCardNetwork(v: String): CardNetwork = CardNetwork.valueOf(v)

    @TypeConverter fun fromCategoryType(v: CategoryType): String = v.name

    @TypeConverter fun toCategoryType(v: String): CategoryType = CategoryType.valueOf(v)

    @TypeConverter fun fromPaymentApp(v: PaymentApp): String = v.name

    @TypeConverter fun toPaymentApp(v: String): PaymentApp = PaymentApp.valueOf(v)

    @TypeConverter fun fromPersonRelation(v: PersonRelation): String = v.name

    @TypeConverter fun toPersonRelation(v: String): PersonRelation = PersonRelation.valueOf(v)

    @TypeConverter fun fromSourceKind(v: SourceKind): String = v.name

    @TypeConverter fun toSourceKind(v: String): SourceKind = SourceKind.valueOf(v)

    @TypeConverter fun fromSourceKindNullable(v: SourceKind?): String? = v?.name

    @TypeConverter fun toSourceKindNullable(v: String?): SourceKind? = v?.let(SourceKind::valueOf)

    @TypeConverter fun fromTransactionType(v: TransactionType): String = v.name

    @TypeConverter fun toTransactionType(v: String): TransactionType = TransactionType.valueOf(v)

    @TypeConverter fun fromTransactionSource(v: TransactionSource): String = v.name

    @TypeConverter fun toTransactionSource(v: String): TransactionSource = TransactionSource.valueOf(v)

    @TypeConverter fun fromInvestmentType(v: InvestmentType): String = v.name

    @TypeConverter fun toInvestmentType(v: String): InvestmentType = InvestmentType.valueOf(v)

    @TypeConverter fun fromValuationMode(v: ValuationMode): String = v.name

    @TypeConverter fun toValuationMode(v: String): ValuationMode = ValuationMode.valueOf(v)

    @TypeConverter fun fromInsuranceType(v: InsuranceType): String = v.name

    @TypeConverter fun toInsuranceType(v: String): InsuranceType = InsuranceType.valueOf(v)

    @TypeConverter fun fromPremiumFrequency(v: PremiumFrequency): String = v.name

    @TypeConverter fun toPremiumFrequency(v: String): PremiumFrequency = PremiumFrequency.valueOf(v)

    @TypeConverter fun fromBudgetScope(v: BudgetScope): String = v.name

    @TypeConverter fun toBudgetScope(v: String): BudgetScope = BudgetScope.valueOf(v)

    @TypeConverter fun fromBudgetPeriod(v: BudgetPeriod): String = v.name

    @TypeConverter fun toBudgetPeriod(v: String): BudgetPeriod = BudgetPeriod.valueOf(v)

    @TypeConverter fun fromSubscriptionFrequency(v: SubscriptionFrequency): String = v.name

    @TypeConverter fun toSubscriptionFrequency(v: String): SubscriptionFrequency = SubscriptionFrequency.valueOf(v)

    @TypeConverter fun fromSubscriptionStatus(v: SubscriptionStatus): String = v.name

    @TypeConverter fun toSubscriptionStatus(v: String): SubscriptionStatus = SubscriptionStatus.valueOf(v)

    @TypeConverter fun fromRecurringFrequency(v: RecurringFrequency): String = v.name

    @TypeConverter fun toRecurringFrequency(v: String): RecurringFrequency = RecurringFrequency.valueOf(v)
}
