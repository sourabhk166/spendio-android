package com.khaata.app.ui

import com.khaata.app.domain.model.CategoryCatalog
import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.Transaction

/** Flattened, display-ready transaction for Compose lists (Home + Review). */
data class TransactionUi(
    val id: String,
    val title: String,
    val amountLabel: String,
    val direction: Direction,
    val occurredAtMillis: Long,
    val needsReview: Boolean,
    val categoryId: String,
    val categoryEmoji: String,
    val rawMerchant: String?,
)

fun Transaction.toUi(): TransactionUi = TransactionUi(
    id = id,
    title = merchant ?: rawMerchant ?: "Unknown",
    amountLabel = amount.format(),
    direction = direction,
    occurredAtMillis = occurredAt.toEpochMilli(),
    needsReview = needsReview,
    categoryId = categoryId,
    categoryEmoji = CategoryCatalog.info(categoryId).emoji,
    rawMerchant = rawMerchant,
)
