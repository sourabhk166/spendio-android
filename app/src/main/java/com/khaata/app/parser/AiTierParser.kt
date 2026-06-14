package com.khaata.app.parser

import com.khaata.app.ai.AiEngine
import com.khaata.app.domain.model.Money
import com.khaata.app.domain.model.ParseResult
import com.khaata.app.domain.model.ParseTier
import com.khaata.app.domain.model.RawNotification

/**
 * Tier 2: hand an unseen notification to the on-device [AiEngine] for structured
 * extraction. Returns null when the engine is a no-op (no AI on device) or when the
 * model says the message isn't a transaction.
 */
class AiTierParser(
    private val ai: AiEngine,
) : TransactionParser {

    override suspend fun parse(input: RawNotification): ParseResult? {
        val extraction = ai.extractTransaction(input) ?: return null
        if (!extraction.isTransaction || extraction.amountMinor == null || extraction.direction == null) {
            return null
        }
        return ParseResult(
            amount = Money(extraction.amountMinor),
            direction = extraction.direction,
            occurredAt = input.postedAt,
            maskedAccount = extraction.maskedAccount,
            rawMerchant = extraction.merchantRaw,
            confidence = extraction.confidence,
            tier = ParseTier.TIER2_AI,
            isFailureOrReversal = extraction.isReversalOrFailure,
            refId = RefExtractor.extract(input.content),
        )
    }
}
