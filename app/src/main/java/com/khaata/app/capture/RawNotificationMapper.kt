package com.khaata.app.capture

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.khaata.app.domain.model.RawNotification
import java.time.Instant

/**
 * Converts the Android framework's [StatusBarNotification] into one or more framework-free
 * [RawNotification]s.
 *
 * Bank / UPI / card alerts that surface through a messaging app are frequently delivered as a
 * **MessagingStyle** (RCS/SMS conversation) or **InboxStyle** (multi-line summary) notification
 * whenever an earlier message in the same thread is still unread. In those cases each message's
 * text lives in `EXTRA_MESSAGES` / `EXTRA_TEXT_LINES`, **not** `EXTRA_TEXT` — so reading only
 * `EXTRA_TEXT` silently drops every alert after the first unread one (the bug where a ₹273 spend
 * never got captured because an earlier alert in the same SBI conversation was unread).
 *
 * We therefore expand every carried message into its own [RawNotification]. Re-reading the whole
 * conversation on each new post is intentional and safe: the capture pipeline dedupes on a stable
 * per-message key, so already-seen messages are no-ops.
 */
fun StatusBarNotification.toRawNotifications(): List<RawNotification> {
    val n = notification ?: return emptyList()
    val extras = n.extras ?: return emptyList()
    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()

    // 1) MessagingStyle (conversations): one RawNotification per message, each with its own time.
    NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)?.messages
        ?.mapNotNull { msg ->
            val body = msg.text?.toString()?.trim()?.ifBlank { null } ?: return@mapNotNull null
            RawNotification(
                packageName = packageName,
                title = title,
                text = body,
                postedAt = Instant.ofEpochMilli(msg.timestamp.takeIf { it > 0L } ?: postTime),
            )
        }
        ?.takeIf { it.isNotEmpty() }
        ?.let { return it }

    // 2) InboxStyle (multi-line summaries): one RawNotification per line.
    extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        ?.mapNotNull { it?.toString()?.trim()?.ifBlank { null } }
        ?.takeIf { it.isNotEmpty() }
        ?.map { line ->
            RawNotification(
                packageName = packageName,
                title = title,
                text = line,
                postedAt = Instant.ofEpochMilli(postTime),
            )
        }
        ?.let { return it }

    // 3) Standard single notification: prefer the expanded "big text" body.
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
    val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
    val body = bigText?.takeIf { it.isNotBlank() } ?: text
    if (title.isNullOrBlank() && body.isNullOrBlank()) return emptyList()

    return listOf(
        RawNotification(
            packageName = packageName,
            title = title,
            text = body,
            postedAt = Instant.ofEpochMilli(postTime),
        ),
    )
}
