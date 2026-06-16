package com.khaata.app.domain.model

import kotlin.math.floor

/** One slice of the month's spending: a category, its total, its share, and a whole-number %. */
data class CategorySlice(
    val categoryId: String,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val amount: Money,
    val fraction: Float, // 0..1 of total
    val percent: Int,    // whole-number %, slices always sum to exactly 100
)

object Insights {
    /**
     * Builds sorted, percentage-weighted slices from category->total(paise) sums. Percentages
     * use the largest-remainder method so the displayed whole numbers always sum to exactly
     * 100 (naively truncating each slice loses the last 1–2% — the "98%" bug).
     */
    fun slices(totalsByCategory: Map<String, Long>): List<CategorySlice> {
        val positive = totalsByCategory.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
        val grand = positive.sumOf { it.value }
        if (grand <= 0L) return emptyList()

        val exact = positive.map { it.value * 100.0 / grand }
        val floors = exact.map { floor(it).toInt() }.toMutableList()
        var leftover = 100 - floors.sum()
        // Hand the remaining whole percent points to the slices with the largest fractional part.
        val byRemainder = exact.indices.sortedByDescending { exact[it] - floor(exact[it]) }
        var i = 0
        while (leftover > 0 && i < byRemainder.size) {
            floors[byRemainder[i]]++
            leftover--
            i++
        }

        return positive.mapIndexed { idx, (categoryId, total) ->
            val info = CategoryCatalog.info(categoryId)
            CategorySlice(
                categoryId = categoryId,
                name = info.name,
                emoji = info.emoji,
                colorHex = info.colorHex,
                amount = Money(total),
                fraction = total.toFloat() / grand,
                percent = floors[idx],
            )
        }
    }
}
