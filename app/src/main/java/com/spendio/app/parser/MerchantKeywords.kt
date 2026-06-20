package com.spendio.app.parser

import com.spendio.app.domain.model.Categories

/**
 * Deterministic merchant-keyword → category mapping. This runs on *every* device,
 * regardless of on-device AI availability (Gemini Nano only exists on a handful of
 * recent Pixels), so in practice this is what categorizes the large majority of real
 * transactions. The AI tier, when present, only handles merchants this dictionary misses.
 *
 * Matching is first-hit over an ordered list against the normalized merchant key, so more
 * specific brands are listed before the generic token they contain (e.g. `jiomart` /
 * `jiocinema` precede the bare `jio` recharge keyword). User corrections and the learned
 * rule cache still override everything here.
 */
object MerchantKeywords {

    // Ordered: specific-before-generic. First keyword contained in the merchant wins.
    private val entries: List<Pair<String, String>> = listOf(
        // ── Groceries (before Food, so "swiggy instamart" → groceries) ──────────────
        "instamart" to Categories.GROCERIES,
        "bigbasket" to Categories.GROCERIES,
        "blinkit" to Categories.GROCERIES,
        "zepto" to Categories.GROCERIES,
        "grofers" to Categories.GROCERIES,
        "jiomart" to Categories.GROCERIES,
        "dmart" to Categories.GROCERIES,
        "milkbasket" to Categories.GROCERIES,
        "reliance fresh" to Categories.GROCERIES,
        "spencers" to Categories.GROCERIES,
        "supermarket" to Categories.GROCERIES,
        "grocery" to Categories.GROCERIES,
        "kirana" to Categories.GROCERIES,

        // ── Food & Dining ───────────────────────────────────────────────────────────
        "swiggy" to Categories.FOOD,
        "zomato" to Categories.FOOD,
        "eatclub" to Categories.FOOD,
        "eat club" to Categories.FOOD,
        "dominos" to Categories.FOOD,
        "mcdonald" to Categories.FOOD,
        "burger" to Categories.FOOD,
        "pizza" to Categories.FOOD,
        "biryani" to Categories.FOOD,
        "restaurant" to Categories.FOOD,
        "bakery" to Categories.FOOD,
        "starbucks" to Categories.FOOD,
        "dunkin" to Categories.FOOD,
        "subway" to Categories.FOOD,
        "faasos" to Categories.FOOD,
        "behrouz" to Categories.FOOD,
        "freshmenu" to Categories.FOOD,
        "haldiram" to Categories.FOOD,
        "barbeque" to Categories.FOOD,
        "chaayos" to Categories.FOOD,
        "kfc" to Categories.FOOD,

        // ── Transport ───────────────────────────────────────────────────────────────
        "uber" to Categories.TRANSPORT,
        "rapido" to Categories.TRANSPORT,
        "namma yatri" to Categories.TRANSPORT,
        "blusmart" to Categories.TRANSPORT,
        "irctc" to Categories.TRANSPORT,
        "redbus" to Categories.TRANSPORT,
        "fastag" to Categories.TRANSPORT,
        "petrol" to Categories.TRANSPORT,
        "indianoil" to Categories.TRANSPORT,
        "indian oil" to Categories.TRANSPORT,
        "hpcl" to Categories.TRANSPORT,
        "bpcl" to Categories.TRANSPORT,
        "iocl" to Categories.TRANSPORT,
        "railway" to Categories.TRANSPORT,
        "ola" to Categories.TRANSPORT,

        // ── Shopping ────────────────────────────────────────────────────────────────
        "amazon" to Categories.SHOPPING,
        "flipkart" to Categories.SHOPPING,
        "myntra" to Categories.SHOPPING,
        "ajio" to Categories.SHOPPING,
        "meesho" to Categories.SHOPPING,
        "nykaa" to Categories.SHOPPING,
        "tatacliq" to Categories.SHOPPING,
        "lifestyle" to Categories.SHOPPING,
        "decathlon" to Categories.SHOPPING,
        "croma" to Categories.SHOPPING,
        "lenskart" to Categories.SHOPPING,
        "snapdeal" to Categories.SHOPPING,
        "firstcry" to Categories.SHOPPING,
        "pepperfry" to Categories.SHOPPING,

        // ── Entertainment (before Bills, so jiocinema → entertainment) ──────────────
        "netflix" to Categories.ENTERTAINMENT,
        "spotify" to Categories.ENTERTAINMENT,
        "hotstar" to Categories.ENTERTAINMENT,
        "disney" to Categories.ENTERTAINMENT,
        "sonyliv" to Categories.ENTERTAINMENT,
        "sony liv" to Categories.ENTERTAINMENT,
        "zee5" to Categories.ENTERTAINMENT,
        "jiocinema" to Categories.ENTERTAINMENT,
        "bookmyshow" to Categories.ENTERTAINMENT,
        "pvr" to Categories.ENTERTAINMENT,
        "inox" to Categories.ENTERTAINMENT,
        "gaana" to Categories.ENTERTAINMENT,
        "audible" to Categories.ENTERTAINMENT,
        "playstation" to Categories.ENTERTAINMENT,

        // ── Health ──────────────────────────────────────────────────────────────────
        "pharmeasy" to Categories.HEALTH,
        "netmeds" to Categories.HEALTH,
        "1mg" to Categories.HEALTH,
        "apollo" to Categories.HEALTH,
        "medplus" to Categories.HEALTH,
        "pharmacy" to Categories.HEALTH,
        "hospital" to Categories.HEALTH,
        "diagnostic" to Categories.HEALTH,
        "practo" to Categories.HEALTH,
        "cultfit" to Categories.HEALTH,
        "healthkart" to Categories.HEALTH,
        "chemist" to Categories.HEALTH,

        // ── Bills & Utilities (generic tokens last) ─────────────────────────────────
        "airtel" to Categories.BILLS,
        "vodafone" to Categories.BILLS,
        "bsnl" to Categories.BILLS,
        "tata power" to Categories.BILLS,
        "tatasky" to Categories.BILLS,
        "dish tv" to Categories.BILLS,
        "act fibernet" to Categories.BILLS,
        "hathway" to Categories.BILLS,
        "bharat gas" to Categories.BILLS,
        "indane" to Categories.BILLS,
        "electricity" to Categories.BILLS,
        "broadband" to Categories.BILLS,
        "postpaid" to Categories.BILLS,
        "recharge" to Categories.BILLS,
        "jio" to Categories.BILLS,

        // ── Investments ─────────────────────────────────────────────────────────────
        "zerodha" to Categories.INVESTMENTS,
        "groww" to Categories.INVESTMENTS,
        "indmoney" to Categories.INVESTMENTS,
        "ind money" to Categories.INVESTMENTS,
        "angelone" to Categories.INVESTMENTS,
        "angel one" to Categories.INVESTMENTS,
        "upstox" to Categories.INVESTMENTS,
        "paytm money" to Categories.INVESTMENTS,
        "smallcase" to Categories.INVESTMENTS,
        "wealthdesk" to Categories.INVESTMENTS,
        "sip" to Categories.INVESTMENTS,
        "mutual fund" to Categories.INVESTMENTS,

        // ── Income (specific sub-types before generic) ───────────────────────────────
        "interest credited" to Categories.INCOME_INTEREST,
        "interest earned" to Categories.INCOME_INTEREST,
        "dividend" to Categories.INCOME_INTEREST,
        "cashback" to Categories.INCOME_CASHBACK,
        "cash back" to Categories.INCOME_CASHBACK,
        "refund" to Categories.INCOME_REFUND,
        "salary" to Categories.INCOME_SALARY,
    )

    /**
     * First category whose keyword matches [normalizedMerchant], or null. Matching is
     * token-aware so a bare "ola" hits the merchant "ola" / "olacabs" but not "chocolate":
     *   - multi-word keywords ("tata power") match as a substring phrase
     *   - single-word keywords match a token exactly or as a token prefix
     */
    fun categoryFor(normalizedMerchant: String): String? {
        val s = normalizedMerchant.lowercase()
        val tokens = s.split(' ').filter { it.isNotBlank() }
        return entries.firstOrNull { (keyword, _) ->
            if (keyword.contains(' ')) {
                s.contains(keyword)
            } else {
                tokens.any { it == keyword || it.startsWith(keyword) }
            }
        }?.second
    }
}
