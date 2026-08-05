package com.example.data.repository

import android.content.Context
import com.example.data.local.dao.EventLogDao
import com.example.data.local.entity.EventLogEntity
import com.example.domain.repository.EventRepository
import com.example.util.NotificationService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventLogDao: EventLogDao,
    private val notificationService: NotificationService,
) : EventRepository {

    override val allEventLogs: Flow<List<EventLogEntity>> = eventLogDao.getAllEventLogs()

    override suspend fun addEventLog(
        type: String,
        title: String,
        detail: String,
        status: String,
        isSensitive: Boolean
    ) = withContext(Dispatchers.IO) {
        val log = EventLogEntity(
            type = type,
            title = title,
            detail = detail,
            isSensitive = isSensitive,
            status = status,
            timestamp = System.currentTimeMillis(),
            isSynced = false
        )
        eventLogDao.insertEventLog(log)
        notificationService.sendPrivacySafeAlert(context, title)
    }

    override suspend fun getUnsyncedLogs(): List<EventLogEntity> = withContext(Dispatchers.IO) {
        eventLogDao.getUnsyncedLogs()
    }

    override suspend fun markLogsAsSynced(ids: List<Long>) = withContext(Dispatchers.IO) {
        eventLogDao.markAsSynced(ids)
    }

    override suspend fun insertEventLog(log: EventLogEntity) = withContext(Dispatchers.IO) {
        eventLogDao.insertEventLog(log)
    }

    override suspend fun updateNote(id: Long, note: String) = withContext(Dispatchers.IO) {
        eventLogDao.updateNote(id, note)
    }
}
