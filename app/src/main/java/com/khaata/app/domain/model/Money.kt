package com.khaata.app.domain.model

import java.text.NumberFormat
import java.util.Locale

/**
 * Money is stored as integer **paise** (1 rupee = 100 paise) to avoid floating-point
 * rounding errors. See ADR-008. Never represent money as Double/Float anywhere.
 */
@JvmInline
value class Money(val minorUnits: Long) {

    operator fun plus(other: Money) = Money(minorUnits + other.minorUnits)
    operator fun minus(other: Money) = Money(minorUnits - other.minorUnits)

    /** Whole rupees, truncated toward zero. */
    val rupees: Long get() = minorUnits / 100

    /** Formats as e.g. "₹1,234.50" using the Indian locale grouping. */
    fun format(locale: Locale = Locale("en", "IN")): String {
        val nf = NumberFormat.getCurrencyInstance(locale)
        return nf.format(minorUnits / 100.0)
    }

    companion object {
        val ZERO = Money(0)

        /** Builds Money from a rupee amount string like "1,234.50" or "1234". */
        fun fromRupeeString(raw: String): Money? {
            val cleaned = raw.replace(",", "").trim()
            if (cleaned.isEmpty()) return null
            val parts = cleaned.split(".")
            return when (parts.size) {
                1 -> parts[0].toLongOrNull()?.let { Money(it * 100) }
                2 -> {
                    val whole = parts[0].toLongOrNull() ?: return null
                    // pad/truncate the fractional part to exactly 2 digits
                    val fracDigits = parts[1].padEnd(2, '0').take(2)
                    val frac = fracDigits.toLongOrNull() ?: return null
                    Money(whole * 100 + frac)
                }
                else -> null
            }
        }
    }
}
