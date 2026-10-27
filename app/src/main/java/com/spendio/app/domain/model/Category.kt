package com.spendio.app.domain.model

/**
 * System categories. Captured transactions are assigned one by the Categorizer
 * (merchant rules, keywords and learned user corrections); unresolved ones land in [UNCATEGORIZED].
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
    const val SELF_TRANSFER = "self_transfer"
    const val UPI = "upi"
    const val INCOME = "income"
    const val INCOME_SALARY = "income_salary"
    const val INCOME_FREELANCE = "income_freelance"
    const val INCOME_INTEREST = "income_interest"
    const val INCOME_REFUND = "income_refund"
    const val INCOME_GIFT = "income_gift"
    const val INCOME_CASHBACK = "income_cashback"
    const val INCOME_PAYBACK = "income_payback"
    const val OTHER = "other"

    /** Income sub-category IDs (named sub-types under the income umbrella). */
    val INCOME_SUBS: Set<String> = setOf(
        INCOME_SALARY, INCOME_FREELANCE, INCOME_INTEREST,
        INCOME_REFUND, INCOME_GIFT, INCOME_CASHBACK, INCOME_PAYBACK,
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
     *  - Self Transfer: money between your own accounts — no net spend
     *  - Investments: SIPs / brokerage deposits build wealth, they aren't an expense
     * Note Transfers and UPI (person-to-person payments) deliberately DO count as spending —
     * most are real payments to vendors/people; only genuine self-transfers are filtered out.
     */
    val NON_EXPENSE: List<String> = listOf(SELF_TRANSFER, INVESTMENTS)

    /**
     * Categories excluded from the "Received" total:
     *  - Self Transfer: an incoming self-transfer isn't income
     *  - Transfers / UPI: money received from a person via an unconfirmed or P2P route isn't
     *    salary-style income (genuine incoming repayments land in Payback, which IS counted)
     */
    val NON_INCOME: List<String> = listOf(SELF_TRANSFER, TRANSFERS, UPI)
}
