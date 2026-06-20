package com.spendio.app.di

import com.spendio.app.BuildConfig
import com.spendio.app.ai.AiEngine
import com.spendio.app.capture.CollectRawSamples
import com.spendio.app.capture.UserNameSource
import com.spendio.app.data.prefs.SettingsStore
import com.spendio.app.parser.AiTierParser
import com.spendio.app.parser.HeuristicParser
import com.spendio.app.parser.RegexTierParser
import com.spendio.app.parser.SeedTemplates
import com.spendio.app.parser.SourceFilter
import com.spendio.app.parser.TemplateCache
import com.spendio.app.parser.TieredTransactionParser
import com.spendio.app.parser.TransactionDetector
import com.spendio.app.parser.TransactionParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ParserModule {

    @Provides @Singleton
    fun provideSourceFilter(settings: SettingsStore): SourceFilter =
        SourceFilter(disabledSources = { settings.disabledSourcesOnce() })

    @Provides @Singleton
    fun provideDetector(): TransactionDetector = TransactionDetector()

    @Provides @Singleton
    fun provideRegexTier(templateCache: TemplateCache): RegexTierParser =
        // Tier 1 consults learned templates (per source) first, then the seed set.
        RegexTierParser(
            seedTemplates = SeedTemplates.ALL,
            learnedTemplates = { pkg -> templateCache.templatesFor(pkg) },
        )

    @Provides @Singleton
    fun provideHeuristicTier(): HeuristicParser = HeuristicParser()

    @Provides @Singleton
    fun provideAiTier(ai: AiEngine): AiTierParser = AiTierParser(ai)

    @Provides @Singleton
    fun provideParser(
        detector: TransactionDetector,
        regexTier: RegexTierParser,
        heuristicTier: HeuristicParser,
        aiTier: AiTierParser,
        templateCache: TemplateCache,
    ): TransactionParser = TieredTransactionParser(
        detector = detector,
        regexTier = regexTier,
        heuristicTier = heuristicTier,
        aiTier = aiTier,
        templateCache = templateCache,
    )

    @Provides
    @CollectRawSamples
    fun provideCollectRawSamples(): Boolean = BuildConfig.DEBUG

    @Provides @Singleton
    fun provideUserNameSource(settings: SettingsStore): UserNameSource =
        UserNameSource { settings.userNameOnce() }
}
