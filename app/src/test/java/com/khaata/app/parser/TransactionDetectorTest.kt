package com.khaata.app.parser

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
}
