package com.khaata.app.capture

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Captures bank/UPI/card notifications. The callback returns immediately and all work
 * happens off the main thread; every exception is swallowed so a bad notification can
 * never crash the listener (docs/04-lld.md §6).
 */
@AndroidEntryPoint
class TxnNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var pipeline: CapturePipeline

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val raw = sbn?.toRawNotification() ?: return
        scope.launch {
            runCatching { pipeline.process(raw) }
                .onFailure { Log.w(TAG, "Capture failed for ${raw.packageName}", it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "KhaataListener"
    }
}
