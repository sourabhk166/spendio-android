package com.spendio.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** Remembered merchant -> category mapping (the categorization memory). */
@Entity(tableName = "merchant_rules")
data class MerchantRuleEntity(
    @PrimaryKey val merchantKey: String,
    val categoryId: String,
    val userConfirmed: Boolean,
)

@Dao
interface MerchantRuleDao {

    /** User-confirmed rule wins — overwrite whatever was there. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: MerchantRuleEntity)

    /** AI prediction — only fill if there's no rule yet, never clobber a confirmed one. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(rule: MerchantRuleEntity)

    @Query("SELECT categoryId FROM merchant_rules WHERE merchantKey = :key")
    suspend fun categoryFor(key: String): String?

    @Query("DELETE FROM merchant_rules")
    suspend fun wipe()
}
