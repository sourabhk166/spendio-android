package com.spendio.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app database, encrypted at rest with SQLCipher (see DatabaseModule).
 *
 * v2 adds the template + categorization tables (parse_templates, merchant_rules).
 * Pre-release builds use destructive migration; a real Migration is added before any
 * production release.
 */
@Database(
    entities = [
        TransactionEntity::class,
        RawCaptureEntity::class,
        ParseTemplateEntity::class,
        MerchantRuleEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class SpendioDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun rawCaptureDao(): RawCaptureDao
    abstract fun parseTemplateDao(): ParseTemplateDao
    abstract fun merchantRuleDao(): MerchantRuleDao

    companion object {
        const val NAME = "spendio.db"
    }
}
