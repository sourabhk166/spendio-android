package com.spendio.app.parser

import com.spendio.app.domain.model.ParseResult
import com.spendio.app.domain.model.RawNotification

/**
 * The orchestrator:
 *   detector gate -> Tier 1 (regex templates) -> Tier 2 (on-device AI) -> Tier 3 (heuristic).
 *
 * When the AI tier parses an unseen format, the result is fed to [TemplateCache.learnFrom],
 * which induces a reusable regex template and caches it. The *next* notification of that
 * shape is then a free Tier-1 hit — this is the self-learning loop that bounds AI calls to
 * one per format.
 */
class TieredTransactionParser(
    private val detector: TransactionDetector,
    private val regexTier: RegexTierParser,
    private val heuristicTier: HeuristicParser,
    private val aiTier: AiTierParser? = null,
    private val templateCache: TemplateCache? = null,
) : TransactionParser {

    override suspend fun parse(input: RawNotification): ParseResult? {
        if (!detector.looksLikeTransaction(input)) return null

        regexTier.parse(input)
            ?.takeIf { it.isComplete && it.confidence >= HIGH_CONFIDENCE }
            ?.let { return it }

        // Tier 2: on-device AI for unseen formats. On success, learn a template so the
        // next message of this shape is a Tier-1 hit.
        aiTier?.parse(input)?.takeIf { it.isComplete }?.let { result ->
            runCatching { templateCache?.learnFrom(input, result) }
            return result
        }

        return heuristicTier.parse(input)?.takeIf { it.isComplete }
    }

    companion object {
        const val HIGH_CONFIDENCE = 0.85f
    }
}
