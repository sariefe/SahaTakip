package com.example.data.remote

import com.example.data.local.entity.EventLogEntity
import com.example.data.local.entity.LocationEntity
import kotlinx.coroutines.delay
import java.util.UUID

data class SyncPayload(
    val deviceId: String,
    val timestamp: Long,
    val locationHistory: List<LocationEntity>,
    val eventLogs: List<EventLogEntity>
)

data class SyncResponse(
    val success: Boolean,
    val syncedLocationCount: Int,
    val syncedLogCount: Int,
    val syncBatchId: String,
    val serverTimestamp: Long,
    val message: String
)

class MockSyncApi {
    suspend fun syncOfflineData(endpointUrl: String, payload: SyncPayload): SyncResponse {
        // Simulate network latency for realistic sync
        delay(1200)
        return SyncResponse(
            success = true,
            syncedLocationCount = payload.locationHistory.size,
            syncedLogCount = payload.eventLogs.size,
            syncBatchId = "BATCH-${UUID.randomUUID().toString().take(8).uppercase()}",
            serverTimestamp = System.currentTimeMillis(),
            message = "Çevrimdışı veriler sunucuya başarıyla senkronize edildi."
        )
    }
}
