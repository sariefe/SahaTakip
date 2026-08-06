package com.example.domain.repository

import com.example.data.local.entity.EventLogEntity
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    val allEventLogs: Flow<List<EventLogEntity>>
    suspend fun addEventLog(
        type: String,
        title: String,
        detail: String,
        status: String = "UYARI",
        isSensitive: Boolean = true
    )
    suspend fun getUnsyncedLogs(): List<EventLogEntity>
    suspend fun markLogsAsSynced(ids: List<Long>)
    suspend fun insertEventLog(log: EventLogEntity): Long
    suspend fun updateNote(id: Long, note: String)
    suspend fun clearAllLogs()
}
