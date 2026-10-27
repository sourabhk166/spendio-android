package com.spendio.app.di

import com.spendio.app.ai.AiCapability
import com.spendio.app.ai.AiEngine
import com.spendio.app.ai.GeminiNanoEngine
import com.spendio.app.ai.NoOpAiEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    /**
     * Picks the on-device engine at startup. The rest of the app only sees [AiEngine].
     * Today [AiCapability] reports no Nano, so this is [NoOpAiEngine] and the app degrades
     * gracefully (regex + heuristic). When the live ML Kit GenAI check is wired,
     * capable devices transparently get [GeminiNanoEngine].
     */
    @Provides
    @Singleton
    fun provideAiEngine(
        capability: AiCapability,
        gemini: Provider<GeminiNanoEngine>,
    ): AiEngine = if (capability.supportsGeminiNano()) gemini.get() else NoOpAiEngine

    @Provides
    @Singleton
    fun provideGeminiNanoEngine(capability: AiCapability): GeminiNanoEngine =
        GeminiNanoEngine(capability)
}
