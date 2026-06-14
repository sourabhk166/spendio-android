package com.khaata.app.ai

import com.khaata.app.domain.model.RawNotification

/**
 * On-device AI via **ML Kit GenAI Prompt API (Gemini Nano / AICore)**.
 *
 * This is intentionally a thin, isolated skeleton. The ML Kit GenAI Prompt API is alpha
 * and runs only on AICore-capable devices (e.g. Pixel 8/9), so it cannot be exercised in
 * unit tests or CI. Per ADR-011 the live model call is wired and verified on a real device
 * as a separate step; until then this reports unavailable and the app uses regex + heuristic.
 *
 * Intended implementation (kept as a spec so the wiring is unambiguous):
 *  - isAvailable(): GenAi feature status is AVAILABLE or DOWNLOADABLE.
 *  - ensureReady(): if DOWNLOADABLE, trigger feature download and report progress.
 *  - extractTransaction(): prompt the model with the title+text and a strict JSON schema
 *      { isTransaction, amount, currency, direction, merchantRaw, maskedAccount,
 *        isReversalOrFailure, confidence }, then map the JSON to [AiExtraction].
 *  - classifyCategory(): prompt the model to map a merchant key to one of [com.khaata.app
 *      .domain.model.Categories].
 */
class GeminiNanoEngine(
    private val capability: AiCapability,
) : AiEngine {

    override suspend fun isAvailable(): Boolean = capability.supportsGeminiNano()

    override suspend fun ensureReady(onProgress: (Float) -> Unit): Boolean {
        if (!capability.supportsGeminiNano()) return false
        // TODO(phase-2-device): download the GenAI feature if DOWNLOADABLE, report progress.
        return false
    }

    override suspend fun extractTransaction(notification: RawNotification): AiExtraction? {
        if (!isAvailable()) return null
        // TODO(phase-2-device): ML Kit GenAI Prompt API call + JSON -> AiExtraction.
        return null
    }

    override suspend fun classifyCategory(merchantKey: String): String? {
        if (!isAvailable()) return null
        // TODO(phase-2-device): prompt the model to classify into a Categories id.
        return null
    }
}
