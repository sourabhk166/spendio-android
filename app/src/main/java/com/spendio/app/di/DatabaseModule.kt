package com.spendio.app.di

import android.content.Context
import androidx.room.Room
import com.spendio.app.data.db.DatabaseKey
import com.spendio.app.data.db.SpendioDatabase
import com.spendio.app.data.db.MerchantRuleDao
import com.spendio.app.data.db.ParseTemplateDao
import com.spendio.app.data.db.RawCaptureDao
import com.spendio.app.data.db.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        databaseKey: DatabaseKey,
    ): SpendioDatabase {
        // sqlcipher-android (4.6+) replaced the old SQLiteDatabase.loadLibs(context)
        // bootstrap with a plain System.loadLibrary; without it the JNI nativeOpen has
        // no implementation and the first DB access crashes with UnsatisfiedLinkError.
        System.loadLibrary("sqlcipher")
        deleteLegacyPlainDatabase(context)
        val factory = SupportOpenHelperFactory(databaseKey.getOrCreatePassphrase())
        return Room.databaseBuilder(context, SpendioDatabase::class.java, SpendioDatabase.NAME)
            .openHelperFactory(factory)
            // Pre-ship only: throwaway dogfood data. Replaced by a real Migration before release.
            .fallbackToDestructiveMigration()
            .build()
    }

    /** SQLCipher cannot open a pre-existing *plain* database, so the one-time switch to
     *  encryption deletes it (throwaway dogfood data). Runs exactly once. */
    private fun deleteLegacyPlainDatabase(context: Context) {
        val marker = context.getSharedPreferences("spendio_secure", Context.MODE_PRIVATE)
        if (!marker.getBoolean("encrypted_v1", false)) {
            listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
                runCatching { context.getDatabasePath(SpendioDatabase.NAME + suffix).delete() }
            }
            marker.edit().putBoolean("encrypted_v1", true).apply()
        }
    }

    @Provides
    fun provideTransactionDao(db: SpendioDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideRawCaptureDao(db: SpendioDatabase): RawCaptureDao = db.rawCaptureDao()

    @Provides
    fun provideParseTemplateDao(db: SpendioDatabase): ParseTemplateDao = db.parseTemplateDao()

    @Provides
    fun provideMerchantRuleDao(db: SpendioDatabase): MerchantRuleDao = db.merchantRuleDao()
}
