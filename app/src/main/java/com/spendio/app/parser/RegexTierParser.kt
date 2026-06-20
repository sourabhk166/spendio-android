package com.spendio.app.parser

import com.spendio.app.domain.model.Money
import com.spendio.app.domain.model.ParseResult
import com.spendio.app.domain.model.ParseTier
import com.spendio.app.domain.model.RawNotification

/**
 * Tier 1: match the notification against known templates — **learned templates first**
 * (induced by the AI tier and cached, see [TemplateCache]), then the bundled seed set.
 * A hit yields a high-confidence [ParseResult]; fields the template doesn't capture
 * (merchant/account/ref) are filled by the shared extractors.
 */
class RegexTierParser(
    private val seedTemplates: List<NotificationTemplate> = SeedTemplates.ALL,
    private val learnedTemplates: suspend (packageName: String) -> List<NotificationTemplate> = { emptyList() },
) : TransactionParser {

    override suspend fun parse(input: RawNotification): ParseResult? {
        val text = input.content
        val candidates = learnedTemplates(input.packageName) + seedTemplates.filter {
            it.packages.isEmpty() || input.packageName in it.packages
        }
        for (t in candidates) {
            val match = t.pattern.find(text) ?: continue
            val amount = match.namedOrNull("amount")?.let { Money.fromRupeeString(it) } ?: continue
            return ParseResult(
                amount = amount,
                direction = t.direction,
                occurredAt = input.postedAt,
                maskedAccount = match.namedOrNull("acct")?.let { "XX$it" }
                    ?: AccountExtractor.maskedAccount(text),
                rawMerchant = match.namedOrNull("merchant")?.trim()?.ifBlank { null }
                    ?: MerchantExtractor.extract(text),
                confidence = t.confidence,
                tier = ParseTier.TIER1_REGEX,
                refId = match.namedOrNull("ref") ?: RefExtractor.extract(text),
            )
        }
        return null
    }
}

/** Returns the value of a named group, or null if the group is absent or didn't match.
 *  Guards against IllegalArgumentException when the pattern has no such group. */
private fun MatchResult.namedOrNull(name: String): String? =
    runCatching { groups[name]?.value }.getOrNull()
