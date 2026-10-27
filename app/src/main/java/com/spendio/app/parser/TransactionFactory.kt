package com.spendio.app.parser

import com.spendio.app.domain.model.Categories
import com.spendio.app.domain.model.Direction
import com.spendio.app.domain.model.Money
import com.spendio.app.domain.model.ParseResult
import com.spendio.app.domain.model.ParseTier
import com.spendio.app.domain.model.Transaction
import com.spendio.app.domain.model.TxnSource
import java.time.Instant
import java.util.UUID

/**
 * Builds a persistable [Transaction] from a [ParseResult]. The category defaults to
 * [Categories.UNCATEGORIZED]; the capture pipeline assigns one via [Categorizer].
 * Stores only a hash of the raw text, never the text itself (privacy).
 */
object TransactionFactory {

    fun fromParse(
        result: ParseResult,
        sourcePackage: String,
        rawText: String?,
        categoryId: String = Categories.UNCATEGORIZED,
        forceReview: Boolean = false,
        idProvider: () -> String = { UUID.randomUUID().toString() },
    ): Transaction {
        require(result.isComplete) { "Cannot build a Transaction from an incomplete ParseResult" }
        val needsReview = forceReview ||
            result.tier == ParseTier.TIER3_HEURISTIC ||
            result.confidence < TieredTransactionParser.HIGH_CONFIDENCE
        return Transaction(
            id = idProvider(),
            amount = result.amount!!,
            direction = result.direction!!,
            occurredAt = result.occurredAt ?: java.time.Instant.now(),
            accountId = result.maskedAccount,
            rawMerchant = result.rawMerchant,
            merchant = null, // display normalization is a later polish step
            categoryId = categoryId,
            note = null,
            source = TxnSource.NOTIFICATION,
            parseTier = result.tier,
            confidence = result.confidence,
            needsReview = needsReview,
            sourcePackage = sourcePackage,
            dedupeKey = Deduplicator.keyFor(result),
            rawTextHash = rawText?.sha256(),
        )
    }

    /**
     * Builds a user-entered transaction. Manual entries are always high-confidence and never
     * need review. Their dedupe key is derived from the unique id (never from amount/time), so
     * a manual entry can never collide with a captured one — a user may legitimately log a cash
     * spend identical to a card spend.
     */
    fun manual(
        amount: Money,
        direction: Direction,
        merchant: String,
        categoryId: String,
        occurredAt: Instant,
        note: String?,
        idProvider: () -> String = { UUID.randomUUID().toString() },
    ): Transaction {
        val id = idProvider()
        return Transaction(
            id = id,
            amount = amount,
            direction = direction,
            occurredAt = occurredAt,
            accountId = null,
            rawMerchant = merchant,
            merchant = merchant,
            categoryId = categoryId,
            note = note,
            source = TxnSource.MANUAL,
            parseTier = ParseTier.MANUAL,
            confidence = 1f,
            needsReview = false,
            sourcePackage = null,
            dedupeKey = "manual:$id",
            rawTextHash = null,
        )
    }
}
