package com.khaata.app.domain

import com.khaata.app.domain.model.Categories
import com.khaata.app.domain.model.CategoryCatalog
import com.khaata.app.domain.model.Insights
import com.khaata.app.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsTest {

    @Test fun `slices are sorted by amount with correct fractions`() {
        val slices = Insights.slices(
            mapOf(Categories.FOOD to 6000L, Categories.TRANSPORT to 4000L),
        )
        assertEquals(listOf(Categories.FOOD, Categories.TRANSPORT), slices.map { it.categoryId })
        assertEquals(Money(6000), slices[0].amount)
        assertEquals(0.6f, slices[0].fraction, 0.001f)
        assertEquals(0.4f, slices[1].fraction, 0.001f)
        assertEquals("Food & Dining", slices[0].name)
    }

    @Test fun `percentages always sum to exactly 100`() {
        // The classic truncation trap: 1/3 + 1/3 + 1/3 naively floors to 33+33+33 = 99.
        val thirds = Insights.slices(
            mapOf(Categories.FOOD to 100L, Categories.TRANSPORT to 100L, Categories.SHOPPING to 100L),
        )
        assertEquals(100, thirds.sumOf { it.percent })

        // The reported case: 283 + 50 + 1 of 334 would truncate to 84 + 14 + 0 = 98.
        val reported = Insights.slices(
            mapOf(Categories.FOOD to 28300L, Categories.TRANSPORT to 5000L, Categories.UNCATEGORIZED to 100L),
        )
        assertEquals(100, reported.sumOf { it.percent })
    }

    @Test fun `zero and negative totals are excluded`() {
        val slices = Insights.slices(mapOf(Categories.FOOD to 100L, Categories.SHOPPING to 0L))
        assertEquals(1, slices.size)
        assertEquals(Categories.FOOD, slices[0].categoryId)
    }

    @Test fun `empty input yields no slices`() {
        assertTrue(Insights.slices(emptyMap()).isEmpty())
    }

    @Test fun `unknown category falls back to uncategorized info`() {
        val info = CategoryCatalog.info("totally-unknown-id")
        assertEquals(Categories.UNCATEGORIZED, info.id)
    }

    @Test fun `selectable categories exclude the uncategorized sentinel`() {
        assertFalse(CategoryCatalog.selectable.any { it.id == Categories.UNCATEGORIZED })
    }
}
