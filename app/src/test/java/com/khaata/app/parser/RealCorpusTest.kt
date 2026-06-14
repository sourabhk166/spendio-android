package com.khaata.app.parser

import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.Money
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression tests modelled on real-world Indian bank/UPI notification formats. Account
 * digits, reference numbers, phone numbers and names are fictional; message shapes are kept.
 * Every parsing bug becomes a test case here first, then a fix.
 */
class RealCorpusTest {

    private val parser = TieredTransactionParser(
        detector = TransactionDetector(),
        regexTier = RegexTierParser(),
        heuristicTier = HeuristicParser(),
    )

    private suspend fun parse(text: String) = parser.parse(notif(text))

    @Test fun `SBI card spent parses as debit`() = runTest {
        val r = parse("Rs.162.00 spent on your SBI Credit Card ending 3917 at SwiggyLimited on 23/05/26. Trxn. not done by you? Report at https://sbicard.com/Dispute")!!
        assertEquals(Money(16200), r.amount)
        assertEquals(Direction.DEBIT, r.direction)
        assertEquals("XX3917", r.maskedAccount)
    }

    @Test fun `SBI card via UPI parses as debit`() = runTest {
        val r = parse("Rs.1,204.57 spent on your SBI Credit Card ending with 2758 at PhonePe on 30-05-26 via UPI (Ref No. 300000000101). Trxn. not done by you? Report at https://sbicard.com/Dispute")!!
        assertEquals(Money(120457), r.amount)
        assertEquals(Direction.DEBIT, r.direction)
    }

    @Test fun `HDFC card spent-prefix parses as debit`() = runTest {
        val r = parse("Spent Rs.222 On HDFC Bank Card 5521 At PiTrU*Swiggy Food On 2026-03-16 20:14:18 Not You? To Block+Reissue Call 18002586161/SMS BLOCK CC 5521 to 9000000001")!!
        assertEquals(Money(22200), r.amount)
        assertEquals(Direction.DEBIT, r.direction)
        assertEquals("XX5521", r.maskedAccount)
    }

    @Test fun `Axis card spent-prefix parses as debit with account`() = runTest {
        val r = parse("Spent INR 170 Axis Bank Card no. XX6184 04-01-26 10:04:22 IST CHEAP AND B Avl Limit: INR 150000 Not you? SMS BLOCK 6184 to 919000000002")!!
        assertEquals(Money(17000), r.amount)
        assertEquals(Direction.DEBIT, r.direction)
        assertEquals("XX6184", r.maskedAccount)
        // Must NOT capture the trailing helpline number as the merchant.
        assertNull(r.rawMerchant?.takeIf { it.all { c -> c.isDigit() } })
    }

    @Test fun `HDFC verb-less From-To is captured as debit (was dropped)`() = runTest {
        val r = parse("Rs.625.00 From HDFC Bank A/C *8803 To FRZ.COM INVESTMENT ADVISO On 30-APR-26 Ref 300000000102 Not You? Call 18002586161/SMS BLOCK UPI to 9000000001")!!
        assertEquals(Money(62500), r.amount)
        assertEquals(Direction.DEBIT, r.direction)
    }

    @Test fun `ICICI debited parses as debit`() = runTest {
        val r = parse("ICICI Bank Acc XX207 debited Rs. 20,000.00 on 04-May-26 CAM*14172HHR*. Avb Bal Rs. 12,480.50. To dispute Call 18002662 or SMS BLOCK 207 to 9000000003 .")!!
        assertEquals(Money(2000000), r.amount)
        assertEquals(Direction.DEBIT, r.direction)
        assertEquals("XX207", r.maskedAccount)
    }

    @Test fun `ICICI credited parses as credit`() = runTest {
        val r = parse("Dear Customer, Acct XX207 is credited with Rs 10000.00 on 11-Jun-25 from Arjun Mehta. UPI:300000000103-ICICI Bank.")!!
        assertEquals(Money(1000000), r.amount)
        assertEquals(Direction.CREDIT, r.direction)
        assertEquals("XX207", r.maskedAccount)
    }

    @Test fun `HDFC credit alert parses as credit`() = runTest {
        val r = parse("Credit Alert! Rs.9564.00 credited to HDFC Bank A/c XX8803 on 25-05-26 from VPA 9000000004@yescred (UPI 300000000104)")!!
        assertEquals(Money(956400), r.amount)
        assertEquals(Direction.CREDIT, r.direction)
        assertEquals("XX8803", r.maskedAccount)
    }

    @Test fun `non-transactions from the corpus are dropped`() = runTest {
        // SBI OTP (carries "INR amount at merchant" — a trap)
        assertNull(parse("482917 is the OTP for Trxn. of INR 9284.31 at BIGTREE En with your credit card ending 3917. OTP is valid for 10 mins. Do not share it with anyone - SBI Card"))
        // HDFC bill-due nudge (has a "pay" verb)
        assertNull(parse("Amount Due Rs.70 on HDFC Bank Credit Card. Pay instantly by 03/APR/2026 via PayZapp - Bill Pay > Credit Card."))
        // HDFC EMI reminder
        assertNull(parse("Your HDFC Bank Loan on Credit Card EMI is due on 03/JUN/2026. If you have opted for autopay, make sure there is balance in your A/c to pay the EMI. Ignore if paid."))
        // SBI security advisory
        assertNull(parse("Dear Cardholder, never hand over your credit card or share your card details like card number, OTP, CVV, PIN or card expiry date with anyone."))
        // UPI mandate setup
        assertNull(parse("UPI Mandate: Sent Rs.700.00 mandate created"))
    }
}
