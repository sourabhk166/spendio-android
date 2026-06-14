package com.khaata.app.parser

import com.khaata.app.ai.NoOpAiEngine
import com.khaata.app.domain.model.Categories
import com.khaata.app.domain.repository.RuleRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals(Categories.FOOD, c.categorize("Swiggy"))
    }

    @Test fun `ai prediction is used and cached`() = runTest {
        val rules = MapRuleRepository()
        val c = Categorizer(rules, FakeAiEngine(category = Categories.SHOPPING), MerchantNormalizer())
        assertEquals(Categories.SHOPPING, c.categorize("Amazon"))
        assertEquals(Categories.SHOPPING, rules.categoryFor("amazon"))
    }

    @Test fun `user correction overrides the ai guess`() = runTest {
        val rules = MapRuleRepository()
        val c = Categorizer(rules, FakeAiEngine(category = Categories.SHOPPING), MerchantNormalizer())
        c.categorize("Amazon")
        c.correct("Amazon", Categories.GROCERIES)
        assertEquals(Categories.GROCERIES, c.categorize("Amazon"))
    }

    @Test fun `unknown category from ai is ignored`() = runTest {
        val c = Categorizer(MapRuleRepository(), FakeAiEngine(category = "not-a-real-category"), MerchantNormalizer())
        assertEquals(Categories.UNCATEGORIZED, c.categorize("Amazon"))
    }

    @Test fun `no merchant and no-op ai stay uncategorized`() = runTest {
        val c = Categorizer(MapRuleRepository(), NoOpAiEngine, MerchantNormalizer())
        assertEquals(Categories.UNCATEGORIZED, c.categorize(null))
        assertEquals(Categories.UNCATEGORIZED, c.categorize("Some New Merchant"))
    }
}
