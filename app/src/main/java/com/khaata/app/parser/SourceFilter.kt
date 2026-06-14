package com.khaata.app.parser

/** A known financial source the app can read, with a user-facing name for the allow-list UI. */
data class KnownSource(val packageName: String, val displayName: String)

/**
 * Allowlist of packages whose notifications we even look at. Everything else is ignored
 * immediately (privacy + performance). The user can turn individual sources OFF in
 * Settings; [disabledSources] supplies those. Bank SMS also surface as notifications from
 * the device's default messaging app, so messaging packages are included; the
 * [TransactionDetector] then decides whether the message is actually a transaction.
 */
class SourceFilter(
    private val known: Set<String> = KnownSources.PACKAGES,
    private val disabledSources: suspend () -> Set<String> = { emptySet() },
) {
    suspend fun isFinancialSource(packageName: String): Boolean =
        packageName in known && packageName !in disabledSources()
}

object KnownSources {
    val ALL: List<KnownSource> = listOf(
        // UPI / payment apps
        KnownSource("com.phonepe.app", "PhonePe"),
        KnownSource("com.google.android.apps.nbu.paisa.user", "Google Pay"),
        KnownSource("net.one97.paytm", "Paytm"),
        KnownSource("in.org.npci.upiapp", "BHIM"),
        KnownSource("com.dreamplug.androidapp", "CRED"),
        KnownSource("com.whatsapp", "WhatsApp Pay"),
        // Bank apps
        KnownSource("com.snapwork.hdfc", "HDFC Bank"),
        KnownSource("com.csam.icici.bank.imobile", "ICICI iMobile"),
        KnownSource("com.sbi.lotusintouch", "YONO SBI"),
        KnownSource("com.sbi.SBIFreedomPlus", "SBI Anywhere"),
        KnownSource("com.axis.mobile", "Axis Mobile"),
        KnownSource("com.msf.kbank.mobile", "Kotak"),
        KnownSource("com.bankofbaroda.mconnect", "Bank of Baroda"),
        KnownSource("com.infrasofttech.indianbank", "Indian Bank"),
        KnownSource("com.fss.pnb", "PNB"),
        // Default messaging apps (bank SMS arrive here as notifications)
        KnownSource("com.google.android.apps.messaging", "Google Messages"),
        KnownSource("com.samsung.android.messaging", "Samsung Messages"),
        KnownSource("com.android.messaging", "Messages"),
        KnownSource("com.android.mms", "Messaging"),
    )

    val PACKAGES: Set<String> = ALL.map { it.packageName }.toSet()
}
