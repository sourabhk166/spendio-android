package com.spendio.app.parser

import com.spendio.app.domain.model.RawNotification
import java.time.Instant

/** Builds a RawNotification for tests with a fixed default timestamp. */
fun notif(
    text: String,
    title: String? = null,
    packageName: String = "com.phonepe.app",
    postedAt: Instant = Instant.parse("2026-06-14T10:15:30Z"),
): RawNotification = RawNotification(
    packageName = packageName,
    title = title,
    text = text,
    postedAt = postedAt,
)
