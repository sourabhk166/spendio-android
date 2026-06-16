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
        CategoryInfo(Categories.INVESTMENTS, "Investments", "📈", "#5C6BC0"),
        CategoryInfo(Categories.TRANSFERS, "Transfers", "🔁", "#78909C"),
        CategoryInfo(Categories.INCOME, "Other Income", "💰", "#9CCC65"),
        CategoryInfo(Categories.INCOME_SALARY, "Salary", "💼", "#43A047"),
        CategoryInfo(Categories.INCOME_FREELANCE, "Freelance", "💻", "#00ACC1"),
        CategoryInfo(Categories.INCOME_INTEREST, "Interest", "🏦", "#7CB342"),
        CategoryInfo(Categories.INCOME_REFUND, "Refund", "↩️", "#F4511E"),
        CategoryInfo(Categories.INCOME_GIFT, "Gift", "🎁", "#E91E63"),
        CategoryInfo(Categories.INCOME_CASHBACK, "Cashback", "💸", "#0097A7"),
        CategoryInfo(Categories.OTHER, "Other", "📌", "#8D6E63"),
        CategoryInfo(Categories.UNCATEGORIZED, "Uncategorized", "🧾", "#9E9E9E"),
    )

    private val byId: Map<String, CategoryInfo> = all.associateBy { it.id }

    fun info(id: String): CategoryInfo = byId[id] ?: byId.getValue(Categories.UNCATEGORIZED)

    /**
     * Categories shown in the review screen and the DEBIT (expense) picker.
     * Excludes UNCATEGORIZED sentinel and all income sub-types.
     */
    val selectable: List<CategoryInfo> = all.filter {
        it.id != Categories.UNCATEGORIZED && it.id !in Categories.INCOME_SUBS
    }

    /** Income sub-types shown in the editor when direction = CREDIT. */
    val incomeCategories: List<CategoryInfo> = listOf(
        info(Categories.INCOME_SALARY),
        info(Categories.INCOME_FREELANCE),
        info(Categories.INCOME_INTEREST),
        info(Categories.INCOME_REFUND),
        info(Categories.INCOME_GIFT),
        info(Categories.INCOME_CASHBACK),
        info(Categories.INCOME), // "Other Income" catch-all at the bottom
    )
}
