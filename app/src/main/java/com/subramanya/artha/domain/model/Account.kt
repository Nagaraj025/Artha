package com.subramanya.artha.domain.model

import com.subramanya.artha.data.entity.enums.AccountType

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val institution: String?,
    val accountNumberLast4: String?,
    val openingBalance: Double,
    val currency: String,
    val icon: String,
    val color: Long,
    val isArchived: Boolean,
    val displayOrder: Int,
    val createdAt: Long,
)

data class AccountWithBalance(val account: Account, val currentBalance: Double)
