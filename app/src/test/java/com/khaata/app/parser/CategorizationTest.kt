package com.khaata.app.parser

import com.khaata.app.ai.NoOpAiEngine
import com.khaata.app.domain.model.Categories
import com.khaata.app.domain.repository.RuleRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class MapRuleRepository : RuleRepository {
    val map = mutableMapOf<String, String>()
    override suspend fun categoryFor(merchantKey: String): String? = map[merchantKey]
    override suspend fun put(merchantKey: String, categoryId: String, userConfirmed: Boolean) {
        map[merchantKey] = categoryId
    }
}

class MerchantNormalizerTest {
    private val n = MerchantNormalizer()

    @Test fun `strips paytm gateway prefix`() = assertEquals("swiggy", n.normalize("PAYTM*SWIGGY"))
    @Test fun `strips vpa handle`() = assertEquals("swiggy", n.normalize("swiggy@okhdfcbank"))
    @Test fun `removes noise tokens`() = assertEquals("zomato", n.normalize("Zomato Pvt Ltd"))
    @Test fun `null and blank are null`() {
        assertNull(n.normalize(null))
        assertNull(n.normalize("   "))
    }
}

class CategorizerTest {

    @Test fun `cached rule wins without calling ai`() = runTest {
        val rules = MapRuleRepository().apply { map["swiggy"] = Categories.FOOD }
        val ai = FakeAiEngine(category = Categories.SHOPPING)
        val c = Categorizer(rules, ai, MerchantNormalizer())
        assertEquals(Categories.FOOD, c.categorize("Swiggy").categoryId)
    }

    // "Acme" is deliberately a merchant the keyword dictionary does NOT know, so these
    // tests exercise the AI tier that sits behind it (Amazon/Swiggy/etc. now short-circuit
    // at the keyword tier).
    @Test fun `ai prediction is used and cached`() = runTest {
        val rules = MapRuleRepository()
        val c = Categorizer(rules, FakeAiEngine(category = Categories.SHOPPING), MerchantNormalizer())
        assertEquals(Categories.SHOPPING, c.categorize("Acme").categoryId)
        assertEquals(Categories.SHOPPING, rules.categoryFor("acme"))
    }

    @Test fun `user correction overrides the ai guess`() = runTest {
        val rules = MapRuleRepository()
        val c = Categorizer(rules, FakeAiEngine(category = Categories.SHOPPING), MerchantNormalizer())
        c.categorize("Acme")
        c.correct("Acme", Categories.GROCERIES)
        assertEquals(Categories.GROCERIES, c.categorize("Acme").categoryId)
    }

    @Test fun `unknown category from ai is ignored`() = runTest {
        val c = Categorizer(MapRuleRepository(), FakeAiEngine(category = "not-a-real-category"), MerchantNormalizer())
        assertEquals(Categories.UNCATEGORIZED, c.categorize("Acme").categoryId)
    }

    @Test fun `no merchant and no-op ai stay uncategorized`() = runTest {
        val c = Categorizer(MapRuleRepository(), NoOpAiEngine, MerchantNormalizer())
        assertEquals(Categories.UNCATEGORIZED, c.categorize(null).categoryId)
        // Single-token unknown merchant: no keyword match, no P2P heuristic, no AI → uncategorized.
        assertEquals(Categories.UNCATEGORIZED, c.categorize("Xyzacme").categoryId)
    }

    @Test fun `two-word person name is transfers and flagged for review`() = runTest {
        val c = Categorizer(MapRuleRepository(), NoOpAiEngine, MerchantNormalizer())
        val rahul = c.categorize("RAHUL SHARMA")
        assertEquals(Categories.TRANSFERS, rahul.categoryId)
        assertTrue("P2P person transfers must be surfaced for review", rahul.needsReview)
        assertEquals(Categories.TRANSFERS, c.categorize("Karan Malhotra").categoryId)
    }

    @Test fun `known brand two-word name is not mistaken for a person`() = runTest {
        val c = Categorizer(MapRuleRepository(), NoOpAiEngine, MerchantNormalizer())
        // "Jio Recharge" — "jio" keyword hits before the P2P heuristic, and is not reviewed.
        val jio = c.categorize("Jio Recharge")
        assertEquals(Categories.BILLS, jio.categoryId)
        assertFalse(jio.needsReview)
    }

    @Test fun `keyword dictionary categorizes known merchants without ai`() = runTest {
        // NoOp AI = the common case on non-Pixel devices. The keyword tier must still work.
        val c = Categorizer(MapRuleRepository(), NoOpAiEngine, MerchantNormalizer())
        assertEquals(Categories.FOOD, c.categorize("EATCLUB").categoryId)
        assertEquals(Categories.FOOD, c.categorize("Swiggy").categoryId)
        assertEquals(Categories.TRANSPORT, c.categorize("UBER India").categoryId)
        assertEquals(Categories.SHOPPING, c.categorize("AMAZON").categoryId)
        assertEquals(Categories.GROCERIES, c.categorize("Blinkit").categoryId)
    }

    @Test fun `keyword tier does not persist a guess as a rule`() = runTest {
        val rules = MapRuleRepository()
        val c = Categorizer(rules, NoOpAiEngine, MerchantNormalizer())
        assertEquals(Categories.FOOD, c.categorize("Swiggy").categoryId)
        // Deterministic dictionary, so nothing is cached — unlike an AI guess.
        assertNull(rules.categoryFor("swiggy"))
    }

    @Test fun `user correction still overrides a keyword match`() = runTest {
        val rules = MapRuleRepository()
        val c = Categorizer(rules, NoOpAiEngine, MerchantNormalizer())
        c.correct("Swiggy", Categories.OTHER)
        assertEquals(Categories.OTHER, c.categorize("Swiggy").categoryId)
    }
}
