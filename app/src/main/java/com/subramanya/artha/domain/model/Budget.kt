package com.subramanya.artha.domain.model

import com.subramanya.artha.data.entity.enums.BudgetPeriod
import com.subramanya.artha.data.entity.enums.BudgetScope

data class Budget(
    val id: String,
    val name: String,
    val scope: BudgetScope,
    val categoryId: String?,
    val amount: Double,
    val period: BudgetPeriod,
    val startDate: Long,
    val alertThresholdPercent: Int,
    val isActive: Boolean,
    val createdAt: Long,
)

/** Domain view that pairs the budget with this-period spend + days remaining. */
data class BudgetWithProgress(val budget: Budget, val spent: Double, val daysRemainingInPeriod: Int)
