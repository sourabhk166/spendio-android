package com.khaata.app.data.export

import com.khaata.app.domain.model.Categories
import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.Money
import com.khaata.app.domain.model.ParseTier
import com.khaata.app.domain.model.Transaction
import com.khaata.app.domain.model.TxnSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class CsvExporterTest {

    private fun txn(
        amountMinor: Long,
        direction: Direction,
        merchant: String?,
        categoryId: String,
        note: String? = null,
        at: String = "2026-06-15T08:00:00Z",
    ) = Transaction(
        id = "id-$merchant-$amountMinor",
        amount = Money(amountMinor),
        direction = direction,
        occurredAt = Instant.parse(at),
        accountId = null,
        rawMerchant = merchant,
        merchant = merchant,
        categoryId = categoryId,
        note = note,
        source = TxnSource.MANUAL,
        parseTier = ParseTier.MANUAL,
        confidence = 1f,
        needsReview = false,
        sourcePackage = null,
        dedupeKey = "k-$merchant-$amountMinor",
        rawTextHash = null,
    )

    private val utc = ZoneId.of("UTC")

    @Test fun `header is always present`() {
        val csv = CsvExporter.toCsv(emptyList())
        assertEquals("Date,Type,Amount,Category,Merchant,Note", csv)
    }

    @Test fun `rows render with type, rupee amount and category name`() {
        val csv = CsvExporter.toCsv(
            listOf(
                txn(28350, Direction.DEBIT, "Swiggy", Categories.FOOD),
                txn(5000000, Direction.CREDIT, "Acme Corp", Categories.INCOME_SALARY),
            ),
            zone = utc,
        )
        val lines = csv.lines()
        assertEquals(3, lines.size)
        assertEquals("2026-06-15,Expense,283.50,Food & Dining,Swiggy,", lines[1])
        assertEquals("2026-06-15,Income,50000.00,Salary,Acme Corp,", lines[2])
    }

    @Test fun `fields containing commas or quotes are escaped`() {
        val csv = CsvExporter.toCsv(
            listOf(txn(10000, Direction.DEBIT, "Big Bazaar, Pune", Categories.GROCERIES, note = "said \"hi\"")),
            zone = utc,
        )
        val row = csv.lines()[1]
        assertTrue(row.contains("\"Big Bazaar, Pune\""))
        assertTrue(row.contains("\"said \"\"hi\"\"\""))
    }
}
