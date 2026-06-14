package com.khaata.app.di

import com.khaata.app.BuildConfig
import com.khaata.app.capture.CollectRawSamples
import com.khaata.app.parser.HeuristicParser
import com.khaata.app.parser.RegexTierParser
import com.khaata.app.parser.SourceFilter
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
    fun provideRegexTier(): RegexTierParser = RegexTierParser()

    @Provides @Singleton
    fun provideHeuristicTier(): HeuristicParser = HeuristicParser()

    @Provides @Singleton
    fun provideParser(
        detector: TransactionDetector,
        regexTier: RegexTierParser,
        heuristicTier: HeuristicParser,
    ): TransactionParser = TieredTransactionParser(detector, regexTier, heuristicTier)

    @Provides
    @CollectRawSamples
    fun provideCollectRawSamples(): Boolean = BuildConfig.DEBUG
}
