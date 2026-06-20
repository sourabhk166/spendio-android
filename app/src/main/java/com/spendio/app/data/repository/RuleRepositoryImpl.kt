package com.spendio.app.data.repository

import com.spendio.app.data.db.MerchantRuleDao
import com.spendio.app.data.db.MerchantRuleEntity
import com.spendio.app.domain.repository.RuleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepositoryImpl @Inject constructor(
    private val dao: MerchantRuleDao,
) : RuleRepository {

    override suspend fun categoryFor(merchantKey: String): String? = dao.categoryFor(merchantKey)

    override suspend fun put(merchantKey: String, categoryId: String, userConfirmed: Boolean) {
        val rule = MerchantRuleEntity(merchantKey, categoryId, userConfirmed)
        if (userConfirmed) dao.upsert(rule) else dao.insertIfAbsent(rule)
    }
}
