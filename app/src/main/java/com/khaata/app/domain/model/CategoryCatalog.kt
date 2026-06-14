package com.khaata.app.domain.model

/** Display metadata for a category. Kept in code (not the DB) for the fixed system set. */
data class CategoryInfo(
    val id: String,
    val name: String,
    val emoji: String,
    val colorHex: String,
)

object CategoryCatalog {
    private val all: List<CategoryInfo> = listOf(
        CategoryInfo(Categories.FOOD, "Food & Dining", "🍔", "#FF7043"),
        CategoryInfo(Categories.GROCERIES, "Groceries", "🛒", "#66BB6A"),
        CategoryInfo(Categories.SHOPPING, "Shopping", "🛍️", "#AB47BC"),
        CategoryInfo(Categories.TRANSPORT, "Transport", "🚕", "#42A5F5"),
        CategoryInfo(Categories.BILLS, "Bills & Utilities", "💡", "#FFA726"),
        CategoryInfo(Categories.ENTERTAINMENT, "Entertainment", "🎬", "#EC407A"),
        CategoryInfo(Categories.HEALTH, "Health", "💊", "#26A69A"),
        CategoryInfo(Categories.TRANSFERS, "Transfers", "🔁", "#78909C"),
        CategoryInfo(Categories.INCOME, "Income", "💰", "#9CCC65"),
        CategoryInfo(Categories.OTHER, "Other", "📌", "#8D6E63"),
        CategoryInfo(Categories.UNCATEGORIZED, "Uncategorized", "❓", "#9E9E9E"),
    )

    private val byId: Map<String, CategoryInfo> = all.associateBy { it.id }

    fun info(id: String): CategoryInfo = byId[id] ?: byId.getValue(Categories.UNCATEGORIZED)

    /** Categories a user may pick when correcting (excludes the Uncategorized sentinel). */
    val selectable: List<CategoryInfo> = all.filter { it.id != Categories.UNCATEGORIZED }
}
