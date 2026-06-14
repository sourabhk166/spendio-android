package com.khaata.app.data

import com.khaata.app.data.db.MerchantRuleDao
import com.khaata.app.data.db.ParseTemplateDao
import com.khaata.app.data.db.RawCaptureDao
import com.khaata.app.data.db.TransactionDao
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
