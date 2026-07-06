package com.subramanya.artha.data.repository

import com.subramanya.artha.data.dao.SubscriptionDao
import com.subramanya.artha.data.entity.enums.SubscriptionFrequency
import com.subramanya.artha.data.mapper.toDomain
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.domain.model.Subscription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SubscriptionRepository(private val subscriptionDao: SubscriptionDao) {
    fun observeAll(): Flow<List<Subscription>> = subscriptionDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActive(): Flow<List<Subscription>> = subscriptionDao.observeActive().map { list -> list.map { it.toDomain() } }

    suspend fun upsert(subscription: Subscription) = subscriptionDao.upsert(subscription.toEntity())
    suspend fun delete(subscription: Subscription) = subscriptionDao.delete(subscription.toEntity())

    /** Annualised total of all active subscriptions — drives the hero card. */
    fun annualisedMonthlyAverage(active: List<Subscription>): Double = active.sumOf {
        when (it.frequency) {
            SubscriptionFrequency.MONTHLY -> it.amount
            SubscriptionFrequency.QUARTERLY -> it.amount / 3.0
            SubscriptionFrequency.YEARLY -> it.amount / 12.0
        }
    }
}
