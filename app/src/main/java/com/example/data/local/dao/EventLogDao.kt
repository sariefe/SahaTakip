package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.EventLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventLogDao {
    @Query("SELECT * FROM event_logs ORDER BY timestamp DESC")
    fun getAllEventLogs(): Flow<List<EventLogEntity>>

    @Query("SELECT * FROM event_logs WHERE isSynced = 0")
    suspend fun getUnsyncedLogs(): List<EventLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventLog(eventLog: EventLogEntity): Long

    @Query("UPDATE event_logs SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String)

    @Query("UPDATE event_logs SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("DELETE FROM event_logs")
    suspend fun clearAll()
}
