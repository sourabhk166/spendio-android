package com.khaata.app.parser

import com.khaata.app.ai.AiEngine
import com.khaata.app.domain.model.Categories
import com.khaata.app.domain.repository.RuleRepository
import javax.inject.Inject

/**
 * Resolves a merchant to a category (docs/04-lld.md §5):
 *   normalize -> cached rule -> AI classify (cache the guess) -> uncategorized.
 * User corrections feed back through [correct] and win over AI guesses forever.
 */
class Categorizer @Inject constructor(
    private val rules: RuleRepository,
    private val ai: AiEngine,
    private val normalizer: MerchantNormalizer,
) {
    suspend fun categorize(rawMerchant: String?): String {
        val key = normalizer.normalize(rawMerchant) ?: return Categories.UNCATEGORIZED
        rules.categoryFor(key)?.let { return it }
        val predicted = ai.classifyCategory(key) ?: return Categories.UNCATEGORIZED
        if (predicted !in Categories.ALL) return Categories.UNCATEGORIZED
        rules.put(key, predicted, userConfirmed = false)
        return predicted
    }

    /** Records a user's category correction for this merchant (and future ones). */
    suspend fun correct(rawMerchant: String?, categoryId: String) {
        val key = normalizer.normalize(rawMerchant) ?: return
        rules.put(key, categoryId, userConfirmed = true)
    }
}
