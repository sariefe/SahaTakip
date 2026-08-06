package com.example.data.repository

import com.example.data.local.PreferencesManager
import com.example.data.local.dao.OfflineActivityReportDao
import com.example.data.local.entity.EventLogEntity
import com.example.data.remote.MockSyncApi
import com.example.data.remote.SyncPayload
import com.example.domain.repository.EventRepository
import com.example.domain.repository.LocationRepository
import com.example.domain.repository.SyncRepository
import com.example.util.trGlobal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val locationRepository: LocationRepository,
    private val eventRepository: EventRepository,
    private val offlineReportDao: OfflineActivityReportDao,
    private val mockSyncApi: MockSyncApi,
    private val preferencesManager: PreferencesManager,
) : SyncRepository {

    override suspend fun performOfflineSync(): Boolean = withContext(Dispatchers.IO) {
        val unsyncedLocs = locationRepository.getUnsyncedLocations()
        val unsyncedLogs = eventRepository.getUnsyncedLogs()
        val unsyncedReports = offlineReportDao.getUnsyncedReports()

        if (unsyncedLocs.isEmpty() && unsyncedLogs.isEmpty() && unsyncedReports.isEmpty()) {
            return@withContext true
        }

        try {
            val deviceId = preferencesManager.deviceId.value
            val payload = SyncPayload(
                deviceId = deviceId,
                timestamp = System.currentTimeMillis(),
                locationHistory = unsyncedLocs,
                eventLogs = unsyncedLogs,
                offlineActivityReports = unsyncedReports,
            )
            val response = mockSyncApi.syncOfflineData(payload)
            if (response.success) {
                // ... sync mark as logic ...
                val locIds = unsyncedLocs.map { it.id }
                val logIds = unsyncedLogs.map { it.id }
                val reportIds = unsyncedReports.map { it.id }

                if (locIds.isNotEmpty()) locationRepository.markLocationsAsSynced(locIds)
                if (logIds.isNotEmpty()) eventRepository.markLogsAsSynced(logIds)
                if (reportIds.isNotEmpty()) offlineReportDao.markAsSynced(reportIds)

                val lang = preferencesManager.language.value
                eventRepository.insertEventLog(
                    EventLogEntity(
                        type = "SYNC_SUCCESS",
                        title = trGlobal("Veri Senkronizasyonu", "Data Synchronization", lang),
                        detail = trGlobal(
                            "${locIds.size} konum kaydı, ${logIds.size} olay günlüğü ve ${reportIds.size} çevrimdışı aktivite raporu sunucuya başarıyla iletildi.",
                            "${locIds.size} location records, ${logIds.size} event logs and ${reportIds.size} offline activity reports successfully sent to server.",
                            lang,
                        ),
                        isSensitive = false,
                        status = "BİLGİ",
                        timestamp = System.currentTimeMillis(),
                        isSynced = true
                    )
                )
                return@withContext true
            } else {
                throw Exception(response.message)
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncRepository", "Sync failed: ${e.message}", e)
            throw e
        }
    }
}
