package com.khaata.app.parser

import com.khaata.app.ai.NoOpAiEngine
import com.khaata.app.domain.model.Money
import com.khaata.app.domain.model.ParseTier
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiTierParserTest {

    @Test fun `maps ai extraction to a tier-2 result`() = runTest {
        val parser = AiTierParser(FakeAiEngine())
        val r = parser.parse(notif("Zomato charged you Rs.250 via MyBank"))!!
        assertEquals(Money(25000), r.amount)
        assertEquals(ParseTier.TIER2_AI, r.tier)
    }

    @Test fun `no-op engine yields null`() = runTest {
        assertNull(AiTierParser(NoOpAiEngine).parse(notif("Zomato charged you Rs.250 via MyBank")))
    }
}

class SelfLearningTest {

    @Test fun `unseen format is AI-parsed once, then becomes a free tier-1 hit`() = runTest {
        val ai = FakeAiEngine()
        val cache = TemplateCache(FakeParseTemplateDao())
        val parser = TieredTransactionParser(
            detector = TransactionDetector(),
            regexTier = RegexTierParser(learnedTemplates = { pkg -> cache.templatesFor(pkg) }),
            heuristicTier = HeuristicParser(),
            aiTier = AiTierParser(ai),
            templateCache = cache,
        )

        // 1st time: no template matches -> AI parses it -> a template is learned.
        val first = parser.parse(notif("Zomato charged you Rs.250 via MyBank UPI ref 778899"))!!
        assertEquals(ParseTier.TIER2_AI, first.tier)
        assertEquals(1, ai.extractCalls)

        // 2nd time, same shape different values: Tier 1 hits the learned template, no AI.
        val second = parser.parse(notif("Swiggy charged you Rs.999 via MyBank UPI ref 112233"))!!
        assertEquals(ParseTier.TIER1_REGEX, second.tier)
        assertEquals(Money(99900), second.amount)
        assertEquals("Swiggy", second.rawMerchant)
        assertEquals(1, ai.extractCalls) // AI was NOT called again — the whole point
    }
}
