package com.khaata.app.parser

/**
 * Allowlist of packages whose notifications we even look at. Everything else is
 * ignored immediately (privacy + performance). Bank SMS also surface as notifications
 * from the device's default messaging app, so common messaging packages are included;
 * the [TransactionDetector] then decides whether the message is actually a transaction.
 */
class SourceFilter(
    private val allowed: Set<String> = DEFAULT_FINANCIAL_PACKAGES,
) {
    fun isFinancialSource(packageName: String): Boolean = packageName in allowed

    companion object {
        val DEFAULT_FINANCIAL_PACKAGES: Set<String> = setOf(
            // UPI / payment apps
            "com.phonepe.app",                              // PhonePe
            "com.google.android.apps.nbu.paisa.user",       // Google Pay (India)
            "net.one97.paytm",                              // Paytm
            "in.org.npci.upiapp",                           // BHIM
            "com.dreamplug.androidapp",                     // CRED
            "com.whatsapp",                                 // WhatsApp Pay (noisy; detector filters)
            // Bank apps
            "com.snapwork.hdfc",                            // HDFC Bank
            "com.csam.icici.bank.imobile",                  // ICICI iMobile
            "com.sbi.lotusintouch",                         // YONO SBI
            "com.sbi.SBIFreedomPlus",                       // SBI Anywhere
            "com.axis.mobile",                              // Axis
            "com.msf.kbank.mobile",                         // Kotak
            "com.bankofbaroda.mconnect",                    // BoB
            "com.infrasofttech.indianbank",                 // Indian Bank
            "com.fss.pnb",                                  // PNB
            // Default messaging apps (bank SMS arrive here as notifications)
            "com.google.android.apps.messaging",            // Google Messages
            "com.samsung.android.messaging",                // Samsung Messages
            "com.android.messaging",                        // AOSP Messaging
            "com.android.mms",                              // AOSP MMS
        )
    }
}
