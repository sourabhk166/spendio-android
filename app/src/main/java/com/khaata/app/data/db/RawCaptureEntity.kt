package com.khaata.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Debug-only log of raw notifications from financial sources — including ones we
 * failed to parse. This is the raw material for growing the parser corpus in Phase 1
 * (roadmap step 2). Surfaced in the in-app debug screen; never leaves the device.
 */
@Entity(tableName = "raw_captures", indices = [Index("capturedAt")])
data class RawCaptureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val title: String?,
    val text: String?,
    val capturedAt: Long,
    val parsed: Boolean,
    val parseTier: String?,
)
