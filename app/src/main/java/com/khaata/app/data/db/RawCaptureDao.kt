package com.khaata.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RawCaptureDao {

    @Insert
    suspend fun insert(capture: RawCaptureEntity)

    @Query("SELECT * FROM raw_captures ORDER BY capturedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RawCaptureEntity>>

    @Query("DELETE FROM raw_captures")
    suspend fun wipe()
}
