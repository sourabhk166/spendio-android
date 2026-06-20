package com.spendio.app.parser

import com.spendio.app.ai.AiEngine
import com.spendio.app.domain.model.Categories
import com.spendio.app.domain.model.Direction
import com.spendio.app.domain.repository.RuleRepository
import javax.inject.Inject

/**
 * The outcome of categorization: the chosen [categoryId] plus whether the guess is uncertain
 * enough that the user should confirm it in Review. [needsReview] is set for person-to-person
 * payments, which are genuinely ambiguous (a repayment, a gift, actual income, or a transfer)
 * and only the user can disambiguate. Confident hits (user rules, keyword dictionary) never
 * force review.
 */
data class Categorization(val categoryId: String, val needsReview: Boolean = false)

/**
 * Resolves a merchant to a category (docs/04-lld.md §5):
 *   normalize -> cached rule -> keyword dictionary -> P2P heuristic -> AI classify -> uncategorized.
 * The keyword tier is deterministic and device-independent, so categorization works even
 * where on-device AI is unavailable (i.e. nearly all devices). User corrections feed back
 * through [correct] and win over every guess forever.
 */
class Categorizer @Inject constructor(
    private val rules: RuleRepository,
    private val ai: AiEngine,
    private val normalizer: MerchantNormalizer,
) {
    /**
     * [direction] and [isConfirmedUpi] only affect the P2P person-name heuristic below — every
     * other tier (user rule, keyword dict, AI) is direction/method agnostic.
     */
    suspend fun categorize(
        rawMerchant: String?,
        direction: Direction = Direction.DEBIT,
        isConfirmedUpi: Boolean = false,
    ): Categorization {
        val key = normalizer.normalize(rawMerchant) ?: return Categorization(Categories.UNCATEGORIZED)
        rules.categoryFor(key)?.let { return Categorization(it) }
        MerchantKeywords.categoryFor(key)?.let { return Categorization(it) }
        // P2P heuristic: two or more all-alphabetic tokens → person name. The exact bucket
        // depends on what we actually know:
        //  - money coming IN from a person → Payback (someone returning money they owed)
        //  - money going OUT, confirmed UPI (VPA handle / "UPI" in the notification) → UPI
        //  - money going OUT, method unclear (could be UPI, IMPS, NEFT...) → Transfers
        // Runs after keyword lookup so known brands (Airtel, Zerodha, etc.) are never affected.
        // All three count as spending/income as normal; a genuine self-transfer is re-routed to
        // Self Transfer later in the capture pipeline, where the user's own name is known.
        // Always flagged for review since the meaning is the user's to confirm.
        if (isPersonName(key)) {
            val categoryId = when {
                direction == Direction.CREDIT -> Categories.INCOME_PAYBACK
                isConfirmedUpi -> Categories.UPI
                else -> Categories.TRANSFERS
            }
            return Categorization(categoryId, needsReview = true)
        }
        val predicted = ai.classifyCategory(key) ?: return Categorization(Categories.UNCATEGORIZED)
        if (predicted !in Categories.ALL) return Categorization(Categories.UNCATEGORIZED)
        rules.put(key, predicted, userConfirmed = false)
        return Categorization(predicted)
    }

    private fun isPersonName(key: String): Boolean {
        val tokens = key.split(' ').filter { it.isNotBlank() }
        return tokens.size >= 2 && tokens.all { token ->
            token.length >= 2 && token.all { it.isLetter() }
        }
    }

    /** Records a user's category correction for this merchant (and future ones). */
    suspend fun correct(rawMerchant: String?, categoryId: String) {
        val key = normalizer.normalize(rawMerchant) ?: return
        rules.put(key, categoryId, userConfirmed = true)
    }
}
