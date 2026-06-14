package com.khaata.app.parser

import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.Money
import com.khaata.app.domain.model.ParseTier
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegexTierParserTest {

    private val parser = RegexTierParser()

    @Test fun `paid to template captures amount merchant and is high confidence`() = runTest {
        val r = parser.parse(notif("₹149 paid to Swiggy via UPI. UPI Ref: 412345678901"))!!
        assertEquals(Money(14900), r.amount)
        assertEquals(Direction.DEBIT, r.direction)
        assertEquals("Swiggy", r.rawMerchant)
        assertEquals("412345678901", r.refId)
        assertEquals(ParseTier.TIER1_REGEX, r.tier)
        assertTrue(r.confidence >= 0.85f)
    }

    @Test fun `debited template fills account and ref from shared extractors`() = runTest {
        val r = parser.parse(notif("Rs.500.00 debited from a/c XX1234 on 14-06-26. UPI Ref 123456"))!!
        assertEquals(Money(50000), r.amount)
        assertEquals(Direction.DEBIT, r.direction)
        assertEquals("XX1234", r.maskedAccount)
        assertEquals("123456", r.refId)
        assertEquals(ParseTier.TIER1_REGEX, r.tier)
    }

    @Test fun `credited template yields credit`() = runTest {
        val r = parser.parse(notif("Rs.2,000 credited to a/c XX5678 on 14-06-26"))!!
        assertEquals(Money(200000), r.amount)
        assertEquals(Direction.CREDIT, r.direction)
        assertEquals("XX5678", r.maskedAccount)
    }

    @Test fun `card spent template`() = runTest {
        val r = parser.parse(notif("Rs.899.00 spent on your HDFC card ending 4321"))!!
        assertEquals(Money(89900), r.amount)
        assertEquals(Direction.DEBIT, r.direction)
        assertEquals("XX4321", r.maskedAccount)
    }

    @Test fun `hdfc sent format parses as tier 1`() = runTest {
        // Real-world HDFC format (fictional identifiers): "Sent Rs.100.00 From HDFC Bank A/C *8803 To <name>"
        val r = parser.parse(notif("Sent Rs.100.00 From HDFC Bank A/C *8803 To Rohan Kapoor"))!!
        assertEquals(Money(10000), r.amount)
        assertEquals(Direction.DEBIT, r.direction)
        assertEquals("Rohan Kapoor", r.rawMerchant)
        assertEquals("XX8803", r.maskedAccount)
        assertEquals(ParseTier.TIER1_REGEX, r.tier)
    }

    @Test fun `unmatched format returns null from tier 1`() = runTest {
        assertNull(parser.parse(notif("Payment of Rs.300 from your account was debited successfully")))
    }
}
