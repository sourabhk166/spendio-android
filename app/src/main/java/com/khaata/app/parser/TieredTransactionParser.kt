package com.khaata.app.parser

import com.khaata.app.domain.model.ParseResult
import com.khaata.app.domain.model.RawNotification

/**
 * The orchestrator (docs/04-lld.md §4). Phase 1 wires two tiers:
 *   detector gate -> Tier 1 (regex templates) -> Tier 3 (heuristic, needsReview).
 * Tier 2 (on-device AI) slots in between them in Phase 2 without changing callers.
 */
class TieredTransactionParser(
    private val detector: TransactionDetector,
    private val regexTier: RegexTierParser,
    private val heuristicTier: HeuristicParser,
) : TransactionParser {

    override fun parse(input: RawNotification): ParseResult? {
        if (!detector.looksLikeTransaction(input)) return null

        regexTier.parse(input)
            ?.takeIf { it.isComplete && it.confidence >= HIGH_CONFIDENCE }
            ?.let { return it }

        // Tier 2 (AI) will be attempted here in Phase 2.

        return heuristicTier.parse(input)?.takeIf { it.isComplete }
    }

    companion object {
        const val HIGH_CONFIDENCE = 0.85f
    }
}
