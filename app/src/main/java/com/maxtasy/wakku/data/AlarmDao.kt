package com.maxtasy.wakku.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun observeAll(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): Alarm?

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getAllEnabled(): List<Alarm>

    // id = 0 inserts a new row (autoGenerate); a real id replaces that row.
    // Returns the row id, so the caller can schedule the alarm right after saving it.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alarm: Alarm): Long

    @Delete
    suspend fun delete(alarm: Alarm)

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
