package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
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
import com.example.data.util.SecurityUtils
import com.example.domain.model.DeviceStatus
import com.example.util.OcrCardScanner
import com.example.util.ScannedIdCardResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaybackState(
    val isPlaying: Boolean = false,
    val speedMultiplier: Float = 1.0f,
    val progress: Float = 0.0f,
    val currentIndex: Int = 0,
    val currentLocation: LocationEntity? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SahaRepository(application)

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allLocations: StateFlow<List<LocationEntity>> = repository.allLocations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestLocation: StateFlow<LocationEntity?> = repository.latestLocation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allEventLogs: StateFlow<List<EventLogEntity>> = repository.allEventLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLeaveRequests: StateFlow<List<LeaveRequestEntity>> = repository.allLeaveRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGeofences: StateFlow<List<GeofenceZoneEntity>> = repository.allGeofences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val _ocrScanningState = MutableStateFlow<ScannedIdCardResult?>(null)
    val ocrScanningState: StateFlow<ScannedIdCardResult?> = _ocrScanningState.asStateFlow()

    private val _ocrIsLoading = MutableStateFlow(false)
    val ocrIsLoading: StateFlow<Boolean> = _ocrIsLoading.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private var playbackJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeDefaultDataIfEmpty()
            updateDeviceStatus()
        }
    }

    fun updateDeviceStatus() {
        val context = getApplication<Application>()

        // Connectivity check
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(activeNetwork)
        val isOnline = capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))

        // Battery check
        val batteryIntent = context.registerReceiver(null, IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 85
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else 85
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val isRooted = SecurityUtils.checkIsDeviceRooted(context)

        _deviceStatus.value = DeviceStatus(
            isInternetConnected = isOnline,
            isGpsEnabled = true,
            isBackgroundLocationGranted = true,
            isNotificationGranted = true,
            batteryLevel = batteryPct,
            isBatteryCharging = isCharging,
            isRooted = isRooted,
            lastCheckedTimestamp = System.currentTimeMillis()
        )
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

    fun startIdCardOcrScan(fullNameInput: String = "") {
        viewModelScope.launch {
            _ocrIsLoading.value = true
            val result = OcrCardScanner.processIdCardScan(fallbackNameInput = fullNameInput)
            _ocrScanningState.value = result
            _ocrIsLoading.value = false
        }
    }

    fun activateWithCode(code: String): Boolean {
        if (code.trim() == PreferencesManager.DEFAULT_ACTIVATION_CODE || code.trim() == "123456") {
            viewModelScope.launch {
                val scannedName = _ocrScanningState.value?.fullName ?: "AHMET CAN YILMAZ"
                val scannedTc = _ocrScanningState.value?.tcNo ?: "10293847562"

                repository.userDao.insertOrUpdateUser(
                    UserProfileEntity(
                        id = 1,
                        fullName = scannedName,
                        tcNo = scannedTc,
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
            _authErrorMessage.value = "Geçersiz aktivasyon kodu! Lütfen 'SAHA2026' veya '123456' kodunu giriniz."
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

    fun startRoutePlayback() {
        val points = allLocations.value.reversed()
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

                delay((800 / _playbackState.value.speedMultiplier).toLong())
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
        val points = allLocations.value.reversed()
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

    fun toggleGeofenceActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.geofenceDao.setGeofenceActive(id, isActive)
        }
    }

    fun setLanguage(lang: String) = repository.preferencesManager.setLanguage(lang)
    fun setUpdateInterval(seconds: Int) = repository.preferencesManager.setUpdateInterval(seconds)
    fun setTheme(themeMode: String) = repository.preferencesManager.setTheme(themeMode)
    fun setMockServerUrl(url: String) = repository.preferencesManager.setMockServerUrl(url)
}
