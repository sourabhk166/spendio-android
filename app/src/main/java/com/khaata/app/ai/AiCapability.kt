package com.khaata.app.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Decides which [AiEngine] a device can run. The real Gemini Nano check queries AICore
 * via ML Kit GenAI's `FeatureStatus`; until that alpha dependency is wired (see ADR-011),
 * this reports "no Nano", so DI falls back to [NoOpAiEngine] and the app degrades
 * gracefully. Wiring the live check is an isolated, device-verified follow-up.
 */
class AiCapability @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun supportsGeminiNano(): Boolean {
        // TODO(phase-2-device): return GenAi.checkFeatureStatus(...) == AVAILABLE/DOWNLOADABLE
        return false
    }
}
