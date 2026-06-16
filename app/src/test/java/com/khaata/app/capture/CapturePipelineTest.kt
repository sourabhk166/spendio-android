package com.khaata.app.capture

import com.khaata.app.ai.NoOpAiEngine
import com.khaata.app.data.db.RawCaptureDao
import com.khaata.app.data.db.RawCaptureEntity
import com.khaata.app.domain.model.Money
import com.khaata.app.domain.model.RawNotification
import com.khaata.app.domain.model.Transaction
import com.khaata.app.domain.repository.MonthlyTotals
import com.khaata.app.domain.repository.RuleRepository
import com.khaata.app.domain.repository.TransactionRepository
import com.khaata.app.parser.Categorizer
import com.khaata.app.parser.HeuristicParser
import com.khaata.app.parser.MerchantNormalizer
import com.khaata.app.parser.RegexTierParser
import com.khaata.app.parser.SourceFilter
import com.khaata.app.parser.TieredTransactionParser
import com.khaata.app.parser.TransactionDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class CapturePipelineTest {

    private val repo = FakeTransactionRepository()
    private val rawDao = FakeRawCaptureDao()

    private val categorizer = Categorizer(FakeRuleRepository(), NoOpAiEngine, MerchantNormalizer())

    private val pipeline = CapturePipeline(
        sourceFilter = SourceFilter(),
        parser = TieredTransactionParser(TransactionDetector(), RegexTierParser(), HeuristicParser()),
        categorizer = categorizer,
        repository = repo,
        rawCaptureDao = rawDao,
        collectRawSamples = true,
    )

    private fun raw(text: String, pkg: String = "com.phonepe.app") = RawNotification(
        packageName = pkg,
        title = null,
        text = text,
        postedAt = Instant.parse("2026-06-14T10:15:30Z"),
    )

    @Test fun `valid transaction is saved`() = runTest {
        val outcome = pipeline.process(raw("Rs.500 debited from a/c XX1234"))
        assertTrue(outcome is CaptureOutcome.Saved)
        assertEquals(1, repo.inserted.size)
        assertEquals(Money(50000), repo.inserted.first().amount)
    }

    @Test fun `duplicate notification is not stored twice`() = runTest {
        pipeline.process(raw("Rs.500 debited from a/c XX1234"))
        val second = pipeline.process(raw("Rs.500 debited from a/c XX1234"))
        assertTrue(second is CaptureOutcome.Duplicate)
        assertEquals(1, repo.inserted.size)
    }

    @Test fun `non-financial source is ignored and not logged`() = runTest {
        val outcome = pipeline.process(raw("Rs.500 debited", pkg = "com.android.chrome"))
        assertEquals(CaptureOutcome.Ignored, outcome)
        assertTrue(repo.inserted.isEmpty())
        assertTrue(rawDao.captures.isEmpty())
    }

    @Test fun `person-to-person UPI is saved as a transfer flagged for review`() = runTest {
        val outcome = pipeline.process(raw("Rs.500 sent to RAHUL SHARMA via UPI Ref 123456789012"))
        assertTrue(outcome is CaptureOutcome.Saved)
        val txn = repo.inserted.first()
        assertEquals(com.khaata.app.domain.model.Categories.TRANSFERS, txn.categoryId)
        assertTrue("P2P transfer must be surfaced for review", txn.needsReview)
    }

    @Test fun `non-transaction from financial source is logged but not stored`() = runTest {
        val outcome = pipeline.process(raw("Your OTP is 123456"))
        assertEquals(CaptureOutcome.NotParsed, outcome)
        assertTrue(repo.inserted.isEmpty())
        assertEquals(1, rawDao.captures.size)
        assertEquals(false, rawDao.captures.first().parsed)
    }
}

/** In-memory repository that enforces dedupe on dedupeKey, like the real Room unique index. */
private class FakeTransactionRepository : TransactionRepository {
    val inserted = mutableListOf<Transaction>()
    private val keys = mutableSetOf<String>()

    override suspend fun insertIfNew(txn: Transaction): Boolean {
        if (!keys.add(txn.dedupeKey)) return false
        inserted += txn
        return true
    }

    override fun observeRecent(limit: Int): Flow<List<Transaction>> = flowOf(inserted.toList())
    override fun observeMonth(month: YearMonth): Flow<List<Transaction>> = emptyFlow()
    override fun observeMonthlyTotals(month: YearMonth): Flow<MonthlyTotals> =
        flowOf(MonthlyTotals(Money.ZERO, Money.ZERO))
    override fun observeCategoryBreakdown(month: YearMonth) = emptyFlow<List<com.khaata.app.domain.model.CategorySlice>>()
    override fun observeNeedsReview(): Flow<List<Transaction>> = emptyFlow()
    override fun observeNeedsReviewCount(): Flow<Int> = flowOf(0)
    override suspend fun approveReview(id: String) = Unit
    override suspend fun recategorize(id: String, categoryId: String) = Unit
    override suspend fun getById(id: String): Transaction? = inserted.firstOrNull { it.id == id }
    override suspend fun update(txn: Transaction) = Unit
    override suspend fun delete(txn: Transaction) = Unit
    override suspend fun count(): Int = inserted.size
    override suspend fun wipeAll() = Unit
}

private class FakeRuleRepository : RuleRepository {
    private val rules = mutableMapOf<String, String>()
    override suspend fun categoryFor(merchantKey: String): String? = rules[merchantKey]
    override suspend fun put(merchantKey: String, categoryId: String, userConfirmed: Boolean) {
        rules[merchantKey] = categoryId
    }
}

private class FakeRawCaptureDao : RawCaptureDao {
    val captures = mutableListOf<RawCaptureEntity>()
    override suspend fun insert(capture: RawCaptureEntity) { captures += capture }
    override fun observeRecent(limit: Int): Flow<List<RawCaptureEntity>> = flowOf(captures.toList())
    override suspend fun wipe() { captures.clear() }
}
