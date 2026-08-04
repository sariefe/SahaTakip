package com.example.data.repository

import android.content.Context
import com.example.data.local.PreferencesManager
import com.example.data.local.dao.EventLogDao
import com.example.data.local.dao.GeofenceDao
import com.example.data.local.dao.LeaveRequestDao
import com.example.data.local.dao.LocationDao
import com.example.data.local.dao.OfflineActivityReportDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.EventLogEntity
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.data.local.entity.LeaveRequestEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.remote.MockSyncApi
import com.example.data.remote.SyncPayload
import com.example.util.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SahaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    val locationDao: LocationDao,
    val eventLogDao: EventLogDao,
    val leaveRequestDao: LeaveRequestDao,
    val geofenceDao: GeofenceDao,
    val userDao: UserDao,
    val offlineReportDao: OfflineActivityReportDao,
    val preferencesManager: PreferencesManager,
    private val mockSyncApi: MockSyncApi
) {

    val latestLocation: Flow<LocationEntity?> = locationDao.getLatestLocation()
    val allEventLogs: Flow<List<EventLogEntity>> = eventLogDao.getAllEventLogs()
    val allLeaveRequests: Flow<List<LeaveRequestEntity>> = leaveRequestDao.getAllLeaveRequests()
    val allGeofences: Flow<List<GeofenceZoneEntity>> = geofenceDao.getAllGeofences()
    val userProfile: Flow<UserProfileEntity?> = userDao.getUserProfile()

    suspend fun initializeAndSyncDefaultData() = withContext(Dispatchers.IO) {
        val currentUser = userDao.getUserProfile().firstOrNull()
        if (currentUser == null) {
            userDao.insertOrUpdateUser(
                UserProfileEntity(
                    id = 1,
                    firstName = "Örnek",
                    lastName = "Personel",
                    fullName = "Örnek Personel",
                    position = "Saha Teknisyeni",
                    department = "SAHA",
                    staffId = "ID-2026-DEMO",
                    roleTitle = "Saha Personeli (Demo)",
                    activationCode = PreferencesManager.DEFAULT_ACTIVATION_CODE,
                    isActivated = false,
                    isBiometricEnabled = true,
                    isCheckedIn = false
                )
            )
        }
    }

    suspend fun recordNewLocation(
        lat: Double,
        lng: Double,
        speed: Float = 0f,
        accuracy: Float = 5f,
        batteryLevel: Int = 85,
        address: String = "Canlı Saha Konumu"
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

        checkGeofenceBreach(lat, lng)
        id
    }

    private var lastGeofenceAlertTimestamp: Long = 0L

    private suspend fun checkGeofenceBreach(lat: Double, lng: Double) {
        val activeGeofences = geofenceDao.getActiveGeofences()
        if (activeGeofences.isEmpty()) return
        
        val now = System.currentTimeMillis()

        val isInsideAny = activeGeofences.any { zone ->
            calculateDistanceInMeters(lat, lng, zone.centerLat, zone.centerLng) <= zone.radiusMeters
        }

        if (!isInsideAny) {
            if (now - lastGeofenceAlertTimestamp > 120_000L) {
                lastGeofenceAlertTimestamp = now

                val log = EventLogEntity(
                    type = "GEOFENCE_VIOLATION",
                    title = "Bölge İhlal Kaydı",
                    detail = "Güvenli bölge dışına çıkıldı (Demo Tespiti).",
                    isSensitive = true,
                    status = "UYARI",
                    timestamp = now,
                    isSynced = false
                )
                eventLogDao.insertEventLog(log)
                NotificationHelper.sendPrivacySafeAlert(context, "Güvenlik & Bölge İhlali Uyarısı")
            }
        }
    }

    suspend fun addEventLog(type: String, title: String, detail: String, status: String = "UYARI", isSensitive: Boolean = true) = withContext(Dispatchers.IO) {
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
        NotificationHelper.sendPrivacySafeAlert(context, title)
    }

    suspend fun deactivateUser() = withContext(Dispatchers.IO) {
        userDao.deactivateUser()
        addEventLog(
            type = "APP_DEACTIVATED",
            title = "Cihaz Devre Dışı",
            detail = "Personel uygulama oturumunu tamamen sonlandırdı ve cihazı deaktive etti.",
            status = "UYARI"
        )
    }

    suspend fun deleteLeaveRequest(id: Long) = withContext(Dispatchers.IO) {
        leaveRequestDao.deleteById(id)
    }

    suspend fun deleteGeofence(id: Long) = withContext(Dispatchers.IO) {
        geofenceDao.deleteById(id)
    }

    suspend fun performOfflineSync(): Boolean = withContext(Dispatchers.IO) {
        val unsyncedLocs = locationDao.getUnsyncedLocations()
        val unsyncedLogs = eventLogDao.getUnsyncedLogs()
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

                if (locIds.isNotEmpty()) locationDao.markAsSynced(locIds)
                if (logIds.isNotEmpty()) eventLogDao.markAsSynced(logIds)
                if (reportIds.isNotEmpty()) offlineReportDao.markAsSynced(reportIds)


                eventLogDao.insertEventLog(
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

    fun calculateDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return com.example.util.LocationUtils.calculateDistanceInMeters(lat1, lon1, lat2, lon2)
    }
}
