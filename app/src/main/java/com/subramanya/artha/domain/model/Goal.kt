package com.subramanya.artha.domain.model

data class Goal(
    val id: String,
    val name: String,
    val targetAmount: Double,
    val targetDate: Long?,
    val linkedAccountIds: List<String>,
    val linkedInvestmentIds: List<String>,
    val icon: String,
    val color: Long,
    val isAchieved: Boolean,
    val createdAt: Long,
)

data class GoalWithProgress(val goal: Goal, val currentAmount: Double, val percentDone: Double, val daysRemaining: Int?)
