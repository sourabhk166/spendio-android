package com.khaata.app.parser

import com.khaata.app.domain.model.Categories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MerchantKeywordsTest {

    @Test fun `brand tokens map to their category`() {
        assertEquals(Categories.FOOD, MerchantKeywords.categoryFor("eatclub"))
        assertEquals(Categories.FOOD, MerchantKeywords.categoryFor("swiggy"))
        assertEquals(Categories.SHOPPING, MerchantKeywords.categoryFor("amazon"))
        assertEquals(Categories.GROCERIES, MerchantKeywords.categoryFor("blinkit"))
    }

    @Test fun `token prefix matches glued brand names`() {
        assertEquals(Categories.TRANSPORT, MerchantKeywords.categoryFor("ola"))
        assertEquals(Categories.TRANSPORT, MerchantKeywords.categoryFor("olacabs"))
    }

    @Test fun `short keyword does not match inside an unrelated word`() {
        // "ola" must NOT match "chocolate" (the substring trap the token matcher avoids).
        assertNull(MerchantKeywords.categoryFor("chocolate factory"))
    }

    @Test fun `specific brand beats the generic token it contains`() {
        assertEquals(Categories.GROCERIES, MerchantKeywords.categoryFor("jiomart"))
        assertEquals(Categories.ENTERTAINMENT, MerchantKeywords.categoryFor("jiocinema"))
        assertEquals(Categories.BILLS, MerchantKeywords.categoryFor("jio recharge"))
    }

    @Test fun `unknown merchant is null`() {
        assertNull(MerchantKeywords.categoryFor("acme corp"))
    }

    @Test fun `investment platforms map to investments`() {
        assertEquals(Categories.INVESTMENTS, MerchantKeywords.categoryFor("zerodha"))
        assertEquals(Categories.INVESTMENTS, MerchantKeywords.categoryFor("groww"))
        assertEquals(Categories.INVESTMENTS, MerchantKeywords.categoryFor("upstox"))
        assertEquals(Categories.INVESTMENTS, MerchantKeywords.categoryFor("sip mandate"))
        assertEquals(Categories.INVESTMENTS, MerchantKeywords.categoryFor("mutual fund"))
    }

    @Test fun `income sub-categories resolve correctly`() {
        assertEquals(Categories.INCOME_SALARY, MerchantKeywords.categoryFor("salary credited"))
        assertEquals(Categories.INCOME_INTEREST, MerchantKeywords.categoryFor("interest credited"))
        assertEquals(Categories.INCOME_CASHBACK, MerchantKeywords.categoryFor("cashback reward"))
        // "refund" alone (no brand) should resolve to INCOME_REFUND.
        assertEquals(Categories.INCOME_REFUND, MerchantKeywords.categoryFor("refund"))
    }
}
