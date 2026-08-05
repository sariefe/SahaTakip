package com.example.data.repository

import com.example.data.local.dao.LocationDao
import com.example.data.local.entity.LocationEntity
import com.example.domain.repository.GeofenceRepository
import com.example.domain.repository.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao,
    private val geofenceRepository: GeofenceRepository,
) : LocationRepository {

    override val latestLocation: Flow<LocationEntity?> = locationDao.getLatestLocation()

    override suspend fun recordNewLocation(
        lat: Double,
        lng: Double,
        speed: Float,
        accuracy: Float,
        batteryLevel: Int,
        address: String
    ): Long = withContext(Dispatchers.IO) {
        val locationEntity = LocationEntity(
            latitude = lat,
            longitude = lng,
            speed = speed,
            accuracy = accuracy,
            batteryLevel = batteryLevel,
            address = address,
            timestamp = System.currentTimeMillis(),
            isSynced = false
        )
        val id = locationDao.insertLocation(locationEntity)

        geofenceRepository.checkGeofenceBreach(lat, lng)
        id
    }

    override suspend fun getUnsyncedLocations(): List<LocationEntity> = withContext(Dispatchers.IO) {
        locationDao.getUnsyncedLocations()
    }

    override suspend fun markLocationsAsSynced(ids: List<Long>) = withContext(Dispatchers.IO) {
        locationDao.markAsSynced(ids)
    }

    override fun getLocationsSince(timestamp: Long): Flow<List<LocationEntity>> {
        return locationDao.getLocationsSince(timestamp)
    }
}
