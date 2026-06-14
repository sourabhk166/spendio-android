package com.khaata.app.parser

import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.Money
import com.khaata.app.domain.model.ParseResult
import com.khaata.app.domain.model.ParseTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.Instant

class DeduplicatorTest {

    private fun result(
        amountMinor: Long = 50000,
        direction: Direction = Direction.DEBIT,
        account: String? = "XX1234",
        at: Instant = Instant.parse("2026-06-14T10:15:30Z"),
        ref: String? = null,
    ) = ParseResult(
        amount = Money(amountMinor),
        direction = direction,
        occurredAt = at,
        maskedAccount = account,
        rawMerchant = null,
        confidence = 0.95f,
        tier = ParseTier.TIER1_REGEX,
        refId = ref,
    )

    @Test fun `same ref produces same key across sources`() {
        val a = Deduplicator.keyFor(result(ref = "ABC123"))
        val b = Deduplicator.keyFor(result(ref = "ABC123"))
        assertEquals(a, b)
    }

    @Test fun `same fields within time window collide when no ref`() {
        val a = Deduplicator.keyFor(result(at = Instant.parse("2026-06-14T10:15:30Z")))
        val b = Deduplicator.keyFor(result(at = Instant.parse("2026-06-14T10:15:45Z")))
        assertEquals(a, b) // 15s apart -> same 90s bucket
    }

    @Test fun `different amount produces different key`() {
        assertNotEquals(Deduplicator.keyFor(result(amountMinor = 50000)), Deduplicator.keyFor(result(amountMinor = 60000)))
    }

    @Test fun `far apart in time does not collide`() {
        val a = Deduplicator.keyFor(result(at = Instant.parse("2026-06-14T10:15:30Z")))
        val b = Deduplicator.keyFor(result(at = Instant.parse("2026-06-14T11:15:30Z")))
        assertNotEquals(a, b)
    }
}
