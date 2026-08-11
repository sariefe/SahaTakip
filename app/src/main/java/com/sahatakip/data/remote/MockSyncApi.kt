package com.sahatakip.data.remote

import com.sahatakip.data.local.entity.EventLogEntity
import com.sahatakip.data.local.entity.LocationEntity
import com.sahatakip.data.local.entity.OfflineActivityReportEntity
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@JsonClass(generateAdapter = true)
data class SyncPayload(
    val deviceId: String,
    val timestamp: Long,
    val locationHistory: List<LocationEntity>,
    val eventLogs: List<EventLogEntity>,
    val offlineActivityReports: List<OfflineActivityReportEntity> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    val success: Boolean,
    val syncedLocationCount: Int,
    val syncedLogCount: Int,
    val syncedReportCount: Int,
    val syncBatchId: String,
    val serverTimestamp: Long,
    val message: String
)

class MockSyncApi {
    suspend fun syncOfflineData(payload: SyncPayload): SyncResponse {
        delay(1200.milliseconds)
        return SyncResponse(
            success = true,
            syncedLocationCount = payload.locationHistory.size,
            syncedLogCount = payload.eventLogs.size,
            syncedReportCount = payload.offlineActivityReports.size,
            syncBatchId = "BATCH-${UUID.randomUUID().toString().take(8).uppercase()}",
            serverTimestamp = System.currentTimeMillis(),
            message = "Çevrimdışı veriler ve aktivite raporları sunucuya başarıyla senkronize edildi."
        )
    }
}
