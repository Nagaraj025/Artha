package com.subramanya.artha.domain.model

import com.subramanya.artha.data.entity.enums.CardNetwork
import com.subramanya.artha.data.entity.enums.CardType

data class Card(
    val id: String,
    val name: String,
    val type: CardType,
    val issuer: String?,
    val network: CardNetwork,
    val cardNumberLast4: String?,
    val creditLimit: Double?,
    val statementDayOfMonth: Int?,
    val dueDayOfMonth: Int?,
    val linkedAccountId: String?,
    val icon: String,
    val color: Long,
    val isArchived: Boolean,
    val displayOrder: Int,
    val createdAt: Long,
)

data class CardWithBalance(val card: Card, val currentOutstanding: Double)
