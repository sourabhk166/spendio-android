package com.khaata.app.capture

import com.khaata.app.data.db.RawCaptureDao
import com.khaata.app.data.db.RawCaptureEntity
import com.khaata.app.domain.model.Categories
import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.RawNotification
import com.khaata.app.domain.model.Transaction
import com.khaata.app.domain.repository.TransactionRepository
import com.khaata.app.parser.Categorizer
import com.khaata.app.parser.SourceFilter
import com.khaata.app.parser.TransactionFactory
import com.khaata.app.parser.TransactionParser
import javax.inject.Inject

/** What happened to one captured notification — modelled explicitly so it's testable. */
sealed interface CaptureOutcome {
    data object Ignored : CaptureOutcome      // not a financial source
    data object NotParsed : CaptureOutcome    // financial source but not a transaction
    data object Duplicate : CaptureOutcome     // parsed but already stored (dedupe hit)
    data class Saved(val transaction: Transaction) : CaptureOutcome
}

/**
 * The heart of the capture path (docs/04-lld.md §6). Framework-free so it can be
 * unit-tested with fakes. The listener service just forwards [RawNotification]s here.
 */
class CapturePipeline @Inject constructor(
    private val sourceFilter: SourceFilter,
    private val parser: TransactionParser,
    private val categorizer: Categorizer,
    private val repository: TransactionRepository,
    private val rawCaptureDao: RawCaptureDao,
    @CollectRawSamples private val collectRawSamples: Boolean,
) {
    suspend fun process(raw: RawNotification): CaptureOutcome {
        if (!sourceFilter.isFinancialSource(raw.packageName)) return CaptureOutcome.Ignored

        val parsed = parser.parse(raw)

        // Debug-only: keep the raw text (even unparsed) to grow the parser corpus.
        if (collectRawSamples) {
            rawCaptureDao.insert(
                RawCaptureEntity(
                    packageName = raw.packageName,
                    title = raw.title,
                    text = raw.text,
                    capturedAt = raw.postedAt.toEpochMilli(),
                    parsed = parsed != null && parsed.isComplete,
                    parseTier = parsed?.tier?.name,
                ),
            )
        }

        if (parsed == null || !parsed.isComplete) return CaptureOutcome.NotParsed

        val categorization = categorizer.categorize(parsed.rawMerchant)
        // Money coming IN that we couldn't categorize is worth a glance — it could be income,
        // a repayment, or a gift, and only the user knows. Surface it for review rather than
        // letting it sit silently as Uncategorized. (Unknown outgoing spends stay off the queue
        // to avoid flooding it; they're visible on Home and editable there.)
        val incomingUnknown = parsed.direction == Direction.CREDIT &&
            categorization.categoryId == Categories.UNCATEGORIZED
        val txn = TransactionFactory.fromParse(
            result = parsed,
            sourcePackage = raw.packageName,
            rawText = raw.text,
            categoryId = categorization.categoryId,
            forceReview = categorization.needsReview || incomingUnknown,
        )
        return if (repository.insertIfNew(txn)) CaptureOutcome.Saved(txn) else CaptureOutcome.Duplicate
    }
}
