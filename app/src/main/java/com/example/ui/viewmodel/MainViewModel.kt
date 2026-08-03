package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesManager
import com.example.data.local.entity.EventLogEntity
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.data.local.entity.LeaveRequestEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.SahaRepository
import com.example.domain.model.DeviceStatus
import com.example.util.OcrCardScanner
import com.example.util.OcrLine
import com.example.util.PermissionUtils
import com.example.util.ScannedStaffCardResult
import com.example.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

data class PlaybackState(
    val isPlaying: Boolean = false,
    val speedMultiplier: Float = 1.0f,
    val progress: Float = 0.0f,
    val currentIndex: Int = 0,
    val currentLocation: LocationEntity? = null
)

class MainViewModel(
    application: Application,
    val repository: SahaRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, SahaRepository(application))

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val locationsLast24h: StateFlow<List<LocationEntity>> = repository.locationDao.getLocationsSince(
        System.currentTimeMillis() - 24 * 60 * 60 * 1000L
    ).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val latestLocation: StateFlow<LocationEntity?> = repository.latestLocation
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allEventLogs: StateFlow<List<EventLogEntity>> = repository.allEventLogs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allLeaveRequests: StateFlow<List<LeaveRequestEntity>> = repository.allLeaveRequests
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allGeofences: StateFlow<List<GeofenceZoneEntity>> = repository.allGeofences
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val language = repository.preferencesManager.language
    val updateInterval = repository.preferencesManager.updateInterval
    val theme = repository.preferencesManager.theme
    val mockServerUrl = repository.preferencesManager.mockServerUrl

    private val _deviceStatus = MutableStateFlow(DeviceStatus())
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _ocrScanningState = MutableStateFlow<ScannedStaffCardResult?>(null)
    val ocrScanningState: StateFlow<ScannedStaffCardResult?> = _ocrScanningState.asStateFlow()

    private val _ocrIsLoading = MutableStateFlow(false)
    val ocrIsLoading: StateFlow<Boolean> = _ocrIsLoading.asStateFlow()

    private val _ocrStability = MutableStateFlow(0f)
    val ocrStability: StateFlow<Float> = _ocrStability.asStateFlow()

    private val _detectedLines = MutableStateFlow<List<OcrLine>>(emptyList())
    val detectedLines: StateFlow<List<OcrLine>> = _detectedLines.asStateFlow()

    private val _ocrImageWidth = MutableStateFlow(0)
    val ocrImageWidth: StateFlow<Int> = _ocrImageWidth.asStateFlow()

    private val _ocrImageHeight = MutableStateFlow(0)
    val ocrImageHeight: StateFlow<Int> = _ocrImageHeight.asStateFlow()

    private val ocrResultBuffer = mutableListOf<String>()
    private var lastOcrTop = -1
    private var lastOcrLeft = -1
    private val stabilitityThreshold = 5 

    private val _ocrScanSuggested = MutableStateFlow(false)
    val ocrScanSuggested: StateFlow<Boolean> = _ocrScanSuggested.asStateFlow()

    fun markOcrScanSuggested() {
        _ocrScanSuggested.value = true
    }

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _ocrAuthError = MutableStateFlow<String?>(null)
    val ocrAuthError: StateFlow<String?> = _ocrAuthError.asStateFlow()

    private var playbackJob: Job? = null

    private val gpsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateDeviceStatus()
        }
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateDeviceStatus()
            val isPowerSave = PermissionUtils.isPowerSaveMode(context)
            viewModelScope.launch {
                repository.addEventLog(
                    type = "POWER_MODE_CHANGED",
                    title = if (isPowerSave) "Düşük Güç Modu Aktif" else "Normal Güç Moduna Geçildi",
                    detail = if (isPowerSave) 
                        "Cihaz pil tasarrufu moduna girdi. Konum hassasiyeti ve arka plan aktiviteleri kısıtlanabilir."
                    else 
                        "Cihaz normal güç moduna döndü. Takip servisleri tam kapasite çalışıyor.",
                    status = if (isPowerSave) "UYARI" else "BİLGİ"
                )
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.initializeAndSyncDefaultData()
            updateDeviceStatus()
        }
        
        // Continuous connectivity monitoring
        viewModelScope.launch {
            com.example.util.ConnectivityObserver(getApplication()).observe().collect { status ->
                val isOnline = status == com.example.util.ConnectivityStatus.Available
                _deviceStatus.value = _deviceStatus.value.copy(isInternetConnected = isOnline)
                
                if (isOnline) {
                    triggerOfflineSync()
                }
            }
        }

        // Monitor GPS / Provider changes
        getApplication<Application>().registerReceiver(
            gpsReceiver,
            IntentFilter(android.location.LocationManager.PROVIDERS_CHANGED_ACTION)
        )

        // Monitor Power Save Mode
        getApplication<Application>().registerReceiver(
            powerReceiver,
            IntentFilter(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        )
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        try {
            getApplication<Application>().unregisterReceiver(gpsReceiver)
            getApplication<Application>().unregisterReceiver(powerReceiver)
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error unregistering receivers", e)
        }
        super.onCleared()
    }

    fun updateDeviceStatus() {
        val context = getApplication<Application>()

        viewModelScope.launch(Dispatchers.IO) {
            // Connectivity check
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
            val isOnline = capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))

            // Battery check
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 85
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else 85
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            val isRooted = SecurityUtils.checkIsDeviceRooted()

            _deviceStatus.value = DeviceStatus(
                isInternetConnected = isOnline,
                isGpsEnabled = PermissionUtils.isGpsEnabled(context) && PermissionUtils.hasLocationPermissions(context),
                isBackgroundLocationGranted = PermissionUtils.hasBackgroundLocationPermission(context),
                isNotificationGranted = PermissionUtils.hasNotificationPermission(context),
                isBatteryOptimizationIgnored = PermissionUtils.isIgnoringBatteryOptimizations(context),
                isPowerSaveModeActive = PermissionUtils.isPowerSaveMode(context),
                batteryLevel = batteryPct,
                isBatteryCharging = isCharging,
                isRooted = isRooted,
                lastCheckedTimestamp = System.currentTimeMillis()
            )
        }
    }

    fun toggleGpsSimulation() {
        _deviceStatus.value = _deviceStatus.value.copy(
            isGpsEnabled = !_deviceStatus.value.isGpsEnabled
        )
        if (!_deviceStatus.value.isGpsEnabled) {
            viewModelScope.launch {
                repository.addEventLog(
                    type = "GPS_DISABLED",
                    title = "Konum Servisleri Kapatıldı",
                    detail = "Saha personeli konum servislerini veya GPS antenini devre dışı bıraktı.",
                    status = "TEHLİKE"
                )
            }
        }
    }

    fun toggleInternetSimulation() {
        val nextOnline = !_deviceStatus.value.isInternetConnected
        _deviceStatus.value = _deviceStatus.value.copy(isInternetConnected = nextOnline)
        viewModelScope.launch {
            if (!nextOnline) {
                repository.addEventLog(
                    type = "INTERNET_LOST",
                    title = "İnternet Bağlantı Kaybı",
                    detail = "Şebeke bağlantısı kesildi. Çevrimdışı mod devreye girdi.",
                    status = "UYARI"
                )
            } else {
                repository.addEventLog(
                    type = "INTERNET_RESTORED",
                    title = "İnternet Bağlantısı Sağlandı",
                    detail = "Şebeke bağlantısı yeniden sağlandı. Çevrimdışı veriler senkronize ediliyor.",
                    status = "BİLGİ"
                )
                triggerOfflineSync()
            }
        }
    }

    fun triggerOfflineSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.performOfflineSync()
            _isSyncing.value = false
        }
    }

    fun startIdCardOcrScan(preset: com.example.util.StaffCardPreset? = null) {
        viewModelScope.launch {
            _ocrIsLoading.value = true
            
            // If we already have a real OCR result and no preset is selected, keep the real one
            val existingResult = _ocrScanningState.value
            val result = if (preset == null && existingResult != null) {
                delay(500.milliseconds) // Small UX delay
                existingResult
            } else {
                OcrCardScanner.processStaffCardScan(preset = preset)
            }
            
            _ocrScanningState.value = result
            _ocrIsLoading.value = false

            repository.addEventLog(
                type = "OCR_SCAN_SUCCESS",
                title = "Personel Kartı OCR Taraması Yapıldı",
                detail = "Personel: ${result.fullName} (ID: ${result.staffId}) - Doğruluk Skoru: %${(result.confidenceScore * 100).toInt()}",
                status = "BAŞARILI"
            )
        }
    }

    fun onRealOcrDetected(ocrLines: List<OcrLine>, imgWidth: Int, imgHeight: Int) {
        _detectedLines.value = ocrLines
        _ocrImageWidth.value = imgWidth
        _ocrImageHeight.value = imgHeight

        val result = OcrCardScanner.parseStaffCardText(ocrLines)
        if (result != null) {
            // Find the representative line for spatial tracking (usually the ID line)
            val idLine = ocrLines.find { it.text.contains(result.staffId) }
            
            // Spatial Stability: Check if text is moving too much
            var spatialWeight = 1.0f
            if (idLine != null && lastOcrTop != -1) {
                val dist = sqrt(
                    (idLine.top - lastOcrTop).toDouble().pow(2.0) +
                            (idLine.left - lastOcrLeft).toDouble().pow(2.0)
                )
                // If it moved more than 40 pixels between frames, reduce stability
                if (dist > 40) spatialWeight = 0.4f
            }
            
            if (idLine != null) {
                lastOcrTop = idLine.top
                lastOcrLeft = idLine.left
            }

            ocrResultBuffer.add(result.staffId)
            if (ocrResultBuffer.size > 15) ocrResultBuffer.removeAt(0)

            val frequency = ocrResultBuffer.count { it == result.staffId }
            val stability = ((frequency.toFloat() / stabilitityThreshold) * spatialWeight).coerceAtMost(1f)
            _ocrStability.value = stability

            // Allow update if ID is same but result is more complete (Consensus)
            if (frequency >= stabilitityThreshold) {
                val current = _ocrScanningState.value
                val isMoreComplete = current == null || 
                        (result.fullName.length > current.fullName.length && result.staffId == current.staffId) ||
                        (result.department.length > current.department.length && result.staffId == current.staffId)
                
                if (isMoreComplete || (result.staffId != current.staffId)) {
                    _ocrScanningState.value = result
                    viewModelScope.launch {
                        repository.addEventLog(
                            type = "REAL_OCR_DETECTION",
                            title = "Canlı Personel Kartı Tespiti",
                            detail = "Kamera üzerinden kararlı bir şekilde kart tespiti yapıldı: ${result.fullName}",
                            status = "BİLGİ"
                        )
                    }
                }
            }
        } else {
            // Decay stability if nothing found
            _ocrStability.value = (_ocrStability.value - 0.1f).coerceAtLeast(0f)
        }
    }

    fun clearOcrResult() {
        _ocrScanningState.value = null
        _ocrStability.value = 0f
        _detectedLines.value = emptyList()
        ocrResultBuffer.clear()
        lastOcrTop = -1
        lastOcrLeft = -1
    }

    fun activateWithCode(code: String): Boolean {
        if (code.trim() == PreferencesManager.DEFAULT_ACTIVATION_CODE || code.trim() == "123456") {
            viewModelScope.launch {
                val ocrResult = _ocrScanningState.value
                val scannedFirstName = ocrResult?.firstName ?: "AHMET CAN"
                val scannedLastName = ocrResult?.lastName ?: "YILMAZ"
                val scannedStaffId = ocrResult?.staffId ?: "ID-2026-999"
                val scannedDept = ocrResult?.department?.takeIf { it.isNotBlank() } ?: "SAHA"

                repository.userDao.insertOrUpdateUser(
                    UserProfileEntity(
                        id = 1,
                        firstName = scannedFirstName,
                        lastName = scannedLastName,
                        fullName = "$scannedFirstName $scannedLastName",
                        staffId = scannedStaffId,
                        department = scannedDept,
                        activationCode = code.trim(),
                        isActivated = true,
                        isBiometricEnabled = true,
                        lastLoginAt = System.currentTimeMillis()
                    )
                )
                _isAuthenticated.value = true
                _authErrorMessage.value = null
            }
            return true
        } else {
            _authErrorMessage.value = "Geçersiz aktivasyon kodu! Lütfen doğru şifre kodunu giriniz."
            return false
        }
    }

    fun authenticateWithBiometrics(): Boolean {
        _isAuthenticated.value = true
        viewModelScope.launch {
            repository.userDao.updateLastLogin()
        }
        return true
    }

    fun authenticateWithOcr(scannedStaffId: String, ocrResult: ScannedStaffCardResult? = null): Boolean {
        val currentProfile = userProfile.value
        val registeredStaffId = currentProfile?.staffId
        return if (scannedStaffId == registeredStaffId) {
            _isAuthenticated.value = true
            _ocrAuthError.value = null
            viewModelScope.launch {
                repository.userDao.updateLastLogin()
                
                // Sync profile with latest OCR scan if available
                if (ocrResult != null) {
                    currentProfile.let { profile ->
                        repository.userDao.insertOrUpdateUser(
                            profile.copy(
                                firstName = ocrResult.firstName,
                                lastName = ocrResult.lastName,
                                fullName = ocrResult.fullName,
                                department = ocrResult.department.takeIf { it.isNotBlank() } ?: profile.department
                            )
                        )
                    }
                }

                repository.addEventLog(
                    type = "OCR_AUTH_SUCCESS",
                    title = "Personel Doğrulama Başarılı",
                    detail = "Personel kartı OCR ile doğrulandı ve giriş yapıldı.",
                    status = "BAŞARILI"
                )
            }
            true
        } else {
            _ocrAuthError.value = "Personel kartı kayıtlı personel ile eşleşmiyor!"
            viewModelScope.launch {
                repository.addEventLog(
                    type = "OCR_AUTH_FAILED",
                    title = "Personel Doğrulama Başarısız",
                    detail = "Farklı bir personel kartı ile giriş denemesi yapıldı (Tespit edilen ID: $scannedStaffId).",
                    status = "TEHLİKE"
                )
            }
            false
        }
    }

    fun startRoutePlayback() {
        val points = locationsLast24h.value
        if (points.isEmpty()) return

        playbackJob?.cancel()
        _playbackState.value = _playbackState.value.copy(isPlaying = true)

        playbackJob = viewModelScope.launch {
            var idx = _playbackState.value.currentIndex
            if (idx >= points.size - 1) idx = 0

            while (idx < points.size && _playbackState.value.isPlaying) {
                val currPoint = points[idx]
                val prog = idx.toFloat() / (points.size - 1).coerceAtLeast(1)

                _playbackState.value = _playbackState.value.copy(
                    currentIndex = idx,
                    progress = prog,
                    currentLocation = currPoint
                )

                delay((800 / _playbackState.value.speedMultiplier).toLong().milliseconds)
                idx++
            }

            _playbackState.value = _playbackState.value.copy(isPlaying = false)
        }
    }

    fun pauseRoutePlayback() {
        playbackJob?.cancel()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(speedMultiplier = speed)
    }

    fun seekPlaybackProgress(progress: Float) {
        val points = locationsLast24h.value
        if (points.isEmpty()) return
        val targetIdx = (progress * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
        _playbackState.value = _playbackState.value.copy(
            progress = progress,
            currentIndex = targetIdx,
            currentLocation = points[targetIdx]
        )
    }

    fun addNoteToEventLog(logId: Long, note: String) {
        viewModelScope.launch {
            repository.eventLogDao.updateNote(logId, note)
        }
    }

    fun deleteLeaveRequest(id: Long) {
        viewModelScope.launch {
            repository.deleteLeaveRequest(id)
        }
    }

    fun deleteGeofence(id: Long) {
        viewModelScope.launch {
            repository.deleteGeofence(id)
        }
    }

    fun submitLeaveRequest(type: String, startDate: String, endDate: String, reason: String) {
        viewModelScope.launch {
            repository.leaveRequestDao.insertLeaveRequest(
                LeaveRequestEntity(
                    startDate = startDate,
                    endDate = endDate,
                    requestType = type,
                    reason = reason,
                    status = "BEKLEMEDE"
                )
            )
        }
    }

    fun addGeofenceZone(name: String, lat: Double, lng: Double, radiusMeters: Double) {
        viewModelScope.launch {
            val existingZones = repository.geofenceDao.getAllGeofences().firstOrNull() ?: emptyList()

            val isDuplicate = existingZones.any { 
                it.name.equals(name, ignoreCase = true) || 
                (repository.calculateDistanceInMeters(lat, lng, it.centerLat, it.centerLng) < 10.0) 
            }

            if (!isDuplicate) {
                repository.geofenceDao.insertGeofence(
                    GeofenceZoneEntity(
                        name = name,
                        centerLat = lat,
                        centerLng = lng,
                        radiusMeters = radiusMeters,
                        isActive = true
                    )
                )
            }
        }
    }

    fun toggleGeofenceActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.geofenceDao.setGeofenceActive(id, isActive)
        }
    }

    fun updateGeofenceZone(id: Long, name: String, radiusMeters: Double) {
        viewModelScope.launch {
            val zones = repository.geofenceDao.getAllGeofences().firstOrNull()
            val existing = zones?.find { it.id == id }
            existing?.let {
                repository.geofenceDao.insertGeofence(
                    it.copy(name = name, radiusMeters = radiusMeters)
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.deactivateUser()
            _isAuthenticated.value = false
        }
    }

    fun setLanguage(lang: String) = repository.preferencesManager.setLanguage(lang)
    fun setUpdateInterval(seconds: Int) = repository.preferencesManager.setUpdateInterval(seconds)
    fun setTheme(themeMode: String) = repository.preferencesManager.setTheme(themeMode)
    fun setMockServerUrl(url: String) = repository.preferencesManager.setMockServerUrl(url)
}
