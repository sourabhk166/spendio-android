package com.spendio.app.domain.model

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
    const val INVESTMENTS = "investments"
    const val TRANSFERS = "transfers"
    const val UPI = "upi"
    const val INCOME = "income"
    const val INCOME_SALARY = "income_salary"
    const val INCOME_FREELANCE = "income_freelance"
    const val INCOME_INTEREST = "income_interest"
    const val INCOME_REFUND = "income_refund"
    const val INCOME_GIFT = "income_gift"
    const val INCOME_CASHBACK = "income_cashback"
    const val OTHER = "other"

    /** Income sub-category IDs (named sub-types under the income umbrella). */
    val INCOME_SUBS: Set<String> = setOf(
        INCOME_SALARY, INCOME_FREELANCE, INCOME_INTEREST,
        INCOME_REFUND, INCOME_GIFT, INCOME_CASHBACK,
    )

    /** All income-family IDs — parent + all sub-types. */
    val INCOME_ALL: Set<String> = INCOME_SUBS + INCOME

    /** The closed set the AI classifier is allowed to choose from. */
    val ALL: List<String> = listOf(
        FOOD, GROCERIES, SHOPPING, TRANSPORT, BILLS, ENTERTAINMENT,
        HEALTH, INVESTMENTS, TRANSFERS, INCOME, OTHER,
    )

    /**
     * Categories that move money rather than consume it, so they must NOT count as spending:
     *  - Transfers: money between your own accounts (self-transfer) — no net spend
     *  - Investments: SIPs / brokerage deposits build wealth, they aren't an expense
     * Note UPI (person-to-person payments) deliberately DOES count as spending — most are real
     * payments to vendors/people; only genuine self-transfers are filtered out (to Transfers).
     */
    val NON_EXPENSE: List<String> = listOf(TRANSFERS, INVESTMENTS)

    /**
     * Categories excluded from the "Received" total:
     *  - Transfers: an incoming self-transfer isn't income
     *  - UPI: money received from a person (repayment/gift) isn't salary-style income
     */
    val NON_INCOME: List<String> = listOf(TRANSFERS, UPI)
}
