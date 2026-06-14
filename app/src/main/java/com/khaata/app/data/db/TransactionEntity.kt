package com.khaata.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("occurredAt"),
        Index("categoryId"),
        Index("accountId"),
        Index(value = ["dedupeKey"], unique = true),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amountMinor: Long,
    val direction: String,
    val occurredAt: Long, // epoch millis
    val accountId: String?,
    val rawMerchant: String?,
    val merchant: String?,
    val categoryId: String,
    val note: String?,
    val source: String,
    val parseTier: String,
    val confidence: Float,
    val needsReview: Boolean,
    val sourcePackage: String?,
    val dedupeKey: String,
    val rawTextHash: String?,
)
