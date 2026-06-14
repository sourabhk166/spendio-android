package com.khaata.app.parser

import com.khaata.app.domain.model.ParseResult
import com.khaata.app.domain.model.RawNotification

/**
 * Strategy interface for every parser tier and the orchestrator. `suspend` because the
 * Tier-2 on-device AI call (Phase 2) is asynchronous; the regex/heuristic tiers implement
 * it with non-suspending bodies.
 */
interface TransactionParser {
    suspend fun parse(input: RawNotification): ParseResult?
}
