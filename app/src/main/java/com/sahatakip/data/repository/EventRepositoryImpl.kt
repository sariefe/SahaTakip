package com.sahatakip.data.repository

import android.content.Context
import com.sahatakip.data.local.dao.EventLogDao
import com.sahatakip.data.local.entity.EventLogEntity
import com.sahatakip.domain.repository.EventRepository
import com.sahatakip.util.Constants
import com.sahatakip.util.NotificationService
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

        if (status == Constants.STATUS_WARNING || status == Constants.STATUS_DANGER) {
            notificationService.sendPrivacySafeAlert(context, title)
        }
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

    override suspend fun clearAllLogs() = withContext(Dispatchers.IO) {
        eventLogDao.clearAll()
    }
}
