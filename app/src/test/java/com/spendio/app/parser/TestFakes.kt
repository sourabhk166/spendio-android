package com.spendio.app.parser

import com.spendio.app.ai.AiEngine
import com.spendio.app.ai.AiExtraction
import com.spendio.app.data.db.ParseTemplateDao
import com.spendio.app.data.db.ParseTemplateEntity
import com.spendio.app.domain.model.Direction
import com.spendio.app.domain.model.RawNotification

/**
 * A fake on-device AI that simulates Gemini Nano by extracting with the shared regexes and
 * treating the first token as the merchant. Lets us exercise the AI tier + self-learning
 * loop deterministically on the JVM (the real model is device-only).
 */
class FakeAiEngine(
    private val available: Boolean = true,
    private val category: String? = null,
) : AiEngine {
    var extractCalls = 0
        private set

    override suspend fun isAvailable() = available
    override suspend fun ensureReady(onProgress: (Float) -> Unit) = available

    override suspend fun extractTransaction(notification: RawNotification): AiExtraction? {
        extractCalls++
        val amount = AmountExtractor.firstAmount(notification.content) ?: return null
        val direction = DirectionDetector.detect(notification.content) ?: Direction.DEBIT
        val merchant = notification.content.trim().substringBefore(' ').ifBlank { null }
        return AiExtraction(
            isTransaction = true,
            amountMinor = amount.minorUnits,
            direction = direction,
            merchantRaw = merchant,
            maskedAccount = AccountExtractor.maskedAccount(notification.content),
            confidence = 0.8f,
        )
    }

    override suspend fun classifyCategory(merchantKey: String): String? = category
}

/** In-memory ParseTemplateDao for tests. */
class FakeParseTemplateDao : ParseTemplateDao {
    val rows = mutableListOf<ParseTemplateEntity>()

    override suspend fun insert(template: ParseTemplateEntity) {
        if (rows.none { it.sourcePackage == template.sourcePackage && it.signature == template.signature }) {
            rows += template
        }
    }

    override suspend fun countFor(pkg: String, signature: String): Int =
        rows.count { it.sourcePackage == pkg && it.signature == signature }

    override suspend fun forPackage(pkg: String): List<ParseTemplateEntity> =
        rows.filter { it.sourcePackage == pkg }.sortedByDescending { it.hitCount }

    override suspend fun wipe() = rows.clear()
}
