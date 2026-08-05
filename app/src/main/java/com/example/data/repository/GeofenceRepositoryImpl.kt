package com.example.data.repository

import android.content.Context
import com.example.data.local.PreferencesManager
import com.example.data.local.dao.GeofenceDao
import com.example.data.local.entity.EventLogEntity
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.domain.repository.EventRepository
import com.example.domain.repository.GeofenceRepository
import com.example.util.LocationUtils
import com.example.util.NotificationService
import com.example.util.trGlobal
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofenceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geofenceDao: GeofenceDao,
    private val eventRepository: EventRepository,
    private val notificationService: NotificationService,
) : GeofenceRepository {

    override val allGeofences: Flow<List<GeofenceZoneEntity>> = geofenceDao.getAllGeofences()

    private var lastGeofenceAlertTimestamp: Long = 0L

    override suspend fun deleteGeofence(id: Long) = withContext(Dispatchers.IO) {
        geofenceDao.deleteById(id)
    }

    override suspend fun checkGeofenceBreach(lat: Double, lng: Double): GeofenceZoneEntity? = withContext(Dispatchers.IO) {
        val activeGeofences = geofenceDao.getActiveGeofences()
        if (activeGeofences.isEmpty()) return@withContext null
        
        val now = System.currentTimeMillis()

        val containingZone = activeGeofences.find { zone ->
            LocationUtils.calculateDistanceInMeters(lat, lng, zone.centerLat, zone.centerLng) <= zone.radiusMeters
        }

        if (containingZone == null) {
            if ((now - lastGeofenceAlertTimestamp) > 120_000L) {
                lastGeofenceAlertTimestamp = now

                val lang = PreferencesManager(context).language.value
                val log = EventLogEntity(
                    type = "GEOFENCE_VIOLATION",
                    title = trGlobal("Bölge İhlal Kaydı", "Geofence Violation Log", lang),
                    detail = trGlobal("Güvenli bölgelerin dışına çıkıldı (Otomatik Tespit).", "Exited safe zones (Automatic Detection).", lang),
                    isSensitive = true,
                    status = "UYARI",
                    timestamp = now,
                    isSynced = false
                )
                eventRepository.insertEventLog(log)
                notificationService.sendPrivacySafeAlert(context, trGlobal("Güvenlik & Bölge İhlali Uyarısı", "Security & Geofence Alert", lang))
            }
        }
        return@withContext containingZone
    }

    override suspend fun getActiveGeofences(): List<GeofenceZoneEntity> = withContext(Dispatchers.IO) {
        geofenceDao.getActiveGeofences()
    }

    override suspend fun insertGeofence(geofence: GeofenceZoneEntity) = withContext(Dispatchers.IO) {
        geofenceDao.insertGeofence(geofence)
    }

    override suspend fun setGeofenceActive(id: Long, isActive: Boolean) = withContext(Dispatchers.IO) {
        geofenceDao.setGeofenceActive(id, isActive)
    }

    override fun calculateDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return LocationUtils.calculateDistanceInMeters(lat1, lon1, lat2, lon2)
    }

    override suspend fun getAllGeofencesOnce(): List<GeofenceZoneEntity> = withContext(Dispatchers.IO) {
        geofenceDao.getAllGeofences().firstOrNull() ?: emptyList()
    }
}
