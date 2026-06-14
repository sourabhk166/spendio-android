package com.khaata.app.domain

import com.khaata.app.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test fun `whole rupees parse to paise`() {
        assertEquals(Money(50000), Money.fromRupeeString("500"))
    }

    @Test fun `decimals parse to paise`() {
        assertEquals(Money(50050), Money.fromRupeeString("500.50"))
    }

    @Test fun `single decimal digit is padded`() {
        assertEquals(Money(50050), Money.fromRupeeString("500.5"))
    }

    @Test fun `commas are stripped including indian grouping`() {
        assertEquals(Money(12345678), Money.fromRupeeString("1,23,456.78"))
    }

    @Test fun `blank is null`() {
        assertNull(Money.fromRupeeString("   "))
    }

    @Test fun `arithmetic stays in paise`() {
        assertEquals(Money(150), Money(100) + Money(50))
        assertEquals(Money(50), Money(100) - Money(50))
    }
}
