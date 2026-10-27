package com.spendio.app.parser

import com.spendio.app.domain.model.ParseResult

/**
 * Produces a deterministic key so the same transaction captured twice collides — e.g.
 * when both the bank app *and* the SMS app post a notification for one payment. The key
 * deliberately excludes the source package so those cross-app duplicates merge. The DB
 * enforces a unique index on this key with INSERT OR IGNORE, making dedup atomic.
 *
 */
object Deduplicator {
    /** Notifications for one txn can arrive seconds apart; bucket time to absorb the skew. */
    const val DEDUPE_WINDOW_MS = 90_000L

    fun keyFor(result: ParseResult): String {
        val bucket = (result.occurredAt?.toEpochMilli() ?: 0L) / DEDUPE_WINDOW_MS
        // A ref id, when present, identifies the txn exactly; otherwise fall back to the
        // amount/direction/account/time-bucket tuple.
        val discriminator = result.refId?.takeIf { it.isNotBlank() } ?: "t$bucket"
        return listOf(
            result.amount?.minorUnits?.toString() ?: "?",
            result.direction?.name ?: "?",
            result.maskedAccount ?: "?",
            discriminator,
        ).joinToString("|").sha256()
    }
}
