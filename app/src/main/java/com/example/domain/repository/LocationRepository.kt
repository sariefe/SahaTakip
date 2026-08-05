package com.example.domain.repository

import com.example.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    val latestLocation: Flow<LocationEntity?>
    suspend fun recordNewLocation(
        lat: Double,
        lng: Double,
        speed: Float,
        accuracy: Float,
        batteryLevel: Int,
        address: String
    ): Long
    suspend fun getUnsyncedLocations(): List<LocationEntity>
    suspend fun markLocationsAsSynced(ids: List<Long>)
    fun getLocationsSince(timestamp: Long): Flow<List<LocationEntity>>
}
