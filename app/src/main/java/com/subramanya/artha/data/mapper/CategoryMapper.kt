package com.subramanya.artha.data.mapper

import com.subramanya.artha.data.entity.CategoryEntity
import com.subramanya.artha.domain.model.Category

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    parentId = parentId,
    type = type,
    icon = icon,
    color = color,
    isSystem = isSystem,
    displayOrder = displayOrder,
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    parentId = parentId,
    type = type,
    icon = icon,
    color = color,
    isSystem = isSystem,
    displayOrder = displayOrder,
)
