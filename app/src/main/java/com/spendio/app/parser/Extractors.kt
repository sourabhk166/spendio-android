package com.spendio.app.parser

import com.spendio.app.domain.model.Direction
import com.spendio.app.domain.model.Money

/**
 * Shared, pure-Kotlin field extractors used by every parser tier. Each is a small,
 * independently unit-tested regex unit. Keeping these
 * framework-free is what makes the whole parser testable on the JVM.
 */

object AmountExtractor {
    // ₹/Rs/INR followed by an Indian/Western-grouped number with optional 1-2 decimals.
    private val regex = Regex(
        """(?:rs\.?|inr|₹)\s?([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE,
    )

    /** First currency amount in the text — for most bank formats this is the txn amount. */
    fun firstAmount(text: String): Money? =
        regex.find(text)?.groupValues?.get(1)?.let { Money.fromRupeeString(it) }

    fun allAmounts(text: String): List<Money> =
        regex.findAll(text).mapNotNull { Money.fromRupeeString(it.groupValues[1]) }.toList()

    /** Character range of the numeric part of the first amount (for template induction). */
    fun numericRange(text: String): IntRange? = regex.find(text)?.groups?.get(1)?.range
}

object DirectionDetector {
    // Bare "debit"/"credit" exclude a following "card" — "your SBI Credit Card" or "HDFC Debit
    // Card" names the card, it doesn't state a direction, and either can show up on either side
    // of a transaction (a Credit Card spend is a debit; a Debit Card refund is a credit).
    private val debitRegex = Regex(
        """\b(debited|debit(?!\s*card)|spent|paid|sent|withdrawn|withdrawal|purchased?|deducted|charged)\b""",
        RegexOption.IGNORE_CASE,
    )
    // Note: bare "cashback" is intentionally NOT a credit verb — promo notifications
    // ("₹10 cashback earned, unlock on your next payment") are not transactions. Real
    // cashback credits say "cashback credited", which "credited" already catches.
    private val creditRegex = Regex(
        """\b(credited|credit(?!\s*card)|received|refund(?:ed)?|deposited|added)\b""",
        RegexOption.IGNORE_CASE,
    )

    // Verb-less outgoing transfer shape: "Rs.X From <bank> A/c <num> To <payee>".
    private val fromAcctToRegex = Regex("""\bfrom\b.{0,40}?\ba/?c\b.{0,80}?\bto\b""", RegexOption.IGNORE_CASE)

    /** Returns the direction, choosing the keyword that appears earliest when both exist.
     *  Falls back to a structural "from <a/c> … to <payee>" = DEBIT rule. */
    fun detect(text: String): Direction? {
        val d = debitRegex.find(text)?.range?.first
        val c = creditRegex.find(text)?.range?.first
        return when {
            d != null && c == null -> Direction.DEBIT
            c != null && d == null -> Direction.CREDIT
            d != null && c != null -> if (d <= c) Direction.DEBIT else Direction.CREDIT
            fromAcctToRegex.containsMatchIn(text) -> Direction.DEBIT
            else -> null
        }
    }
}

object AccountExtractor {
    private val patterns = listOf(
        Regex("""(?:x|\*){2,}\s?(\d{3,4})\b""", RegexOption.IGNORE_CASE),
        Regex("""ending\s+(?:with\s+)?(\d{4})\b""", RegexOption.IGNORE_CASE),
        Regex("""a/?c(?:count)?\s*(?:no\.?|number)?\s*(?:x|\*)*\s*(\d{3,4})\b""", RegexOption.IGNORE_CASE),
        Regex("""card\s+(?:no\.?|ending|xx)?\s*(?:x|\*)*\s*(\d{4})\b""", RegexOption.IGNORE_CASE),
    )

    /** Normalized masked tail like "XX1234", or null. */
    fun maskedAccount(text: String): String? {
        for (p in patterns) p.find(text)?.let { return "XX" + it.groupValues[1] }
        return null
    }
}

object RefExtractor {
    private val regex = Regex(
        """(?:\bupi\s*ref(?:erence)?(?:\s*no\.?|:)?|\bref(?:erence)?(?:\s*no\.?|:)?|\btxn\s*id|\btransaction\s*id|\bimps\s*ref)\s*[:#]?\s*([A-Za-z0-9]{6,20})""",
        RegexOption.IGNORE_CASE,
    )

    fun extract(text: String): String? = regex.find(text)?.groupValues?.get(1)
}

object MerchantExtractor {
    // Tried in order; first match wins. Stops at common trailing tokens / punctuation.
    private val patterns = listOf(
        Regex("""(?:to|at)\s+vpa\s+(\S+)""", RegexOption.IGNORE_CASE),
        Regex("""\bvpa\s+(\S+)""", RegexOption.IGNORE_CASE),
        Regex(
            """paid\s+to\s+([A-Za-z0-9 ._&'@*-]{2,40}?)(?=\s+(?:via|on|using|ref|upi|dt|date)\b|[.,;]|$)""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """(?:to|at)\s+([A-Za-z0-9 ._&'@*-]{2,40}?)(?=\s+(?:via|on|using|ref|upi|dt|date)\b|[.,;]|$)""",
            RegexOption.IGNORE_CASE,
        ),
        // Incoming P2P credits ("...credited...from RAHUL SHARMA"): no "to"/"at" verb at all,
        // so this must come last — it would otherwise shadow the "from <bank> a/c ... to <payee>"
        // debit shape, but that shape's "to" pattern always matches first in the list above.
        // Deliberately case-SENSITIVE (no IGNORE_CASE) and requires each word to start with a
        // capital letter — bank prose like "from your account was debited" is all lowercase
        // and must NOT be mistaken for a name; real names/banks are always Title Case or CAPS.
        Regex(
            """\bfrom\s+([A-Z][A-Za-z&'-]*(?:\s+[A-Z][A-Za-z&'-]*){0,3})(?=\s+(?i:via|on|using|ref|upi|dt|date)\b|[.,;]|$)""",
        ),
    )

    fun extract(text: String): String? {
        for (p in patterns) {
            p.findAll(text).forEach { m ->
                val candidate = m.groupValues[1].trim()
                // Require at least one letter so we don't grab a trailing helpline number etc.
                if (candidate.isNotBlank() && candidate.any(Char::isLetter)) return candidate
            }
        }
        return null
    }
}
