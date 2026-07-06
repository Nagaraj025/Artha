package com.subramanya.artha.data.mapper

import com.subramanya.artha.data.entity.PendingSmsTransactionEntity
import com.subramanya.artha.domain.model.SmsDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class PendingSmsTransactionMapperTest {

    @Test
    fun `entity round-trips through domain and back unchanged`() {
        val entity = PendingSmsTransactionEntity(
            id = "p1",
            rawSmsBody = "Rs.500 debited from A/c XX1234 at SWIGGY",
            sender = "HDFCBK",
            receivedAt = 1_700_000_000_000L,
            direction = "DEBIT",
            amount = 500.0,
            accountHint = "1234",
            merchant = "SWIGGY",
            suggestedCategoryId = null,
        )

        val domain = entity.toDomain()
        assertEquals(SmsDirection.DEBIT, domain.direction)
        assertEquals("SWIGGY", domain.merchant)

        val roundTripped = domain.toEntity()
        assertEquals(entity, roundTripped)
    }
}
