package com.spendio.app.data

import com.spendio.app.data.db.MerchantRuleDao
import com.spendio.app.data.db.ParseTemplateDao
import com.spendio.app.data.db.RawCaptureDao
import com.spendio.app.data.db.TransactionDao
import javax.inject.Inject

/** One-shot "delete everything" used by the Settings wipe action. Clears all tables —
 *  transactions, learned templates, merchant rules, and the debug capture log. */
class DataMaintenance @Inject constructor(
    private val transactionDao: TransactionDao,
    private val parseTemplateDao: ParseTemplateDao,
    private val merchantRuleDao: MerchantRuleDao,
    private val rawCaptureDao: RawCaptureDao,
) {
    suspend fun wipeAll() {
        transactionDao.wipe()
        parseTemplateDao.wipe()
        merchantRuleDao.wipe()
        rawCaptureDao.wipe()
    }
}
