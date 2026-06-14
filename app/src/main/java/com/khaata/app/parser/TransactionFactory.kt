package com.khaata.app.parser

import com.khaata.app.domain.model.Categories
import com.khaata.app.domain.model.ParseResult
import com.khaata.app.domain.model.ParseTier
import com.khaata.app.domain.model.Transaction
import com.khaata.app.domain.model.TxnSource
import java.util.UUID

/**
 * Builds a persistable [Transaction] from a [ParseResult]. Categorization is left as
 * [Categories.UNCATEGORIZED] in Phase 1 (the AI categorizer arrives in Phase 2).
 * Stores only a hash of the raw text, never the text itself (privacy).
 */
object TransactionFactory {

    fun fromParse(
        result: ParseResult,
        sourcePackage: String,
        rawText: String?,
        categoryId: String = Categories.UNCATEGORIZED,
        idProvider: () -> String = { UUID.randomUUID().toString() },
    ): Transaction {
        require(result.isComplete) { "Cannot build a Transaction from an incomplete ParseResult" }
        val needsReview = result.tier == ParseTier.TIER3_HEURISTIC ||
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
}
