package com.khaata.app.parser

import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.Money
import com.khaata.app.domain.model.ParseResult
import com.khaata.app.domain.model.ParseTier
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.Instant

class StructuralSignatureTest {

    @Test fun `same shape yields same signature`() {
        assertEquals(StructuralSignature.of("Rs.250 paid"), StructuralSignature.of("Rs.999 paid"))
    }

    @Test fun `different shape yields different signature`() {
        assertNotEquals(StructuralSignature.of("Rs.250 paid"), StructuralSignature.of("Paid Rs.250"))
    }
}

class TemplateCacheTest {

    private val aiResult = ParseResult(
        amount = Money(25000),
        direction = Direction.DEBIT,
        occurredAt = Instant.parse("2026-06-14T10:15:30Z"),
        maskedAccount = null,
        rawMerchant = "Zomato",
        confidence = 0.8f,
        tier = ParseTier.TIER2_AI,
        refId = "778899",
    )

    @Test fun `learns a template then serves it for the package`() = runTest {
        val dao = FakeParseTemplateDao()
        val cache = TemplateCache(dao)
        cache.learnFrom(notif("Zomato charged you Rs.250 via MyBank UPI ref 778899"), aiResult)

        assertEquals(1, dao.rows.size)
        val templates = cache.templatesFor("com.phonepe.app")
        assertEquals(1, templates.size)
        assertNotNull(templates[0].pattern.find("Swiggy charged you Rs.10 via MyBank UPI ref 999999"))
    }

    @Test fun `same shape is not learned twice`() = runTest {
        val dao = FakeParseTemplateDao()
        val cache = TemplateCache(dao)
        cache.learnFrom(notif("Zomato charged you Rs.250 via MyBank UPI ref 778899"), aiResult)
        cache.learnFrom(notif("Swiggy charged you Rs.999 via MyBank UPI ref 112233"), aiResult)
        assertEquals(1, dao.rows.size)
    }
}
