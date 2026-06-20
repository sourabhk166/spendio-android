package com.spendio.app.ui

import com.spendio.app.domain.model.CategoryCatalog
import com.spendio.app.domain.model.Direction
import com.spendio.app.domain.model.Transaction

/** Flattened, display-ready transaction for Compose lists (Home + Review). */
data class TransactionUi(
    val id: String,
    val title: String,
    val amountLabel: String,
    /** Signed minor units (paise): positive for credit, negative for debit. For daily nets. */
    val signedMinor: Long,
    val direction: Direction,
    val occurredAtMillis: Long,
    val needsReview: Boolean,
    val categoryId: String,
    val categoryEmoji: String,
    val rawMerchant: String?,
)

fun Transaction.toUi(): TransactionUi = TransactionUi(
    id = id,
    title = displayName(merchant, rawMerchant),
    amountLabel = amount.format(),
    signedMinor = if (direction == Direction.CREDIT) amount.minorUnits else -amount.minorUnits,
    direction = direction,
    occurredAtMillis = occurredAt.toEpochMilli(),
    needsReview = needsReview,
    categoryId = categoryId,
    categoryEmoji = CategoryCatalog.info(categoryId).emoji,
    rawMerchant = rawMerchant,
)

/**
 * A human-friendly title. A user-set [merchant] is shown verbatim. Otherwise the captured
 * [rawMerchant] is tidied for display: a UPI VPA like "amitverma42@okaxis" drops its bank
 * handle ("amitverma42"), separators become spaces, and each word is capitalized — without
 * mangling all-caps brand names (EATCLUB stays EATCLUB).
 */
internal fun displayName(merchant: String?, rawMerchant: String?): String {
    merchant?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
    val raw = rawMerchant?.trim()?.takeIf { it.isNotBlank() } ?: return "Unknown"
    val base = if ('@' in raw) raw.substringBefore('@') else raw
    val cleaned = base.replace(Regex("[._]+"), " ").trim().ifBlank { return raw }
    return cleaned.split(' ').joinToString(" ") { word ->
        if (word.length > 1 && word == word.uppercase()) word // keep acronyms / all-caps brands
        else word.replaceFirstChar { it.uppercaseChar() }
    }
}
