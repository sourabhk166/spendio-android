package com.spendio.app.domain.model

import java.time.Instant

/**
 * Raw notification handed to the parser. Framework-free so the whole parser is
 * unit-testable on the JVM without Android.
 */
data class RawNotification(
    val packageName: String,
    val title: String?,
    val text: String?,
    val postedAt: Instant,
    val extras: Map<String, String> = emptyMap(),
) {
    /** Combined searchable text; title + body. */
    val content: String
        get() = listOfNotNull(title, text).joinToString("  ").trim()
}

/**
 * The structured output of a parse attempt, before it becomes a Transaction.
 * Null fields mean "the parser couldn't determine this".
 */
data class ParseResult(
    val amount: Money?,
    val direction: Direction?,
    val occurredAt: Instant?,
    val maskedAccount: String?,
    val rawMerchant: String?,
    val confidence: Float,
    val tier: ParseTier,
    val isFailureOrReversal: Boolean = false,
    val refId: String? = null,
) {
    /** A result is usable only if we extracted at least an amount and a direction. */
    val isComplete: Boolean get() = amount != null && direction != null
}
