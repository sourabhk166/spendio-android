package com.spendio.app.di

import com.spendio.app.data.repository.RuleRepositoryImpl
import com.spendio.app.data.repository.TransactionRepositoryImpl
import com.spendio.app.domain.repository.RuleRepository
import com.spendio.app.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindRuleRepository(impl: RuleRepositoryImpl): RuleRepository
}
