package com.khaata.app.parser

import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountExtractorTest {

    @Test fun `extracts rupee amount with rs prefix`() {
        assertEquals(Money(50000), AmountExtractor.firstAmount("Rs.500.00 debited"))
    }

    @Test fun `extracts inr with comma grouping`() {
        assertEquals(Money(123450), AmountExtractor.firstAmount("INR 1,234.50 spent"))
    }

    @Test fun `extracts rupee symbol`() {
        assertEquals(Money(14900), AmountExtractor.firstAmount("₹149 paid to Swiggy"))
    }

    @Test fun `first amount wins over balance`() {
        // Transaction amount appears before the available-balance amount.
        assertEquals(Money(50000), AmountExtractor.firstAmount("Rs.500 debited. Avl bal Rs.20000"))
    }

    @Test fun `no currency returns null`() {
        assertNull(AmountExtractor.firstAmount("debited from your account"))
    }
}

class DirectionDetectorTest {

    @Test fun `debited is debit`() {
        assertEquals(Direction.DEBIT, DirectionDetector.detect("Rs.500 debited from a/c"))
    }

    @Test fun `credited is credit`() {
        assertEquals(Direction.CREDIT, DirectionDetector.detect("Rs.500 credited to a/c"))
    }

    @Test fun `card charged is debit`() {
        assertEquals(Direction.DEBIT, DirectionDetector.detect("Rs.899 charged on your card"))
    }

    @Test fun `earliest keyword wins when both present`() {
        // "debited" appears before "credited" (refund context).
        assertEquals(Direction.DEBIT, DirectionDetector.detect("Rs.500 debited; will be credited on reversal"))
    }

    @Test fun `prepaid does not falsely match paid`() {
        assertNull(DirectionDetector.detect("prepaid recharge plan details"))
    }
}

class AccountExtractorTest {

    @Test fun `xx masked tail`() {
        assertEquals("XX1234", AccountExtractor.maskedAccount("debited from a/c XX1234 on"))
    }

    @Test fun `long masked account`() {
        assertEquals("XX1234", AccountExtractor.maskedAccount("A/c XXXXXX1234"))
    }

    @Test fun `card ending`() {
        assertEquals("XX5678", AccountExtractor.maskedAccount("card ending 5678 used"))
    }

    @Test fun `no account returns null`() {
        assertNull(AccountExtractor.maskedAccount("payment successful"))
    }
}

class RefExtractorTest {

    @Test fun `upi ref with colon`() {
        assertEquals("412345678901", RefExtractor.extract("UPI Ref: 412345678901"))
    }

    @Test fun `upi ref no`() {
        assertEquals("123456789", RefExtractor.extract("UPI Ref no 123456789"))
    }

    @Test fun `plain ref`() {
        assertEquals("123456", RefExtractor.extract("Payment done. Ref 123456."))
    }
}

class MerchantExtractorTest {

    @Test fun `paid to merchant`() {
        assertEquals("Swiggy", MerchantExtractor.extract("₹149 paid to Swiggy via UPI"))
    }

    @Test fun `to vpa`() {
        assertTrue(MerchantExtractor.extract("Rs.500 debited to VPA swiggy@hdfc")!!.contains("swiggy"))
    }

    @Test fun `at merchant`() {
        assertEquals("Amazon", MerchantExtractor.extract("Rs.899 spent at Amazon on 14-06"))
    }
}
