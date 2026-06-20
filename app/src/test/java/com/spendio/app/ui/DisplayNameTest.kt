package com.spendio.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayNameTest {

    @Test fun `user-set merchant is shown verbatim`() {
        assertEquals("My chai stall", displayName(merchant = "My chai stall", rawMerchant = "whatever@upi"))
    }

    @Test fun `vpa handle drops the bank suffix and is capitalized`() {
        assertEquals("Amitverma42", displayName(merchant = null, rawMerchant = "amitverma42@okaxis"))
    }

    @Test fun `dotted handle becomes spaced words`() {
        assertEquals("John Doe", displayName(merchant = null, rawMerchant = "john.doe@oksbi"))
    }

    @Test fun `all-caps brand names are preserved`() {
        assertEquals("EATCLUB", displayName(merchant = null, rawMerchant = "EATCLUB"))
    }

    @Test fun `blank falls back to Unknown`() {
        assertEquals("Unknown", displayName(merchant = null, rawMerchant = null))
        assertEquals("Unknown", displayName(merchant = "  ", rawMerchant = "  "))
    }
}
