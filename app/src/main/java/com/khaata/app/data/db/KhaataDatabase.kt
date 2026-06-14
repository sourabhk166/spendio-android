package com.khaata.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Phase 1 uses a plain Room database. SQLCipher encryption (ADR-006) is the next
 * hardening step, added as an isolated change so the native dependency can be
 * verified independently of the rest of the app.
 */
@Database(
    entities = [TransactionEntity::class, RawCaptureEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class KhaataDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun rawCaptureDao(): RawCaptureDao

    companion object {
        const val NAME = "khaata.db"
    }
}
