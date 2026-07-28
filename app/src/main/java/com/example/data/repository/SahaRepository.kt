package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.local.entity.EventLogEntity
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.data.local.entity.LeaveRequestEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.local.entity.OfflineActivityReportEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.remote.MockSyncApi
import com.example.data.remote.SyncPayload
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class SahaRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    val locationDao = db.locationDao()
    val eventLogDao = db.eventLogDao()
    val leaveRequestDao = db.leaveRequestDao()
    val geofenceDao = db.geofenceDao()
    val userDao = db.userDao()
    val offlineReportDao = db.offlineActivityReportDao()

    val preferencesManager = PreferencesManager(context)
    private val mockSyncApi = MockSyncApi()

    val allLocations: Flow<List<LocationEntity>> = locationDao.getAllLocations()
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
                    gender = "Erkek",
                    tcNo = "10000000000",
                    roleTitle = "Saha Personeli (Demo)",
                    activationCode = PreferencesManager.DEFAULT_ACTIVATION_CODE,
                    isActivated = false,
                    isBiometricEnabled = true,
                    isCheckedIn = false
                )
            )
        }


        // Geofence Senkronizasyonu: Liste boş olmasa bile varsayılan bölgeleri kontrol et
        val geofenceList = geofenceDao.getActiveGeofences()
        val existingNames = geofenceList.map { it.name }

        val defaultZones = listOf(
            GeofenceZoneEntity(
                name = "Merkez Bölge (Genel Müdürlük)",
                centerLat = 41.0082,
                centerLng = 28.9784,
                radiusMeters = 800.0,
                isActive = true
            ),
            GeofenceZoneEntity(
                name = "Saha Şantiye A2 Bölgesi",
                centerLat = 41.0150,
                centerLng = 28.9850,
                radiusMeters = 500.0,
                isActive = true
            ),
            GeofenceZoneEntity(
                name = "Sirket Ana Kampüs",
                centerLat = 40.0201,
                centerLng = 29.1111,
                radiusMeters = 600.0,
                isActive = true
            )
        )

        defaultZones.forEach { zone ->
            if (!existingNames.contains(zone.name)) {
                geofenceDao.insertGeofence(zone)
                android.util.Log.d("SahaRepository", "Yeni geofence eklendi: ${zone.name}")
            }
        }

        val locations = locationDao.getUnsyncedLocations()
        if (locations.isEmpty()) {
            seedSampleLocationHistory()
        }
    }

    private suspend fun seedSampleLocationHistory() {
        val now = System.currentTimeMillis()
        val baseLat = 41.0082
        val baseLng = 28.9784
        val samplePoints = listOf(
            Triple(0.0, 0.0, "Genel Müdürlük Binası"),
            Triple(0.0012, 0.0018, "Atatürk Bulvarı Kavşağı"),
            Triple(0.0025, 0.0035, "Lojistik Merkezi"),
            Triple(0.0040, 0.0060, "Saha Kontrol Noktası 1"),
            Triple(0.0065, 0.0085, "Devriye Bölgesi B4"),
            Triple(0.0080, 0.0110, "Bölge İhlal Sınır Yakını"),
            Triple(0.0120, 0.0150, "Güvenli Bölge Dışı Kontrol Noktası"),
            Triple(0.0090, 0.0120, "Dönüş Rotalama Sektörü"),
            Triple(0.0045, 0.0070, "Saha Kontrol Noktası 2"),
            Triple(0.0010, 0.0015, "Merkez Kampüs Girişi")
        )

        samplePoints.forEachIndexed { index, triple ->
            val timestamp = now - (10 - index) * 3600 * 1000L
            locationDao.insertLocation(
                LocationEntity(
                    latitude = baseLat + triple.first,
                    longitude = baseLng + triple.second,
                    speed = (15..45).random().toFloat(),
                    accuracy = (3..8).random().toFloat(),
                    batteryLevel = 100 - index * 4,
                    address = triple.third,
                    timestamp = timestamp,
                    isSynced = index < 4
                )
            )
        }

        // Add sample logs
        eventLogDao.insertEventLog(
            EventLogEntity(
                type = "CUSTOM",
                title = "Sistem Başlatıldı",
                detail = "Saha personeli takip servisi başarıyla aktif edildi.",
                isSensitive = false,
                note = "Kurulum tamamlandı.",
                status = "BİLGİ",
                timestamp = now - 12 * 3600 * 1000L,
                isSynced = true
            )
        )
        eventLogDao.insertEventLog(
            EventLogEntity(
                type = "GEOFENCE_VIOLATION",
                title = "Bölge İhlal Uyarısı",
                detail = "Kullanıcı tanımlı 'Merkez Bölge' güvenli alanının dışına çıktı.",
                isSensitive = true,
                note = "Saha devriye görevi kapsamında geçici çıkış.",
                status = "UYARI",
                timestamp = now - 4 * 3600 * 1000L,
                isSynced = false
            )
        )
        eventLogDao.insertEventLog(
            EventLogEntity(
                type = "INTERNET_LOST",
                title = "Çevrimdışı Mod",
                detail = "Mobil veri/Wi-Fi bağlantısı kesildi. Veriler yerel veri tabanında saklanıyor.",
                isSensitive = false,
                note = "Tünel geçişi esnasında kısıtlı çekim alanı.",
                status = "UYARI",
                timestamp = now - 2 * 3600 * 1000L,
                isSynced = false
            )
        )

        leaveRequestDao.insertLeaveRequest(
            LeaveRequestEntity(
                startDate = "25.07.2026",
                endDate = "27.07.2026",
                requestType = "Yıllık İzin",
                reason = "Yıllık dinlenme izni kullanımı.",
                status = "ONAYLANDI",
                submittedAt = now - 48 * 3600 * 1000L
            )
        )

        offlineReportDao.insertReport(
            OfflineActivityReportEntity(
                title = "Şantiye Güvenlik Ve Devriye Raporu",
                description = "Saha A2 bölgesinde akşam devriyesi tamamlandı. Çevre çit kontrolü yapıldı, kapı kilitleri doğrulandı.",
                locationAddress = "Saha Şantiye A2 Bölgesi",
                latitude = 41.0150,
                longitude = 28.9850,
                reportType = "SAHA_DEVRIYE",
                timestamp = now - 3 * 3600 * 1000L,
                isSynced = false
            )
        )
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
            val response = mockSyncApi.syncOfflineData(preferencesManager.mockServerUrl.value, payload)
            if (response.success) {
                val locIds = unsyncedLocs.map { it.id }
                val logIds = unsyncedLogs.map { it.id }
                val reportIds = unsyncedReports.map { it.id }

                if (locIds.isNotEmpty()) locationDao.markAsSynced(locIds)
                if (logIds.isNotEmpty()) eventLogDao.markAsSynced(logIds)
                if (reportIds.isNotEmpty()) offlineReportDao.markAsSynced(reportIds)

                // Add log for successful sync
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
        val r = 6371000.0 // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
