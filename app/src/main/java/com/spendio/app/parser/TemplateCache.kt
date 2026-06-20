package com.spendio.app.parser

import com.spendio.app.data.db.ParseTemplateDao
import com.spendio.app.data.db.ParseTemplateEntity
import com.spendio.app.domain.model.Direction
import com.spendio.app.domain.model.ParseResult
import com.spendio.app.domain.model.RawNotification
import java.util.UUID
import javax.inject.Inject

/**
 * The self-learning store. [learnFrom] induces a reusable template from an AI parse and
 * caches it (once per shape); [templatesFor] hands Tier 1 the learned templates for a
 * source so the next message of that shape is a free regex hit.
 */
class TemplateCache @Inject constructor(
    private val dao: ParseTemplateDao,
) {
    suspend fun learnFrom(input: RawNotification, result: ParseResult) {
        if (result.direction == null) return
        val signature = StructuralSignature.of(input.content)
        if (dao.countFor(input.packageName, signature) > 0) return
        val induced = TemplateInducer.induce(input.content, result) ?: return
        val now = System.currentTimeMillis()
        dao.insert(
            ParseTemplateEntity(
                id = UUID.randomUUID().toString(),
                sourcePackage = input.packageName,
                signature = signature,
                regexPattern = induced.pattern.pattern,
                direction = result.direction.name,
                confidence = TemplateInducer.LEARNED_CONFIDENCE,
                hitCount = 0,
                createdAt = now,
                lastUsedAt = now,
            ),
        )
    }

    suspend fun templatesFor(packageName: String): List<NotificationTemplate> =
        dao.forPackage(packageName).map { entity ->
            NotificationTemplate(
                id = entity.id,
                direction = Direction.valueOf(entity.direction),
                pattern = Regex(entity.regexPattern, RegexOption.IGNORE_CASE),
                packages = setOf(packageName),
                confidence = entity.confidence,
            )
        }
}
