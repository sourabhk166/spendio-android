package com.spendio.app.data.backup

import com.spendio.app.domain.model.Categories
import com.spendio.app.domain.model.Direction
import com.spendio.app.domain.model.Money
import com.spendio.app.domain.model.ParseTier
import com.spendio.app.domain.model.Transaction
import com.spendio.app.domain.model.TxnSource
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class BackupSerializerTest {

    private fun txn(id: String, amount: Long, category: String, note: String? = null) = Transaction(
        id = id,
        amount = Money(amount),
        direction = Direction.DEBIT,
        occurredAt = Instant.parse("2026-06-15T08:00:00Z"),
        accountId = "XX1234",
        rawMerchant = "SOME, MERCHANT",
        merchant = "Some Merchant",
        categoryId = category,
        note = note,
        source = TxnSource.NOTIFICATION,
        parseTier = ParseTier.TIER1_REGEX,
        confidence = 0.9f,
        needsReview = false,
        sourcePackage = "com.bank.app",
        dedupeKey = "dk-$id",
        rawTextHash = "hash-$id",
    )

    @Test fun `encode then decode preserves every field`() {
        val original = listOf(
            txn("1", 28350, Categories.FOOD, note = "lunch, with tax"),
            txn("2", 500000, Categories.UPI),
        )
        val restored = BackupSerializer.decode(BackupSerializer.encode(original))
        assertEquals(original, restored)
    }

    @Test fun `empty list round-trips`() {
        assertEquals(emptyList<Transaction>(), BackupSerializer.decode(BackupSerializer.encode(emptyList())))
    }
}
