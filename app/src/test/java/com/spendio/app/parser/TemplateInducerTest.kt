package com.spendio.app.parser

import com.spendio.app.domain.model.Direction
import com.spendio.app.domain.model.Money
import com.spendio.app.domain.model.ParseResult
import com.spendio.app.domain.model.ParseTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant

class TemplateInducerTest {

    private fun result(
        amount: Long,
        direction: Direction = Direction.DEBIT,
        merchant: String? = null,
        account: String? = null,
        ref: String? = null,
    ) = ParseResult(
        amount = Money(amount),
        direction = direction,
        occurredAt = Instant.parse("2026-06-14T10:15:30Z"),
        maskedAccount = account,
        rawMerchant = merchant,
        confidence = 0.8f,
        tier = ParseTier.TIER2_AI,
        refId = ref,
    )

    @Test fun `induced template reparses the same format with different values`() {
        // One AI-parsed example of an unseen format...
        val example = "Zomato charged you Rs.250 via MyBank UPI ref 778899"
        val template = TemplateInducer.induce(
            example,
            result(amount = 25000, merchant = "Zomato", ref = "778899"),
        )
        assertNotNull(template)

        // ...should now match a *different* message of the same shape — for free.
        val next = "Swiggy charged you Rs.999 via MyBank UPI ref 112233"
        val m = template!!.pattern.find(next)!!
        assertEquals("999", m.groups["amount"]!!.value)
        assertEquals("Swiggy", m.groups["merchant"]!!.value)
        assertEquals(Direction.DEBIT, template.direction)
    }

    @Test fun `static numbers like dates are generalized not frozen`() {
        val template = TemplateInducer.induce(
            "Rs.500 debited on 14-06-2026 from MyBank",
            result(amount = 50000),
        )!!
        // A later date must still match.
        assertNotNull(template.pattern.find("Rs.500 debited on 15-07-2026 from MyBank"))
    }

    @Test fun `without an amount no template can be induced`() {
        // Amount is mandatory for induction; a result whose text has no currency token.
        val template = TemplateInducer.induce("payment done", result(amount = 100))
        assertEquals(null, template)
    }
}
