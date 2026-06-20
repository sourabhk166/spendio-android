package com.spendio.app.parser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionDetectorTest {

    private val detector = TransactionDetector()

    @Test fun `real debit is a transaction`() {
        assertTrue(detector.looksLikeTransaction(notif("Rs.500.00 debited from a/c XX1234")))
    }

    @Test fun `real credit is a transaction`() {
        assertTrue(detector.looksLikeTransaction(notif("Rs.2000 credited to a/c XX1234")))
    }

    @Test fun `otp is rejected`() {
        assertFalse(detector.looksLikeTransaction(notif("Your OTP is 123456 for txn of Rs.500")))
    }

    @Test fun `future debit is rejected`() {
        assertFalse(detector.looksLikeTransaction(notif("Rs.500 will be debited tomorrow for your SIP")))
    }

    @Test fun `money request is rejected`() {
        assertFalse(detector.looksLikeTransaction(notif("John has requested Rs.500 via UPI")))
    }

    @Test fun `failed txn is rejected`() {
        assertFalse(detector.looksLikeTransaction(notif("Your payment of Rs.500 failed")))
    }

    @Test fun `pure balance message without verb is rejected`() {
        assertFalse(detector.looksLikeTransaction(notif("Your a/c XX1234 balance is Rs.10000")))
    }

    @Test fun `non-financial text is rejected`() {
        assertFalse(detector.looksLikeTransaction(notif("Lunch at 1pm?")))
    }

    @Test fun `reward marketing referencing a real payment is rejected`() {
        // CRED's reward push during testing: has ₹ amount + "payment ... completed" verb,
        // but it is marketing, not a money movement.
        assertFalse(
            detector.looksLikeTransaction(
                notif("good news: your payment of Rs.283 on Eatclub was completed. claim your reward"),
            ),
        )
    }

    @Test fun `you earned a reward is rejected`() {
        assertFalse(detector.looksLikeTransaction(notif("You've earned a reward of Rs.50, redeem now")))
    }

    @Test fun `genuine card spend is still a transaction`() {
        assertTrue(
            detector.looksLikeTransaction(
                notif("Rs.283.00 spent on your SBI Credit Card ending 3917 at EATCLUB on 15/06/26"),
            ),
        )
    }
}
