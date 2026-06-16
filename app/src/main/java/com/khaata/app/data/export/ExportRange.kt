package com.khaata.app.data.export

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** The time window a CSV export covers. */
sealed interface ExportRange {
    data object ThisMonth : ExportRange
    data object Last3Months : ExportRange
    data object ThisYear : ExportRange
    data object AllTime : ExportRange
    data class Custom(val fromMillis: Long, val toMillis: Long) : ExportRange

    /** Inclusive epoch-millis bounds for this range, in the given [zone]. */
    fun bounds(zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val today = LocalDate.now(zone)
        val endOfToday = today.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        return when (this) {
            ThisMonth -> {
                val ym = YearMonth.now(zone)
                ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() to endOfToday
            }
            Last3Months -> {
                val start = YearMonth.now(zone).minusMonths(2).atDay(1)
                start.atStartOfDay(zone).toInstant().toEpochMilli() to endOfToday
            }
            ThisYear -> {
                val start = LocalDate.of(today.year, 1, 1)
                start.atStartOfDay(zone).toInstant().toEpochMilli() to endOfToday
            }
            AllTime -> 0L to Long.MAX_VALUE
            is Custom -> fromMillis to toMillis
        }
    }
}
