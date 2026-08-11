package com.sahatakip.data.repository

import android.content.Context
import com.sahatakip.data.local.PreferencesManager
import com.sahatakip.data.local.dao.GeofenceDao
import com.sahatakip.data.local.entity.EventLogEntity
import com.sahatakip.data.local.entity.GeofenceZoneEntity
import com.sahatakip.domain.repository.EventRepository
import com.sahatakip.domain.repository.GeofenceRepository
import com.sahatakip.util.Constants
import com.sahatakip.util.LocationUtils
import com.sahatakip.util.NotificationService
import com.sahatakip.util.trGlobal
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
    private val preferencesManager: PreferencesManager,
) : GeofenceRepository {

    override val allGeofences: Flow<List<GeofenceZoneEntity>> = geofenceDao.getAllGeofences()

    private var lastKnownZoneId: Long? = null
    private var isInitialized: Boolean = false
    private var lastExitTime: Long = 0L
    private val JITTER_THRESHOLD_MS = 15_000L

    override suspend fun deleteGeofence(id: Long) = withContext(Dispatchers.IO) {
        geofenceDao.deleteById(id)
    }

    override suspend fun checkGeofenceBreach(lat: Double, lng: Double): GeofenceZoneEntity? = withContext(Dispatchers.IO) {
        val activeGeofences = geofenceDao.getActiveGeofences()
        if (activeGeofences.isEmpty()) {
            isInitialized = true
            return@withContext null
        }
        
        val now = System.currentTimeMillis()
        val currentZone = activeGeofences.find { zone ->
            LocationUtils.calculateDistanceInMeters(lat, lng, zone.centerLat, zone.centerLng) <= zone.radiusMeters
        }

        val lang = preferencesManager.language.value

        if (isInitialized) {
            if (lastKnownZoneId != null && (currentZone == null || currentZone.id != lastKnownZoneId)) {
                if ((now - lastExitTime) > JITTER_THRESHOLD_MS) {
                    eventRepository.insertEventLog(
                        EventLogEntity(
                            type = "GEOFENCE_EXIT",
                            title = trGlobal("Bölgeden Ayrıldı", "Exited Zone", lang),
                            detail = trGlobal("Güvenli alandan çıkış yapıldı.", "Exited safe area.", lang),
                            isSensitive = true,
                            status = Constants.STATUS_WARNING,
                            timestamp = now,
                            isSynced = false
                        )
                    )
                    lastExitTime = now
                    if (currentZone == null) {
                        notificationService.sendPrivacySafeAlert(context, trGlobal("Bölge Dışı Uyarısı", "Out of Zone Alert", lang))
                    }
                }
            }

            if (currentZone != null && currentZone.id != lastKnownZoneId) {
                eventRepository.insertEventLog(
                    EventLogEntity(
                        type = "GEOFENCE_ENTER",
                        title = trGlobal("Bölgeye Giriş", "Entered Zone", lang),
                        detail = trGlobal("${currentZone.name} bölgesine girildi.", "Entered zone ${currentZone.name}.", lang),
                        isSensitive = false,
                        status = Constants.STATUS_SUCCESS,
                        timestamp = now,
                        isSynced = false
                    )
                )
            }
        }

        lastKnownZoneId = currentZone?.id
        isInitialized = true
        return@withContext currentZone
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
