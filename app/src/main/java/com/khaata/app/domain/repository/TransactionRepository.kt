package com.khaata.app.domain.repository

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

    suspend fun update(txn: Transaction)

    suspend fun delete(txn: Transaction)

    suspend fun count(): Int

    suspend fun wipeAll()
}
