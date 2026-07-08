package com.subramanya.artha.ui.transactions

import com.subramanya.artha.data.entity.enums.CategoryType
import com.subramanya.artha.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the ledger category-filter completeness bug: the filter must offer EVERY category
 * (parents AND sub-categories), never silently drop children.
 */
class CategoryFilterFlattenTest {

    private fun cat(id: String, parentId: String?, order: Int): Category = Category(
        id = id,
        name = id,
        parentId = parentId,
        type = CategoryType.EXPENSE,
        icon = "x",
        color = 0L,
        isSystem = false,
        displayOrder = order,
    )

    @Test
    fun `includes every category — parents and sub-categories`() {
        val categories = listOf(
            cat("food", null, 0),
            cat("groceries", "food", 0),
            cat("dining", "food", 1),
            cat("travel", null, 1),
            cat("fuel", "travel", 0),
        )
        val rows = flattenCategoryFilterRows(categories)
        assertEquals(5, rows.size)
        assertEquals(categories.map { it.id }.toSet(), rows.map { it.category.id }.toSet())
    }

    @Test
    fun `each sub-category is marked as a child and appears directly after its parent`() {
        val categories = listOf(
            cat("food", null, 0),
            cat("groceries", "food", 0),
        )
        val rows = flattenCategoryFilterRows(categories)
        assertEquals("food", rows[0].category.id)
        assertFalse(rows[0].isChild)
        assertEquals("groceries", rows[1].category.id)
        assertTrue(rows[1].isChild)
    }

    @Test
    fun `children are ordered by displayOrder under their parent`() {
        val categories = listOf(
            cat("food", null, 0),
            cat("dining", "food", 1),
            cat("groceries", "food", 0),
        )
        val rows = flattenCategoryFilterRows(categories)
        assertEquals(listOf("food", "groceries", "dining"), rows.map { it.category.id })
    }

    @Test
    fun `an orphan sub-category whose parent is absent is still included`() {
        val categories = listOf(cat("orphan", "missing-parent", 0))
        val rows = flattenCategoryFilterRows(categories)
        assertEquals(1, rows.size)
        assertEquals("orphan", rows[0].category.id)
    }
}
