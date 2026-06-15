package com.khaata.app.di

import android.content.Context
import androidx.room.Room
import com.khaata.app.data.db.DatabaseKey
import com.khaata.app.data.db.KhaataDatabase
import com.khaata.app.data.db.MerchantRuleDao
import com.khaata.app.data.db.ParseTemplateDao
import com.khaata.app.data.db.RawCaptureDao
import com.khaata.app.data.db.TransactionDao
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
    ): KhaataDatabase {
        // sqlcipher-android (4.6+) replaced the old SQLiteDatabase.loadLibs(context)
        // bootstrap with a plain System.loadLibrary; without it the JNI nativeOpen has
        // no implementation and the first DB access crashes with UnsatisfiedLinkError.
        System.loadLibrary("sqlcipher")
        deleteLegacyPlainDatabase(context)
        val factory = SupportOpenHelperFactory(databaseKey.getOrCreatePassphrase())
        return Room.databaseBuilder(context, KhaataDatabase::class.java, KhaataDatabase.NAME)
            .openHelperFactory(factory)
            // Pre-ship only: throwaway dogfood data. Replaced by a real Migration before release.
            .fallbackToDestructiveMigration()
            .build()
    }

    /** SQLCipher cannot open a pre-existing *plain* database, so the one-time switch to
     *  encryption deletes it (throwaway dogfood data). Runs exactly once. */
    private fun deleteLegacyPlainDatabase(context: Context) {
        val marker = context.getSharedPreferences("khaata_secure", Context.MODE_PRIVATE)
        if (!marker.getBoolean("encrypted_v1", false)) {
            listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
                runCatching { context.getDatabasePath(KhaataDatabase.NAME + suffix).delete() }
            }
            marker.edit().putBoolean("encrypted_v1", true).apply()
        }
    }

    @Provides
    fun provideTransactionDao(db: KhaataDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideRawCaptureDao(db: KhaataDatabase): RawCaptureDao = db.rawCaptureDao()

    @Provides
    fun provideParseTemplateDao(db: KhaataDatabase): ParseTemplateDao = db.parseTemplateDao()

    @Provides
    fun provideMerchantRuleDao(db: KhaataDatabase): MerchantRuleDao = db.merchantRuleDao()
}
