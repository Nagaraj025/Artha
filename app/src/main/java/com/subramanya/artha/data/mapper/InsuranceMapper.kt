package com.subramanya.artha.data.mapper

import com.subramanya.artha.data.entity.InsuranceEntity
import com.subramanya.artha.domain.model.Insurance

fun InsuranceEntity.toDomain(): Insurance = Insurance(
    id = id,
    name = name,
    type = type,
    provider = provider,
    policyNumber = policyNumber,
    sumAssured = sumAssured,
    premiumAmount = premiumAmount,
    premiumFrequency = premiumFrequency,
    nextPremiumDate = nextPremiumDate,
    startDate = startDate,
    endDate = endDate,
    nominee = nominee,
    agentContact = agentContact,
    policyDocUri = policyDocUri,
    taxSection = taxSection,
    icon = icon,
    color = color,
    isArchived = isArchived,
    createdAt = createdAt,
)

fun Insurance.toEntity(): InsuranceEntity = InsuranceEntity(
    id = id,
    name = name,
    type = type,
    provider = provider,
    policyNumber = policyNumber,
    sumAssured = sumAssured,
    premiumAmount = premiumAmount,
    premiumFrequency = premiumFrequency,
    nextPremiumDate = nextPremiumDate,
    startDate = startDate,
    endDate = endDate,
    nominee = nominee,
    agentContact = agentContact,
    policyDocUri = policyDocUri,
    taxSection = taxSection,
    icon = icon,
    color = color,
    isArchived = isArchived,
    createdAt = createdAt,
)
