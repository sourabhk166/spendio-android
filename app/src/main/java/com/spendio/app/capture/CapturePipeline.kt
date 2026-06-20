package com.spendio.app.capture

import com.spendio.app.data.db.RawCaptureDao
import com.spendio.app.data.db.RawCaptureEntity
import com.spendio.app.domain.model.Categories
import com.spendio.app.domain.model.Direction
import com.spendio.app.domain.model.RawNotification
import com.spendio.app.domain.model.Transaction
import com.spendio.app.domain.repository.TransactionRepository
import com.spendio.app.parser.Categorizer
import com.spendio.app.parser.MerchantNormalizer
import com.spendio.app.parser.SourceFilter
import com.spendio.app.parser.TransactionFactory
import com.spendio.app.parser.TransactionParser
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
    private val userNameSource: UserNameSource,
    private val normalizer: MerchantNormalizer,
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

        var categorization = categorizer.categorize(parsed.rawMerchant)
        // Self-transfer: a person-to-person UPI where the other party is the user themselves
        // (paying your own account). Re-route from UPI (counts as spend) to Transfers (excluded).
        if (categorization.categoryId == Categories.UPI && isSelfTransfer(parsed.rawMerchant)) {
            categorization = categorization.copy(categoryId = Categories.TRANSFERS)
        }
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

    /** True when the counterparty name matches the user's own name (a self-transfer). */
    private suspend fun isSelfTransfer(rawMerchant: String?): Boolean {
        val userName = normalizer.normalize(userNameSource.current()) ?: return false
        val party = normalizer.normalize(rawMerchant) ?: return false
        return party == userName
    }
}
