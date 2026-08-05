package com.example.data.repository

import com.example.data.local.entity.EventLogEntity
import com.example.data.local.dao.OfflineActivityReportDao
import com.example.data.remote.MockSyncApi
import com.example.data.remote.SyncPayload
import com.example.domain.repository.EventRepository
import com.example.domain.repository.LocationRepository
import com.example.domain.repository.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val locationRepository: LocationRepository,
    private val eventRepository: EventRepository,
    private val offlineReportDao: OfflineActivityReportDao,
    private val mockSyncApi: MockSyncApi,
) : SyncRepository {

    override suspend fun performOfflineSync(): Boolean = withContext(Dispatchers.IO) {
        val unsyncedLocs = locationRepository.getUnsyncedLocations()
        val unsyncedLogs = eventRepository.getUnsyncedLogs()
        val unsyncedReports = offlineReportDao.getUnsyncedReports()

        if (unsyncedLocs.isEmpty() && unsyncedLogs.isEmpty() && unsyncedReports.isEmpty()) {
            return@withContext true
        }

        try {
            val payload = SyncPayload(
                deviceId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                locationHistory = unsyncedLocs,
                eventLogs = unsyncedLogs,
                offlineActivityReports = unsyncedReports
            )
            val response = mockSyncApi.syncOfflineData(payload)
            if (response.success) {
                val locIds = unsyncedLocs.map { it.id }
                val logIds = unsyncedLogs.map { it.id }
                val reportIds = unsyncedReports.map { it.id }

                if (locIds.isNotEmpty()) locationRepository.markLocationsAsSynced(locIds)
                if (logIds.isNotEmpty()) eventRepository.markLogsAsSynced(logIds)
                if (reportIds.isNotEmpty()) offlineReportDao.markAsSynced(reportIds)

                eventRepository.insertEventLog(
                    EventLogEntity(
                        type = "SYNC_SUCCESS",
                        title = "Veri Senkronizasyonu",
                        detail = "${locIds.size} konum kaydı, ${logIds.size} olay günlüğü ve ${reportIds.size} çevrimdışı aktivite raporu sunucuya başarıyla iletildi.",
                        isSensitive = false,
                        status = "BİLGİ",
                        timestamp = System.currentTimeMillis(),
                        isSynced = true
                    )
                )
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    }
}
