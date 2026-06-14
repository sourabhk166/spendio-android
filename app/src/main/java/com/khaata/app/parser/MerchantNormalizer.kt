package com.khaata.app.parser

import javax.inject.Inject

/**
 * Collapses the many raw merchant strings for one business into a stable key:
 * strips gateway prefixes (PAYTM*, UPI/), VPA handles (swiggy@hdfc -> swiggy), punctuation,
 * and common noise tokens. Best-effort — the merchant->category rule cache + user
 * corrections do the rest.
 */
class MerchantNormalizer @Inject constructor() {

    private val noise = Regex(
        """\b(pvt|ltd|limited|india|payments?|services?|store|retail|bangalore|bengaluru|mumbai|delhi|hyderabad|chennai|pune|kolkata)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun normalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        var s = raw.trim().lowercase()
        s = s.removePrefix("paytm*").removePrefix("upi/").removePrefix("vpa ")
        if (s.contains("@")) s = s.substringBefore("@")          // VPA handle
        s = s.replace(Regex("""[^a-z0-9 ]"""), " ")              // drop punctuation
        s = s.replace(noise, " ")
        s = s.replace(Regex("""\s+"""), " ").trim()
        return s.ifBlank { null }
    }
}
