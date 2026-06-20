package com.spendio.app.capture

import com.spendio.app.ai.NoOpAiEngine
import com.spendio.app.data.db.RawCaptureDao
import com.spendio.app.data.db.RawCaptureEntity
import com.spendio.app.domain.model.Money
import com.spendio.app.domain.model.RawNotification
import com.spendio.app.domain.model.Transaction
import com.spendio.app.domain.repository.MonthlyTotals
import com.spendio.app.domain.repository.RuleRepository
import com.spendio.app.domain.repository.TransactionRepository
import com.spendio.app.parser.Categorizer
import com.spendio.app.parser.HeuristicParser
import com.spendio.app.parser.MerchantNormalizer
import com.spendio.app.parser.RegexTierParser
import com.spendio.app.parser.SourceFilter
import com.spendio.app.parser.TieredTransactionParser
import com.spendio.app.parser.TransactionDetector
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

    private var userName = ""

    private val pipeline = CapturePipeline(
        sourceFilter = SourceFilter(),
        parser = TieredTransactionParser(TransactionDetector(), RegexTierParser(), HeuristicParser()),
        categorizer = categorizer,
        repository = repo,
        rawCaptureDao = rawDao,
        userNameSource = { userName },
        normalizer = MerchantNormalizer(),
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

    @Test fun `person-to-person UPI is categorized UPI and flagged for review`() = runTest {
        val outcome = pipeline.process(raw("Rs.500 sent to RAHUL SHARMA via UPI Ref 123456789012"))
        assertTrue(outcome is CaptureOutcome.Saved)
        val txn = repo.inserted.first()
        // UPI counts as spending (most person payments are real), and is surfaced for review.
        assertEquals(com.spendio.app.domain.model.Categories.UPI, txn.categoryId)
        assertTrue("P2P payment must be surfaced for review", txn.needsReview)
    }

    @Test fun `self-transfer is re-routed to Self Transfer when the name matches the user`() = runTest {
        userName = "Arjun Mehta"
        val outcome = pipeline.process(raw("Rs.5000 sent to ARJUN MEHTA via UPI Ref 998877665544"))
        assertTrue(outcome is CaptureOutcome.Saved)
        val txn = repo.inserted.first()
        // Paying your own account → Self Transfer (excluded from spend), still flagged for review.
        assertEquals(com.spendio.app.domain.model.Categories.SELF_TRANSFER, txn.categoryId)
        assertTrue(txn.needsReview)
    }

    @Test fun `outgoing person payment with no UPI signal lands in Transfers`() = runTest {
        // No "UPI" keyword and no '@' VPA in the merchant — the rail is genuinely unclear.
        val outcome = pipeline.process(raw("Rs.500 sent to RAHUL SHARMA Ref 123456789012"))
        assertTrue(outcome is CaptureOutcome.Saved)
        val txn = repo.inserted.first()
        assertEquals(com.spendio.app.domain.model.Categories.TRANSFERS, txn.categoryId)
        assertTrue(txn.needsReview)
    }

    @Test fun `incoming person payment is categorized Payback and flagged for review`() = runTest {
        val outcome = pipeline.process(raw("Rs.500 received from RAHUL SHARMA via UPI Ref 123456789012"))
        assertTrue(outcome is CaptureOutcome.Saved)
        val txn = repo.inserted.first()
        assertEquals(com.spendio.app.domain.model.Categories.INCOME_PAYBACK, txn.categoryId)
        assertTrue(txn.needsReview)
    }

    @Test fun `uncategorized incoming money is flagged for review`() = runTest {
        // Credit from an unknown VPA (digits in the handle, no keyword match) — could be income,
        // a repayment or a gift, so it should land in the review queue, not sit silently.
        val outcome = pipeline.process(
            raw("Rs.750.00 credited to HDFC Bank A/c XX8803 from VPA randomhandle99@oksbi (UPI 300000000104)"),
        )
        assertTrue(outcome is CaptureOutcome.Saved)
        val txn = repo.inserted.first()
        assertEquals(com.spendio.app.domain.model.Categories.UNCATEGORIZED, txn.categoryId)
        assertTrue("Uncategorized incoming money must be surfaced for review", txn.needsReview)
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
    override fun observeCategoryBreakdown(month: YearMonth) = emptyFlow<List<com.spendio.app.domain.model.CategorySlice>>()
    override fun observeIncomeBreakdown(month: YearMonth) = emptyFlow<List<com.spendio.app.domain.model.CategorySlice>>()
    override fun search(query: String, limit: Int): Flow<List<Transaction>> = emptyFlow()
    override fun observeNeedsReview(): Flow<List<Transaction>> = emptyFlow()
    override fun observeNeedsReviewCount(): Flow<Int> = flowOf(0)
    override suspend fun approveReview(id: String) = Unit
    override suspend fun recategorize(id: String, categoryId: String) = Unit
    override suspend fun getById(id: String): Transaction? = inserted.firstOrNull { it.id == id }
    override suspend fun allTransactions(): List<Transaction> = inserted.toList()
    override suspend fun transactionsBetween(fromMillis: Long, toMillis: Long): List<Transaction> =
        inserted.filter { it.occurredAt.toEpochMilli() in fromMillis..toMillis }
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
