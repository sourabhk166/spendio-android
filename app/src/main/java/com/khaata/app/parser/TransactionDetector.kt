package com.khaata.app.parser

import com.khaata.app.domain.model.RawNotification

/**
 * Decides whether a notification is a *completed* money movement worth parsing.
 * Rule (docs/04-lld.md §8): has a currency amount AND a debit/credit verb AND is
 * not in the denylist (OTP, requests, failures, reminders, promos, future debits).
 */
class TransactionDetector(
    private val denylist: List<String> = DEFAULT_DENYLIST,
) {
    fun looksLikeTransaction(raw: RawNotification): Boolean {
        val text = raw.content
        if (text.isBlank()) return false
        val lower = text.lowercase()
        if (denylist.any { lower.contains(it) }) return false
        val hasAmount = AmountExtractor.firstAmount(text) != null
        val hasDirection = DirectionDetector.detect(text) != null
        return hasAmount && hasDirection
    }

    companion object {
        val DEFAULT_DENYLIST: List<String> = listOf(
            // OTP / verification
            "otp", "one time password", "one-time password", "verification code", "secure code",
            // money requests (not a completed txn)
            "requested", "is requesting", "request money", "collect request", "payment request",
            "has requested",
            // future / scheduled / reminders / bill nudges
            "will be debited", "will be deducted", "will be charged", "is due", "due on",
            "due date", "reminder", "e-mandate", "mandate", "autopay", "scheduled",
            "amount due", "pay instantly", "ignore if paid", "min amount due",
            // failures (handled separately later, dropped for now)
            "failed", "declined", "unsuccessful", "not been processed", "could not be",
            // promos / rewards / marketing (not real money movement). These often *reference*
            // a real payment ("your payment of ₹283 ... was completed, claim your reward") so
            // they have an amount + a debit-ish verb and would otherwise pass — e.g. CRED's
            // reward push that hijacked a genuine SBI card spend during testing.
            "promo", "offer ", "win ", "voucher", "flat ", "discount", "sale ",
            "cashback earned", "unlock", "scratch card", "reward point", "you earned",
            "earned a reward", "claim your reward", "claim now", "your reward",
            "good news", "congratulations", "congrats", "you've won", "youve won",
            "lucky", "giveaway", "redeem",
        )
    }
}
