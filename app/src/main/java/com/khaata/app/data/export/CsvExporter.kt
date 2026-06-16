package com.khaata.app.data.export

import com.khaata.app.domain.model.CategoryCatalog
import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.Transaction
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Renders transactions as RFC-4180 CSV — one header row plus one row per transaction.
 * Pure and framework-free so it's unit-testable; the share/file-write happens in the UI layer.
 * Amounts are written as plain rupee decimals (no currency symbol) so spreadsheets parse them
 * as numbers.
 */
object CsvExporter {

    private val dateFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    private val header = listOf("Date", "Type", "Amount", "Category", "Merchant", "Note")

    fun toCsv(transactions: List<Transaction>, zone: ZoneId = ZoneId.systemDefault()): String {
        val sb = StringBuilder()
        sb.append(header.joinToString(",") { escape(it) })
        for (t in transactions) {
            val date = dateFormat.format(t.occurredAt.atZone(zone))
            val type = if (t.direction == Direction.CREDIT) "Income" else "Expense"
            val amount = "%.2f".format(Locale.US, t.amount.minorUnits / 100.0)
            val category = CategoryCatalog.info(t.categoryId).name
            val merchant = t.merchant ?: t.rawMerchant ?: ""
            val note = t.note ?: ""
            sb.append('\n')
            sb.append(listOf(date, type, amount, category, merchant, note).joinToString(",") { escape(it) })
        }
        return sb.toString()
    }

    /** Quotes a field when it contains a comma, quote or newline; doubles embedded quotes. */
    private fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
}
