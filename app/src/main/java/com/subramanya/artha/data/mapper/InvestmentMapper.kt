package com.subramanya.artha.data.mapper

import com.subramanya.artha.data.entity.InvestmentEntity
import com.subramanya.artha.domain.model.Investment

fun InvestmentEntity.toDomain(): Investment = Investment(
    id = id,
    name = name,
    type = type,
    institution = institution,
    currentValue = currentValue,
    valuationMode = valuationMode,
    openingContribution = openingContribution,
    units = units,
    nav = nav,
    startDate = startDate,
    maturityDate = maturityDate,
    taxSection = taxSection,
    icon = icon,
    color = color,
    linkedInsuranceId = linkedInsuranceId,
    isArchived = isArchived,
    displayOrder = displayOrder,
    createdAt = createdAt,
)

fun Investment.toEntity(): InvestmentEntity = InvestmentEntity(
    id = id,
    name = name,
    type = type,
    institution = institution,
    currentValue = currentValue,
    valuationMode = valuationMode,
    openingContribution = openingContribution,
    units = units,
    nav = nav,
    startDate = startDate,
    maturityDate = maturityDate,
    taxSection = taxSection,
    icon = icon,
    color = color,
    linkedInsuranceId = linkedInsuranceId,
    isArchived = isArchived,
    displayOrder = displayOrder,
    createdAt = createdAt,
)
