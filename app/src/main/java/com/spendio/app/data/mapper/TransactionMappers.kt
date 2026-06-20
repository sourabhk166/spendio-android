package com.spendio.app.data.mapper

import com.spendio.app.data.db.TransactionEntity
import com.spendio.app.domain.model.Direction
import com.spendio.app.domain.model.Money
import com.spendio.app.domain.model.ParseTier
import com.spendio.app.domain.model.Transaction
import com.spendio.app.domain.model.TxnSource
import java.time.Instant

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amountMinor = amount.minorUnits,
    direction = direction.name,
    occurredAt = occurredAt.toEpochMilli(),
    accountId = accountId,
    rawMerchant = rawMerchant,
    merchant = merchant,
    categoryId = categoryId,
    note = note,
    source = source.name,
    parseTier = parseTier.name,
    confidence = confidence,
    needsReview = needsReview,
    sourcePackage = sourcePackage,
    dedupeKey = dedupeKey,
    rawTextHash = rawTextHash,
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amount = Money(amountMinor),
    direction = Direction.valueOf(direction),
    occurredAt = Instant.ofEpochMilli(occurredAt),
    accountId = accountId,
    rawMerchant = rawMerchant,
    merchant = merchant,
    categoryId = categoryId,
    note = note,
    source = TxnSource.valueOf(source),
    parseTier = ParseTier.valueOf(parseTier),
    confidence = confidence,
    needsReview = needsReview,
    sourcePackage = sourcePackage,
    dedupeKey = dedupeKey,
    rawTextHash = rawTextHash,
)
