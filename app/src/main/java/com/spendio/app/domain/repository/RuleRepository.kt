package com.spendio.app.domain.repository

interface RuleRepository {
    suspend fun categoryFor(merchantKey: String): String?

    /** Stores a mapping. A user-confirmed rule overwrites; an AI guess never clobbers one. */
    suspend fun put(merchantKey: String, categoryId: String, userConfirmed: Boolean)
}
