package com.spendio.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Phase 1–2 use a plain Room database. SQLCipher encryption (ADR-006/010) is the next
 * hardening step, added as an isolated change so the native dependency can be verified
 * independently of the rest of the app.
 *
 * v2 adds the self-learning + categorization tables (parse_templates, merchant_rules).
 * Pre-ship we use destructive migration (throwaway dogfood data); a real Migration is
 * added before any production release.
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
