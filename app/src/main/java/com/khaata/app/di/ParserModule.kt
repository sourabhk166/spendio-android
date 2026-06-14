package com.khaata.app.di

import com.khaata.app.BuildConfig
import com.khaata.app.ai.AiEngine
import com.khaata.app.capture.CollectRawSamples
import com.khaata.app.parser.AiTierParser
import com.khaata.app.parser.HeuristicParser
import com.khaata.app.parser.RegexTierParser
import com.khaata.app.parser.SeedTemplates
import com.khaata.app.parser.SourceFilter
import com.khaata.app.parser.TemplateCache
import com.khaata.app.parser.TieredTransactionParser
import com.khaata.app.parser.TransactionDetector
import com.khaata.app.parser.TransactionParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ParserModule {

    @Provides @Singleton
    fun provideSourceFilter(): SourceFilter = SourceFilter()

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
}
