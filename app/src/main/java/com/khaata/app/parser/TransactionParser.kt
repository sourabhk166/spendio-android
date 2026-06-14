package com.khaata.app.parser

import com.khaata.app.domain.model.ParseResult
import com.khaata.app.domain.model.RawNotification

/**
 * Strategy interface for every parser tier and the orchestrator.
 * Synchronous in Phase 1 (regex only). The signature gains `suspend` in Phase 2 when
 * the on-device AI tier is introduced (see docs/05-roadmap-phases.md).
 */
interface TransactionParser {
    fun parse(input: RawNotification): ParseResult?
}
