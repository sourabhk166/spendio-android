package com.khaata.app.parser

import com.khaata.app.domain.model.Direction

/**
 * A Tier-1 extraction template. The [pattern] must capture a named group `amount`;
 * it may optionally capture `merchant`, `acct`, and `ref`. Any field the template
 * does not capture is filled by the shared extractors (see RegexTierParser).
 *
 * In Phase 2 these same shapes are *learned* from AI parses and cached in the
 * parse_templates table; the bundled seed set below bootstraps the common formats.
 */
data class NotificationTemplate(
    val id: String,
    val direction: Direction,
    val pattern: Regex,
    val packages: Set<String> = emptySet(), // empty = applies to any source
    val confidence: Float = 0.95f,
)

/**
 * Bundled seed templates for the most common Indian bank/UPI notification shapes:
 * "<amount> debited/credited/spent/received/paid to ...". These anchor the amount
 * directly against the direction verb, which is high-precision; merchant/account/ref
 * are filled by the shared extractors. Expand this set from your real corpus in Phase 1.
 */
object SeedTemplates {
    private const val AMOUNT = """(?:rs\.?|inr|₹)\s?(?<amount>[0-9][0-9,]*(?:\.[0-9]{1,2})?)"""

    val ALL: List<NotificationTemplate> = listOf(
        NotificationTemplate(
            id = "paid_to",
            direction = Direction.DEBIT,
            pattern = Regex(
                """$AMOUNT\s+paid\s+to\s+(?<merchant>[A-Za-z0-9 ._&'@*-]{2,40}?)(?=\s+(?:via|on|using|ref|upi)\b|[.,;]|$)""",
                RegexOption.IGNORE_CASE,
            ),
        ),
        NotificationTemplate(
            id = "debited",
            direction = Direction.DEBIT,
            pattern = Regex("""$AMOUNT\s+(?:has\s+been\s+|is\s+|was\s+)?debited""", RegexOption.IGNORE_CASE),
        ),
        NotificationTemplate(
            id = "spent",
            direction = Direction.DEBIT,
            pattern = Regex("""$AMOUNT\s+(?:has\s+been\s+|was\s+)?spent""", RegexOption.IGNORE_CASE),
        ),
        NotificationTemplate(
            // verb-BEFORE-amount: HDFC/Axis/SBI cards — "Spent Rs.222 On …" / "Spent INR 170"
            id = "spent_prefix",
            direction = Direction.DEBIT,
            pattern = Regex("""\bspent\b\s+$AMOUNT""", RegexOption.IGNORE_CASE),
        ),
        NotificationTemplate(
            // verb-less HDFC debit: "Rs.625.00 From HDFC Bank A/C *8803 To FRZ.COM … On …"
            id = "from_acct_to",
            direction = Direction.DEBIT,
            pattern = Regex(
                """$AMOUNT\s+from\s+.{0,40}?a/?c\b.{0,20}?\bto\b\s+(?<merchant>[A-Za-z0-9 ._&'@*-]{2,40}?)(?=\s+on\b|[.,;]|$)""",
                RegexOption.IGNORE_CASE,
            ),
        ),
        NotificationTemplate(
            id = "debit_prefix",
            direction = Direction.DEBIT,
            pattern = Regex("""\bdebit(?:ed)?\b.{0,20}?$AMOUNT""", RegexOption.IGNORE_CASE),
        ),
        NotificationTemplate(
            // HDFC UPI SMS shape: "Sent Rs.100.00 From HDFC Bank A/C *8803 To <name>"
            id = "sent_prefix",
            direction = Direction.DEBIT,
            pattern = Regex("""\bsent\b\s+$AMOUNT""", RegexOption.IGNORE_CASE),
        ),
        NotificationTemplate(
            id = "credited",
            direction = Direction.CREDIT,
            pattern = Regex("""$AMOUNT\s+(?:has\s+been\s+|is\s+|was\s+)?credited""", RegexOption.IGNORE_CASE),
        ),
        NotificationTemplate(
            id = "received",
            direction = Direction.CREDIT,
            pattern = Regex("""$AMOUNT\s+received""", RegexOption.IGNORE_CASE),
        ),
        NotificationTemplate(
            id = "received_prefix",
            direction = Direction.CREDIT,
            pattern = Regex("""\breceived\b\s+$AMOUNT""", RegexOption.IGNORE_CASE),
        ),
        NotificationTemplate(
            id = "credit_prefix",
            direction = Direction.CREDIT,
            pattern = Regex("""\bcredit(?:ed)?\b.{0,20}?$AMOUNT""", RegexOption.IGNORE_CASE),
        ),
    )
}
