package com.khaata.app.data.backup

import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.Money
import com.khaata.app.domain.model.ParseTier
import com.khaata.app.domain.model.Transaction
import com.khaata.app.domain.model.TxnSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

/** The on-disk backup payload (before encryption). Versioned so old files stay readable. */
@Serializable
data class BackupFile(
    val version: Int = 1,
    val exportedAt: Long,
    val transactions: List<BackupTransaction>,
)

/** A flat, serialization-friendly mirror of [Transaction] — full fidelity for a clean restore. */
@Serializable
data class BackupTransaction(
    val id: String,
    val amountMinor: Long,
    val direction: String,
    val occurredAt: Long,
    val accountId: String? = null,
    val rawMerchant: String? = null,
    val merchant: String? = null,
    val categoryId: String,
    val note: String? = null,
    val source: String,
    val parseTier: String,
    val confidence: Float,
    val needsReview: Boolean,
    val sourcePackage: String? = null,
    val dedupeKey: String,
    val rawTextHash: String? = null,
)

fun Transaction.toBackup(): BackupTransaction = BackupTransaction(
    id = id,
    amountMinor = amount.minorUnits,
    direction = direction.name,
    occurredAt = occurredAt.toEpochMilli(),
    accountId = accountId,
    rawMerchant = rawMerchant,
    merchant = merchant,
    categoryId = categoryId,
    note = note,
    source = source.name,
    parseTier = parseTier.name,
    confidence = confidence,
    needsReview = needsReview,
    sourcePackage = sourcePackage,
    dedupeKey = dedupeKey,
    rawTextHash = rawTextHash,
)

fun BackupTransaction.toDomain(): Transaction = Transaction(
    id = id,
    amount = Money(amountMinor),
    direction = Direction.valueOf(direction),
    occurredAt = Instant.ofEpochMilli(occurredAt),
    accountId = accountId,
    rawMerchant = rawMerchant,
    merchant = merchant,
    categoryId = categoryId,
    note = note,
    source = TxnSource.valueOf(source),
    parseTier = ParseTier.valueOf(parseTier),
    confidence = confidence,
    needsReview = needsReview,
    sourcePackage = sourcePackage,
    dedupeKey = dedupeKey,
    rawTextHash = rawTextHash,
)

/** JSON (de)serialization for the backup payload. Pure, JVM-testable. */
object BackupSerializer {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(transactions: List<Transaction>): String =
        json.encodeToString(
            BackupFile(
                exportedAt = System.currentTimeMillis(),
                transactions = transactions.map { it.toBackup() },
            ),
        )

    fun decode(text: String): List<Transaction> =
        json.decodeFromString<BackupFile>(text).transactions.map { it.toDomain() }
}
