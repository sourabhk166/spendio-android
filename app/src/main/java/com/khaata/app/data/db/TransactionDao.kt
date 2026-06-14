package com.khaata.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Aggregate row for category breakdowns. */
data class CategoryTotal(val categoryId: String, val total: Long)

@Dao
interface TransactionDao {

    /** dedupeKey has a unique index → duplicates are silently ignored.
     *  Returns the inserted rowId, or -1 if it was a duplicate. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(txn: TransactionEntity): Long

    @Update
    suspend fun update(txn: TransactionEntity)

    @Delete
    suspend fun delete(txn: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE occurredAt BETWEEN :from AND :to ORDER BY occurredAt DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query(
        """SELECT COALESCE(SUM(amountMinor), 0) FROM transactions
           WHERE direction = :direction AND occurredAt BETWEEN :from AND :to""",
    )
    fun observeTotal(direction: String, from: Long, to: Long): Flow<Long>

    @Query(
        """SELECT categoryId, SUM(amountMinor) AS total FROM transactions
           WHERE direction = 'DEBIT' AND occurredAt BETWEEN :from AND :to
           GROUP BY categoryId""",
    )
    fun observeCategoryTotals(from: Long, to: Long): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query("DELETE FROM transactions")
    suspend fun wipe()
}
