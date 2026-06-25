package com.spendio.app.capture

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Aggressive OEM battery management (MIUI, ColorOS, FuntouchOS, and stock Android's own Doze)
 * is the most common reason [TxnNotificationListenerService] silently stops receiving callbacks
 * after a while — the OS kills it like any other background process, with no crash and no log
 * the user would ever see. Exempting the app from battery optimization is the one mitigation
 * with a real platform API; OEM-specific "autostart"/"protected apps" settings have no API at
 * all and have to be hand-rolled per manufacturer if ever needed.
 */
object BatteryOptimization {
    fun isIgnoring(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java)
        return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    fun requestIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
}
