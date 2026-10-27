package com.spendio.app.parser

import com.spendio.app.domain.model.ParseResult
import com.spendio.app.domain.model.ParseTier
import com.spendio.app.domain.model.RawNotification

/**
 * Tier 3: generic extraction for formats no template matched. Always low-confidence,
 * so the resulting transaction is flagged needsReview. This is the safety net that keeps
 * the app capturing even unseen formats.
 */
class HeuristicParser : TransactionParser {
    override suspend fun parse(input: RawNotification): ParseResult? {
        val text = input.content
        val amount = AmountExtractor.firstAmount(text) ?: return null
        val direction = DirectionDetector.detect(text) ?: return null
        return ParseResult(
            amount = amount,
            direction = direction,
            occurredAt = input.postedAt,
            maskedAccount = AccountExtractor.maskedAccount(text),
            rawMerchant = MerchantExtractor.extract(text),
            confidence = HEURISTIC_CONFIDENCE,
            tier = ParseTier.TIER3_HEURISTIC,
            refId = RefExtractor.extract(text),
        )
    }

    companion object {
        const val HEURISTIC_CONFIDENCE = 0.5f
    }
}
