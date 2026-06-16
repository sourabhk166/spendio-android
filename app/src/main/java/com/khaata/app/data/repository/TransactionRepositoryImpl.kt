package com.khaata.app.data.repository

import com.khaata.app.data.db.TransactionDao
import com.khaata.app.data.mapper.toDomain
import com.khaata.app.data.mapper.toEntity
import com.khaata.app.domain.model.Categories
import com.khaata.app.domain.model.CategorySlice
import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.Insights
import com.khaata.app.domain.model.Money
import com.khaata.app.domain.model.Transaction
import com.khaata.app.domain.repository.MonthlyTotals
import com.khaata.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
) : TransactionRepository {

    override suspend fun insertIfNew(txn: Transaction): Boolean =
        dao.insert(txn.toEntity()) != -1L

    override fun observeRecent(limit: Int): Flow<List<Transaction>> =
        dao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeMonth(month: YearMonth): Flow<List<Transaction>> {
        val (from, to) = month.bounds()
        return dao.observeBetween(from, to).map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeMonthlyTotals(month: YearMonth): Flow<MonthlyTotals> {
        val (from, to) = month.bounds()
        // Spent = real consumption (no transfers / investments); Received = real income (no transfers).
        val spent = dao.observeTotal(Direction.DEBIT.name, from, to, Categories.NON_EXPENSE)
        val income = dao.observeTotal(Direction.CREDIT.name, from, to, Categories.NON_INCOME)
        return combine(spent, income) { s, i -> MonthlyTotals(Money(s), Money(i)) }
    }

    override fun observeCategoryBreakdown(month: YearMonth): Flow<List<CategorySlice>> {
        val (from, to) = month.bounds()
        return dao.observeCategoryTotals(Direction.DEBIT.name, from, to, Categories.NON_EXPENSE).map { rows ->
            Insights.slices(rows.associate { it.categoryId to it.total })
        }
    }

    override fun observeIncomeBreakdown(month: YearMonth): Flow<List<CategorySlice>> {
        val (from, to) = month.bounds()
        return dao.observeCategoryTotals(Direction.CREDIT.name, from, to, Categories.NON_INCOME).map { rows ->
            Insights.slices(rows.associate { it.categoryId to it.total })
        }
    }

    override fun search(query: String, limit: Int): Flow<List<Transaction>> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return flowOf(emptyList())
        // Escape LIKE wildcards so a literal % or _ in the query doesn't match everything.
        val escaped = trimmed.replace("%", "").replace("_", "")
        return dao.search(escaped, limit).map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeNeedsReview(): Flow<List<Transaction>> =
        dao.observeNeedsReview().map { rows -> rows.map { it.toDomain() } }

    override fun observeNeedsReviewCount(): Flow<Int> = dao.observeNeedsReviewCount()

    override suspend fun approveReview(id: String) = dao.approveReview(id)

    override suspend fun recategorize(id: String, categoryId: String) = dao.recategorize(id, categoryId)

    override suspend fun getById(id: String): Transaction? = dao.getById(id)?.toDomain()

    override suspend fun allTransactions(): List<Transaction> = dao.getAll().map { it.toDomain() }

    override suspend fun update(txn: Transaction) = dao.update(txn.toEntity())

    override suspend fun delete(txn: Transaction) = dao.delete(txn.toEntity())

    override suspend fun count(): Int = dao.count()

    override suspend fun wipeAll() = dao.wipe()

    private fun YearMonth.bounds(zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val from = atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        return from to to
    }
}
