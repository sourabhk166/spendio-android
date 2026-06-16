package com.khaata.app.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class ExportRangeTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    @Test fun `all time spans the full epoch range`() {
        val (from, to) = ExportRange.AllTime.bounds(zone)
        assertEquals(0L, from)
        assertEquals(Long.MAX_VALUE, to)
    }

    @Test fun `custom range returns its own bounds`() {
        val (from, to) = ExportRange.Custom(1000L, 2000L).bounds(zone)
        assertEquals(1000L, from)
        assertEquals(2000L, to)
    }

    @Test fun `preset ranges are ordered from before to`() {
        listOf(ExportRange.ThisMonth, ExportRange.Last3Months, ExportRange.ThisYear).forEach { range ->
            val (from, to) = range.bounds(zone)
            assertTrue("$range should have from <= to", from <= to)
        }
    }

    @Test fun `last 3 months starts no later than this month`() {
        val (m, _) = ExportRange.ThisMonth.bounds(zone)
        val (q, _) = ExportRange.Last3Months.bounds(zone)
        assertTrue(q <= m)
    }
}
