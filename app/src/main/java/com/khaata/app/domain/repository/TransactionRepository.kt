package com.khaata.app.domain.repository

import com.khaata.app.domain.model.CategorySlice
import com.khaata.app.domain.model.Money
import com.khaata.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

data class MonthlyTotals(val spent: Money, val income: Money)

interface TransactionRepository {
    /** Inserts if the dedupeKey is new. Returns true if a row was actually added. */
    suspend fun insertIfNew(txn: Transaction): Boolean

    fun observeRecent(limit: Int = 50): Flow<List<Transaction>>

    fun observeMonth(month: YearMonth): Flow<List<Transaction>>

    fun observeMonthlyTotals(month: YearMonth): Flow<MonthlyTotals>

    /** Spending breakdown (DEBIT, excluding transfers/investments). */
    fun observeCategoryBreakdown(month: YearMonth): Flow<List<CategorySlice>>

    /** Income breakdown by sub-category (CREDIT, excluding transfers). */
    fun observeIncomeBreakdown(month: YearMonth): Flow<List<CategorySlice>>

    /** Free-text search across merchant / raw merchant / note. Blank query yields nothing. */
    fun search(query: String, limit: Int = 100): Flow<List<Transaction>>

    fun observeNeedsReview(): Flow<List<Transaction>>

    fun observeNeedsReviewCount(): Flow<Int>

    suspend fun approveReview(id: String)

    suspend fun recategorize(id: String, categoryId: String)

    /** One transaction by id, or null — used to populate the edit form. */
    suspend fun getById(id: String): Transaction?

    suspend fun update(txn: Transaction)

    suspend fun delete(txn: Transaction)

    suspend fun count(): Int

    suspend fun wipeAll()
}
