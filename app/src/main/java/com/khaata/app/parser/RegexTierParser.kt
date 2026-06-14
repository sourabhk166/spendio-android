package com.khaata.app.parser

import com.khaata.app.domain.model.Money
import com.khaata.app.domain.model.ParseResult
import com.khaata.app.domain.model.ParseTier
import com.khaata.app.domain.model.RawNotification

/**
 * Tier 1: match the notification against known templates. A hit yields a
 * high-confidence [ParseResult]. Fields not captured by the matched template
 * (merchant/account/ref) are filled by the shared extractors.
 */
class RegexTierParser(
    private val templates: List<NotificationTemplate> = SeedTemplates.ALL,
) : TransactionParser {

    override fun parse(input: RawNotification): ParseResult? {
        val text = input.content
        val candidates = templates.filter {
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
