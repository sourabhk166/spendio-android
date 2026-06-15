package com.khaata.app.parser

import com.khaata.app.domain.model.Categories
import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.Money
import com.khaata.app.domain.model.ParseTier
import com.khaata.app.domain.model.TxnSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TransactionFactoryManualTest {

    private fun build(id: String) = TransactionFactory.manual(
        amount = Money(28300),
        direction = Direction.DEBIT,
        merchant = "Auto rickshaw",
        categoryId = Categories.TRANSPORT,
        occurredAt = Instant.parse("2026-06-15T06:00:00Z"),
        note = null,
        idProvider = { id },
    )

    @Test fun `manual entry is high-confidence and never needs review`() {
        val t = build("a")
        assertEquals(TxnSource.MANUAL, t.source)
        assertEquals(ParseTier.MANUAL, t.parseTier)
        assertEquals(1f, t.confidence, 0f)
        assertFalse(t.needsReview)
        assertEquals(Categories.TRANSPORT, t.categoryId)
    }

    @Test fun `dedupe key is unique per id so manual entries never collide with captures`() {
        assertNotEquals(build("a").dedupeKey, build("b").dedupeKey)
        assertTrue(build("a").dedupeKey.startsWith("manual:"))
    }
}
