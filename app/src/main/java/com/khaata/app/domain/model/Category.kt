package com.khaata.app.domain.model

/**
 * System categories. In Phase 1 every captured transaction lands in [UNCATEGORIZED];
 * automatic categorization (merchant -> category) arrives in Phase 2 with the AI layer.
 */
object Categories {
    const val UNCATEGORIZED = "uncategorized"
    const val FOOD = "food"
    const val GROCERIES = "groceries"
    const val SHOPPING = "shopping"
    const val TRANSPORT = "transport"
    const val BILLS = "bills"
    const val ENTERTAINMENT = "entertainment"
    const val HEALTH = "health"
    const val TRANSFERS = "transfers"
    const val INCOME = "income"
    const val OTHER = "other"

    /** The closed set the AI classifier is allowed to choose from. */
    val ALL: List<String> = listOf(
        FOOD, GROCERIES, SHOPPING, TRANSPORT, BILLS, ENTERTAINMENT,
        HEALTH, TRANSFERS, INCOME, OTHER,
    )
}
