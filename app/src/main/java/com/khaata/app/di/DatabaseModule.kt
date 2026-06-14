package com.khaata.app.di

import android.content.Context
import androidx.room.Room
import com.khaata.app.data.db.KhaataDatabase
import com.khaata.app.data.db.RawCaptureDao
import com.khaata.app.data.db.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KhaataDatabase =
        Room.databaseBuilder(context, KhaataDatabase::class.java, KhaataDatabase.NAME)
            .build()

    @Provides
    fun provideTransactionDao(db: KhaataDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideRawCaptureDao(db: KhaataDatabase): RawCaptureDao = db.rawCaptureDao()
}
