package com.subramanya.artha.data.mapper

import com.subramanya.artha.data.entity.TransactionRuleEntity
import com.subramanya.artha.domain.model.TransactionRule
import com.subramanya.artha.domain.rules.RuleSpecJson

fun TransactionRuleEntity.toDomain(): TransactionRule = TransactionRule(
    id = id,
    name = name,
    conditions = RuleSpecJson.decodeConditions(conditionsJson),
    actions = RuleSpecJson.decodeActions(actionsJson),
    priority = priority,
    isActive = isActive,
    isSystem = isSystem,
    createdAt = createdAt,
)

fun TransactionRule.toEntity(): TransactionRuleEntity = TransactionRuleEntity(
    id = id,
    name = name,
    conditionsJson = RuleSpecJson.encodeConditions(conditions),
    actionsJson = RuleSpecJson.encodeActions(actions),
    priority = priority,
    isActive = isActive,
    isSystem = isSystem,
    createdAt = createdAt,
)
