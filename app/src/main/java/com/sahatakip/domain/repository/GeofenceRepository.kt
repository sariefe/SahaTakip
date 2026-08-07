package com.sahatakip.domain.repository

import com.sahatakip.data.local.entity.GeofenceZoneEntity
import kotlinx.coroutines.flow.Flow

interface GeofenceRepository {
    val allGeofences: Flow<List<GeofenceZoneEntity>>
    suspend fun deleteGeofence(id: Long)
    suspend fun checkGeofenceBreach(lat: Double, lng: Double): GeofenceZoneEntity?
    suspend fun getActiveGeofences(): List<GeofenceZoneEntity>
    suspend fun insertGeofence(geofence: GeofenceZoneEntity): Long
    suspend fun setGeofenceActive(id: Long, isActive: Boolean)
    fun calculateDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double
    suspend fun getAllGeofencesOnce(): List<GeofenceZoneEntity>
}
