package com.khaata.app.ai

import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.RawNotification

/**
 * Structured result of an AI extraction. Framework-free (no JSON types leak out) so the
 * AI tier and categorizer are fully unit-testable with a fake engine.
 */
data class AiExtraction(
    val isTransaction: Boolean,
    val amountMinor: Long?,
    val direction: Direction?,
    val merchantRaw: String?,
    val maskedAccount: String?,
    val isReversalOrFailure: Boolean = false,
    val confidence: Float = 0f,
)

/**
 * The single seam over all on-device AI (docs/04-lld.md §7). Implementations:
 *  - [GeminiNanoEngine]  — ML Kit GenAI / Gemini Nano (device-only; isolated alpha API)
 *  - [NoOpAiEngine]      — everything returns null/false (no AI on this device)
 *
 * The rest of the app only ever sees this interface; DI picks the concrete engine at
 * startup based on [AiCapability], so callers are unaware which is active.
 */
interface AiEngine {
    suspend fun isAvailable(): Boolean

    /** Triggers model download if needed; returns true once the model is usable. */
    suspend fun ensureReady(onProgress: (Float) -> Unit = {}): Boolean

    /** Extracts a transaction from an unseen notification format, or null if it can't. */
    suspend fun extractTransaction(notification: RawNotification): AiExtraction?

    /** Classifies a normalized merchant key into a category id, or null. */
    suspend fun classifyCategory(merchantKey: String): String?
}
