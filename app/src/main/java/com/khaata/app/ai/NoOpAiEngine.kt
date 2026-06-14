package com.khaata.app.ai

import com.khaata.app.domain.model.RawNotification

/**
 * Used on devices without on-device AI. The app remains fully functional: regex Tier 1
 * handles known formats and the heuristic Tier 3 catches the rest (flagged needsReview).
 */
object NoOpAiEngine : AiEngine {
    override suspend fun isAvailable(): Boolean = false
    override suspend fun ensureReady(onProgress: (Float) -> Unit): Boolean = false
    override suspend fun extractTransaction(notification: RawNotification): AiExtraction? = null
    override suspend fun classifyCategory(merchantKey: String): String? = null
}
