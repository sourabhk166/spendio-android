package com.khaata.app.parser

import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.Money
import com.khaata.app.domain.model.ParseTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end parser tests over a small corpus of representative Indian bank/UPI
 * notification shapes. Grow this corpus from real captured notifications in Phase 1 —
 * it is the accuracy-KPI baseline (docs/04-lld.md §12).
 */
class TieredTransactionParserTest {

    private val parser = TieredTransactionParser(
        detector = TransactionDetector(),
        regexTier = RegexTierParser(),
        heuristicTier = HeuristicParser(),
    )

    @Test fun `known formats parse via tier 1 with high confidence`() {
        val cases = listOf(
            "₹149 paid to Swiggy via UPI. UPI Ref: 412345678901" to (Money(14900) to Direction.DEBIT),
            "Rs.500.00 debited from a/c XX1234 on 14-06-26" to (Money(50000) to Direction.DEBIT),
            "Rs.2,000 credited to a/c XX5678" to (Money(200000) to Direction.CREDIT),
            "Rs.899 spent on card ending 4321" to (Money(89900) to Direction.DEBIT),
            "Rs.1,250.75 received from Rahul" to (Money(125075) to Direction.CREDIT),
        )
        for ((text, expected) in cases) {
            val r = parser.parse(notif(text)) ?: error("Expected a parse for: $text")
            val (amount, direction) = expected
            assertEquals("amount for: $text", amount, r.amount)
            assertEquals("direction for: $text", direction, r.direction)
            assertEquals("tier for: $text", ParseTier.TIER1_REGEX, r.tier)
            assertTrue("confidence for: $text", r.confidence >= TieredTransactionParser.HIGH_CONFIDENCE)
        }
    }

    @Test fun `unknown format falls back to heuristic flagged low confidence`() {
        val r = parser.parse(notif("Payment of Rs.300 from your account was debited successfully"))!!
        assertEquals(Money(30000), r.amount)
        assertEquals(Direction.DEBIT, r.direction)
        assertEquals(ParseTier.TIER3_HEURISTIC, r.tier)
        assertTrue(r.confidence < TieredTransactionParser.HIGH_CONFIDENCE)
    }

    @Test fun `non-transactions are dropped`() {
        assertNull(parser.parse(notif("Your OTP is 123456")))
        assertNull(parser.parse(notif("Rs.500 will be debited tomorrow")))
        assertNull(parser.parse(notif("John has requested Rs.500")))
        assertNull(parser.parse(notif("Get 50% off, flat Rs.200 discount on your next order")))
    }
}
