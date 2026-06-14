package com.khaata.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * A template the AI tier learned for a given source + message shape (self-learning cache).
 * Tier 1 loads these alongside the bundled seed templates.
 */
@Entity(
    tableName = "parse_templates",
    indices = [Index("sourcePackage"), Index(value = ["sourcePackage", "signature"], unique = true)],
)
data class ParseTemplateEntity(
    @PrimaryKey val id: String,
    val sourcePackage: String,
    val signature: String,
    val regexPattern: String,
    val direction: String,
    val confidence: Float,
    val hitCount: Int,
    val createdAt: Long,
    val lastUsedAt: Long,
)

@Dao
interface ParseTemplateDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(template: ParseTemplateEntity)

    @Query("SELECT COUNT(*) FROM parse_templates WHERE sourcePackage = :pkg AND signature = :signature")
    suspend fun countFor(pkg: String, signature: String): Int

    @Query("SELECT * FROM parse_templates WHERE sourcePackage = :pkg ORDER BY hitCount DESC")
    suspend fun forPackage(pkg: String): List<ParseTemplateEntity>

    @Query("DELETE FROM parse_templates")
    suspend fun wipe()
}
