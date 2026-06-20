package com.spendio.app.domain.model

import java.time.Instant

/**
 * A single captured or manually-entered transaction. This is the core domain entity;
 * the Room layer mirrors it as TransactionEntity (see data/db).
 */
data class Transaction(
    val id: String,
    val amount: Money,
    val direction: Direction,
    val occurredAt: Instant,
    val accountId: String?,
    val rawMerchant: String?,
    val merchant: String?,
    val categoryId: String,
    val note: String? = null,
    val source: TxnSource,
    val parseTier: ParseTier,
    val confidence: Float,
    val needsReview: Boolean,
    val sourcePackage: String?,
    val dedupeKey: String,
    val rawTextHash: String?,
)
