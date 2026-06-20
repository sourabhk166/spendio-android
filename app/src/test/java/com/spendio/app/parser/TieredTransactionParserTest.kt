package com.spendio.app.parser

import com.spendio.app.domain.model.Direction
import com.spendio.app.domain.model.Money
import com.spendio.app.domain.model.ParseTier
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end parser tests over a small corpus of representative Indian bank/UPI
 * notification shapes (real-world formats, fictional identifiers). Extend it whenever a
 * new format appears — it is the parser's accuracy baseline.
 */
class TieredTransactionParserTest {

    private val parser = TieredTransactionParser(
        detector = TransactionDetector(),
        regexTier = RegexTierParser(),
        heuristicTier = HeuristicParser(),
    )

    @Test fun `known formats parse via tier 1 with high confidence`() = runTest {
        val cases = listOf(
            "₹149 paid to Swiggy via UPI. UPI Ref: 412345678901" to (Money(14900) to Direction.DEBIT),
            "Rs.500.00 debited from a/c XX1234 on 14-06-26" to (Money(50000) to Direction.DEBIT),
            "Rs.2,000 credited to a/c XX5678" to (Money(200000) to Direction.CREDIT),
            "Rs.899 spent on card ending 4321" to (Money(89900) to Direction.DEBIT),
            "Rs.1,250.75 received from Rahul" to (Money(125075) to Direction.CREDIT),
            // Real-world HDFC formats:
            "Sent Rs.100.00 From HDFC Bank A/C *8803 To Rohan Kapoor" to (Money(10000) to Direction.DEBIT),
            "Sent Rs.1.00 From HDFC Bank A/C *8803 To Rohan Kapoor" to (Money(100) to Direction.DEBIT),
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

    @Test fun `unknown format falls back to heuristic flagged low confidence`() = runTest {
        val r = parser.parse(notif("Payment of Rs.300 from your account was debited successfully"))!!
        assertEquals(Money(30000), r.amount)
        assertEquals(Direction.DEBIT, r.direction)
        assertEquals(ParseTier.TIER3_HEURISTIC, r.tier)
        assertTrue(r.confidence < TieredTransactionParser.HIGH_CONFIDENCE)
    }

    @Test fun `non-transactions are dropped`() = runTest {
        assertNull(parser.parse(notif("Your OTP is 123456")))
        assertNull(parser.parse(notif("Rs.500 will be debited tomorrow")))
        assertNull(parser.parse(notif("John has requested Rs.500")))
        assertNull(parser.parse(notif("Get 50% off, flat Rs.200 discount on your next order")))
        // Real-world false positive: CRED promo must not be captured as income.
        assertNull(parser.parse(notif("₹10 cashback earned, use CRED UPI to unlock ₹10 cashback on your next payment")))
        // UPI mandate setup is not a spend.
        assertNull(parser.parse(notif("UPI Mandate: Sent Rs.700.00 mandate created successfully")))
    }
}
