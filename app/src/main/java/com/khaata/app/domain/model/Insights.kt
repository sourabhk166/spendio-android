package com.khaata.app.domain.model

/** One slice of the month's spending: a category, its total, and its share of the whole. */
data class CategorySlice(
    val categoryId: String,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val amount: Money,
    val fraction: Float, // 0..1 of total spend
)

object Insights {
    /** Builds sorted, percentage-weighted slices from category->total(paise) sums. */
    fun slices(totalsByCategory: Map<String, Long>): List<CategorySlice> {
        val grand = totalsByCategory.values.sum().coerceAtLeast(0)
        return totalsByCategory.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { (categoryId, total) ->
                val info = CategoryCatalog.info(categoryId)
                CategorySlice(
                    categoryId = categoryId,
                    name = info.name,
                    emoji = info.emoji,
                    colorHex = info.colorHex,
                    amount = Money(total),
                    fraction = if (grand > 0) total.toFloat() / grand else 0f,
                )
            }
    }
}
