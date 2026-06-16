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
           WHERE direction = :direction AND occurredAt BETWEEN :from AND :to
           AND categoryId NOT IN (:excludedCategories)""",
    )
    fun observeTotal(direction: String, from: Long, to: Long, excludedCategories: List<String>): Flow<Long>

    @Query(
        """SELECT categoryId, SUM(amountMinor) AS total FROM transactions
           WHERE direction = :direction AND occurredAt BETWEEN :from AND :to
           AND categoryId NOT IN (:excludedCategories)
           GROUP BY categoryId""",
    )
    fun observeCategoryTotals(
        direction: String,
        from: Long,
        to: Long,
        excludedCategories: List<String>,
    ): Flow<List<CategoryTotal>>

    /** Free-text search over merchant, raw merchant and note. Newest first. */
    @Query(
        """SELECT * FROM transactions
           WHERE merchant LIKE '%' || :query || '%'
              OR rawMerchant LIKE '%' || :query || '%'
              OR note LIKE '%' || :query || '%'
           ORDER BY occurredAt DESC LIMIT :limit""",
    )
    fun search(query: String, limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE needsReview = 1 ORDER BY occurredAt DESC")
    fun observeNeedsReview(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE needsReview = 1")
    fun observeNeedsReviewCount(): Flow<Int>

    @Query("UPDATE transactions SET needsReview = 0 WHERE id = :id")
    suspend fun approveReview(id: String)

    @Query("UPDATE transactions SET categoryId = :categoryId, needsReview = 0 WHERE id = :id")
    suspend fun recategorize(id: String, categoryId: String)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    /** All transactions, newest first — used for CSV export. */
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    suspend fun getAll(): List<TransactionEntity>

    /** Transactions in a time window, newest first — used for date-ranged export. */
    @Query("SELECT * FROM transactions WHERE occurredAt BETWEEN :from AND :to ORDER BY occurredAt DESC")
    suspend fun getBetween(from: Long, to: Long): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query("DELETE FROM transactions")
    suspend fun wipe()
}
