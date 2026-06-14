package com.khaata.app.capture

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.khaata.app.domain.model.RawNotification
import java.time.Instant

/** Converts the Android framework's [StatusBarNotification] into our framework-free
 *  [RawNotification]. Prefers the expanded "big text" body when present. */
fun StatusBarNotification.toRawNotification(): RawNotification? {
    val extras = notification?.extras ?: return null
    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
    val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
    val body = bigText?.takeIf { it.isNotBlank() } ?: text

    if (title.isNullOrBlank() && body.isNullOrBlank()) return null

    return RawNotification(
        packageName = packageName,
        title = title,
        text = body,
        postedAt = Instant.ofEpochMilli(postTime),
    )
}
