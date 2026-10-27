package com.spendio.app.parser

import com.spendio.app.domain.model.ParseResult
import com.spendio.app.domain.model.RawNotification

/**
 * Strategy interface for every parser tier and the orchestrator. `suspend` because the
 * Tier-2 on-device AI call is asynchronous; the regex/heuristic tiers implement
 * it with non-suspending bodies.
 */
interface TransactionParser {
    suspend fun parse(input: RawNotification): ParseResult?
}
