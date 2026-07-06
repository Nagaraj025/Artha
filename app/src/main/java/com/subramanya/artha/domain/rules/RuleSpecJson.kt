package com.subramanya.artha.domain.rules

import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.PersonRelation
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Hand-rolled JSON codec so we don't drag in kotlinx-serialization just for the rule
 * blobs. Schema is tagged-union: each item carries a `kind` discriminator.
 *
 * Forward compatibility: unknown `kind` values are dropped silently during decode;
 * never throw — a corrupted rule should disable itself, not crash the app.
 */
object RuleSpecJson {

    // ---- encode ----

    fun encodeConditions(c: RuleConditions): String {
        val arr = JSONArray()
        c.items.forEach { arr.put(encodeCondition(it)) }
        return JSONObject().apply {
            put("logic", c.logic.name)
            put("items", arr)
        }.toString()
    }

    fun encodeActions(a: RuleActions): String {
        val arr = JSONArray()
        a.items.forEach { arr.put(encodeAction(it)) }
        return JSONObject().apply { put("items", arr) }.toString()
    }

    private fun encodeCondition(c: RuleCondition): JSONObject = when (c) {
        is RuleCondition.DescriptionContains -> JSONObject().apply {
            put("kind", "DescriptionContains")
            put("text", c.text)
            put("ignoreCase", c.ignoreCase)
        }
        is RuleCondition.AmountCompare -> JSONObject().apply {
            put("kind", "AmountCompare")
            put("op", c.op.name)
            put("value", c.value)
        }
        is RuleCondition.SourceIs -> JSONObject().apply {
            put("kind", "SourceIs")
            put("source_kind", c.kind.name)
            put("id", c.id)
        }
        is RuleCondition.DestinationIs -> JSONObject().apply {
            put("kind", "DestinationIs")
            put("source_kind", c.kind.name)
            put("id", c.id)
        }
        is RuleCondition.PaymentAppIs -> JSONObject().apply {
            put("kind", "PaymentAppIs")
            put("app", c.app.name)
        }
        is RuleCondition.TypeIs -> JSONObject().apply {
            put("kind", "TypeIs")
            put("type", c.type.name)
        }
        is RuleCondition.HasPersonRelation -> JSONObject().apply {
            put("kind", "HasPersonRelation")
            put("relation", c.relation.name)
        }
        is RuleCondition.TimeOfDayBetween -> JSONObject().apply {
            put("kind", "TimeOfDayBetween")
            put("from", c.fromMinuteOfDay)
            put("to", c.toMinuteOfDay)
        }
    }

    private fun encodeAction(a: RuleAction): JSONObject = when (a) {
        is RuleAction.SetType -> JSONObject().apply {
            put("kind", "SetType")
            put("type", a.type.name)
        }
        is RuleAction.SetCategory -> JSONObject().apply {
            put("kind", "SetCategory")
            put("category_id", a.categoryId)
            put("sub_category_id", a.subCategoryId)
        }
        is RuleAction.SetTaxSection -> JSONObject().apply {
            put("kind", "SetTaxSection")
            put("section", a.section)
        }
        is RuleAction.AddTag -> JSONObject().apply {
            put("kind", "AddTag")
            put("tag_id", a.tagId)
        }
        is RuleAction.AddPerson -> JSONObject().apply {
            put("kind", "AddPerson")
            put("person_id", a.personId)
        }
        RuleAction.ExcludeFromExpenseTotal -> JSONObject().apply { put("kind", "ExcludeFromExpenseTotal") }
        RuleAction.PromptSpouse -> JSONObject().apply { put("kind", "PromptSpouse") }
    }

    // ---- decode ----

    fun decodeConditions(raw: String): RuleConditions = runCatching {
        val root = JSONObject(raw)
        val logic = runCatching { ConditionLogic.valueOf(root.optString("logic", "ALL")) }
            .getOrDefault(ConditionLogic.ALL)
        val items = (root.optJSONArray("items") ?: JSONArray()).let { arr ->
            buildList {
                for (i in 0 until arr.length()) {
                    decodeCondition(arr.getJSONObject(i))?.let { add(it) }
                }
            }
        }
        RuleConditions(logic, items)
    }.getOrDefault(RuleConditions())

    fun decodeActions(raw: String): RuleActions = runCatching {
        val root = JSONObject(raw)
        val items = (root.optJSONArray("items") ?: JSONArray()).let { arr ->
            buildList {
                for (i in 0 until arr.length()) {
                    decodeAction(arr.getJSONObject(i))?.let { add(it) }
                }
            }
        }
        RuleActions(items)
    }.getOrDefault(RuleActions())

    private fun decodeCondition(o: JSONObject): RuleCondition? = runCatching {
        when (o.optString("kind")) {
            "DescriptionContains" -> RuleCondition.DescriptionContains(
                text = o.getString("text"),
                ignoreCase = o.optBoolean("ignoreCase", true),
            )
            "AmountCompare" -> RuleCondition.AmountCompare(
                op = AmountOp.valueOf(o.getString("op")),
                value = o.getDouble("value"),
            )
            "SourceIs" -> RuleCondition.SourceIs(
                kind = SourceKind.valueOf(o.getString("source_kind")),
                id = o.optString("id").takeIf { it.isNotBlank() && it != "null" },
            )
            "DestinationIs" -> RuleCondition.DestinationIs(
                kind = SourceKind.valueOf(o.getString("source_kind")),
                id = o.optString("id").takeIf { it.isNotBlank() && it != "null" },
            )
            "PaymentAppIs" -> RuleCondition.PaymentAppIs(PaymentApp.valueOf(o.getString("app")))
            "TypeIs" -> RuleCondition.TypeIs(TransactionType.valueOf(o.getString("type")))
            "HasPersonRelation" -> RuleCondition.HasPersonRelation(PersonRelation.valueOf(o.getString("relation")))
            "TimeOfDayBetween" -> RuleCondition.TimeOfDayBetween(o.getInt("from"), o.getInt("to"))
            else -> null
        }
    }.getOrNull()

    private fun decodeAction(o: JSONObject): RuleAction? = runCatching {
        when (o.optString("kind")) {
            "SetType" -> RuleAction.SetType(TransactionType.valueOf(o.getString("type")))
            "SetCategory" -> RuleAction.SetCategory(
                categoryId = o.getString("category_id"),
                subCategoryId = o.optString("sub_category_id").takeIf { it.isNotBlank() && it != "null" },
            )
            "SetTaxSection" -> RuleAction.SetTaxSection(o.getString("section"))
            "AddTag" -> RuleAction.AddTag(o.getString("tag_id"))
            "AddPerson" -> RuleAction.AddPerson(o.getString("person_id"))
            "ExcludeFromExpenseTotal" -> RuleAction.ExcludeFromExpenseTotal
            "PromptSpouse" -> RuleAction.PromptSpouse
            else -> null
        }
    }.getOrNull()
}
