package com.sahatakip.ui.viewmodel

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.sahatakip.domain.model.DeviceStatus
import com.sahatakip.domain.repository.EventRepository
import com.sahatakip.domain.repository.SyncRepository
import com.sahatakip.util.ConnectionType
import com.sahatakip.util.ConnectivityStatus
import com.sahatakip.util.PermissionUtils
import com.sahatakip.util.SecurityUtils
import com.sahatakip.util.trGlobal
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.sahatakip.data.local.PreferencesManager
import timber.log.Timber
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventRepository: EventRepository,
    private val syncRepository: SyncRepository,
    private val preferencesManager: PreferencesManager,
    private val connectivityObserver: com.sahatakip.util.ConnectivityObserver,
) : androidx.lifecycle.ViewModel() {

    private val _deviceStatus = MutableStateFlow(value = DeviceStatus())
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(value = false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(value = null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    private val _statusAlert = MutableSharedFlow<String>()
    val statusAlert: SharedFlow<String> = _statusAlert.asSharedFlow()

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
                val lang = preferencesManager.language.value
                eventRepository.addEventLog(
                    type = "POWER_MODE_CHANGED",
                    title = if (isPowerSave) 
                        trGlobal("Düşük Güç Modu Aktif", "Low Power Mode Active", lang)
                    else 
                        trGlobal("Normal Güç Moduna Geçildi", "Switched to Normal Power Mode", lang),
                    detail = if (isPowerSave) 
                        trGlobal("Cihaz pil tasarrufu moduna girdi. Konum hassasiyeti ve arka plan aktiviteleri kısıtlanabilir.", "Device entered battery saver mode. Location accuracy and background activities may be restricted.", lang)
                    else 
                        trGlobal("Cihaz normal güç moduna döndü. Takip servisleri tam kapasite çalışıyor.", "Device returned to normal power mode. Tracking services are running at full capacity.", lang),
                    status = if (isPowerSave) "UYARI" else "BİLGİ",
                )
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateDeviceStatus()
        }
    }

    private var cachedIsRooted: Boolean? = null

    init {
        updateDeviceStatus()
        
        viewModelScope.launch {
            connectivityObserver.observe().collect { (status, type) ->
                val wasOnline = _deviceStatus.value.isInternetConnected
                val isOnline = status == ConnectivityStatus.Available
                
                _deviceStatus.value = _deviceStatus.value.copy(
                    isInternetConnected = isOnline,
                    connectionType = type
                )

                if (wasOnline != isOnline) {
                    val lang = preferencesManager.language.value
                    val message = if (isOnline) {
                        val connectionName = when (type) {
                            ConnectionType.Wifi -> "Wi-Fi"
                            ConnectionType.Cellular -> trGlobal("Mobil Veri", "Mobile Data", lang)
                            else -> trGlobal("İnternet", "Internet", lang)
                        }
                        trGlobal(
                            "Bağlantı sağlandı ($connectionName). Veriler senkronize ediliyor.",
                            "Connection established ($connectionName). Data is being synchronized.",
                            lang
                        )
                    } else {
                        trGlobal(
                            "İnternet bağlantısı kesildi. Çevrimdışı mod aktif.",
                            "Internet connection lost. Offline mode active.",
                            lang
                        )
                    }
                    _statusAlert.emit(message)
                    
                    eventRepository.addEventLog(
                        type = if (isOnline) "INTERNET_RESTORED" else "INTERNET_LOST",
                        title = if (isOnline) 
                            trGlobal("Bağlantı Sağlandı", "Connection Restored", lang)
                        else 
                            trGlobal("Bağlantı Kesildi", "Connection Lost", lang),
                        detail = message,
                        status = if (isOnline) "BİLGİ" else "UYARI"
                    )
                }

                if (isOnline) triggerOfflineSync()
            }
        }

        ContextCompat.registerReceiver(
            context,
            gpsReceiver,
            IntentFilter(android.location.LocationManager.PROVIDERS_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        ContextCompat.registerReceiver(
            context,
            powerReceiver,
            IntentFilter(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        ContextCompat.registerReceiver(
            context,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        try {
            context.unregisterReceiver(gpsReceiver)
            context.unregisterReceiver(powerReceiver)
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            Timber.tag("DeviceViewModel").e(e, "Error unregistering receivers")
        }
        super.onCleared()
    }

    fun updateDeviceStatus() {
        viewModelScope.launch {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
            
            val type = when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> ConnectionType.Wifi
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> ConnectionType.Cellular
                else -> ConnectionType.None
            }
            val isOnline = type != ConnectionType.None

            val batteryIntent = ContextCompat.registerReceiver(
                context,
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 85
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val batteryPct = if ((level >= 0) && (scale > 0)) (level * 100 / scale.toFloat()).toInt() else 85
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            val isRooted = cachedIsRooted ?: SecurityUtils.checkIsDeviceRooted().also {
                cachedIsRooted = it
            }

            val oldStatus = _deviceStatus.value
            val newStatus = DeviceStatus(
                isInternetConnected = isOnline,
                connectionType = type,
                isGpsEnabled = PermissionUtils.isGpsEnabled(context) && PermissionUtils.hasLocationPermissions(context),
                isBackgroundLocationGranted = PermissionUtils.hasBackgroundLocationPermission(context),
                isNotificationGranted = PermissionUtils.hasNotificationPermission(context),
                isCameraPermissionGranted = PermissionUtils.hasCameraPermission(context),
                isBatteryOptimizationIgnored = PermissionUtils.isIgnoringBatteryOptimizations(context),
                isPowerSaveModeActive = PermissionUtils.isPowerSaveMode(context),
                batteryLevel = batteryPct,
                isBatteryCharging = isCharging,
                isRooted = isRooted
            )

            if (oldStatus != newStatus) {
                detectCriticalStatusChanges(oldStatus, newStatus)
                _deviceStatus.value = newStatus
            }
        }
    }

    private suspend fun detectCriticalStatusChanges(old: DeviceStatus, new: DeviceStatus) {
        val lang = preferencesManager.language.value
        if (old.isGpsEnabled != new.isGpsEnabled) {
            val msg = if (new.isGpsEnabled) 
                trGlobal("Konum servisleri aktif edildi.", "Location services enabled.", lang)
            else 
                trGlobal("Konum servisleri kapatıldı! Saha takibi durduruldu.", "Location services disabled! Field tracking stopped.", lang)
            
            _statusAlert.emit(msg)
            eventRepository.addEventLog(
                type = if (new.isGpsEnabled) "GPS_ENABLED" else "GPS_DISABLED",
                title = if (new.isGpsEnabled) 
                    trGlobal("GPS Aktif", "GPS Active", lang)
                else 
                    trGlobal("GPS Kapalı", "GPS Off", lang),
                detail = msg,
                status = if (new.isGpsEnabled) "BİLGİ" else "TEHLİKE"
            )
        }

        if (old.isBackgroundLocationGranted != new.isBackgroundLocationGranted) {
            val msg = if (new.isBackgroundLocationGranted) 
                trGlobal("Arka plan konum izni sağlandı.", "Background location permission granted.", lang)
            else 
                trGlobal("Arka plan konum izni iptal edildi! Takip kesilebilir.", "Background location permission revoked! Tracking may be interrupted.", lang)
            
            _statusAlert.emit(msg)
            eventRepository.addEventLog(
                type = "PERMISSION_CHANGED",
                title = trGlobal("Arka Plan Konum İzni", "Background Location Permission", lang),
                detail = msg,
                status = if (new.isBackgroundLocationGranted) "BİLGİ" else "UYARI"
            )
        }

        if (old.isNotificationGranted != new.isNotificationGranted) {
            val msg = if (new.isNotificationGranted) 
                trGlobal("Bildirim izni sağlandı.", "Notification permission granted.", lang)
            else 
                trGlobal("Bildirim izni iptal edildi! Önemli uyarıları alamayabilirsiniz.", "Notification permission revoked! You may miss important alerts.", lang)
            _statusAlert.emit(msg)
        }

        if (old.isCameraPermissionGranted != new.isCameraPermissionGranted) {
            val msg = if (new.isCameraPermissionGranted) 
                trGlobal("Kamera izni sağlandı.", "Camera permission granted.", lang)
            else 
                trGlobal("Kamera izni iptal edildi! OCR tarama çalışmayacaktır.", "Camera permission revoked! OCR scanning will not work.", lang)
            _statusAlert.emit(msg)
        }

        if (old.isBatteryOptimizationIgnored != new.isBatteryOptimizationIgnored) {
            if (!new.isBatteryOptimizationIgnored) {
                val msg = trGlobal(
                    "Pil optimizasyonu kısıtlaması tespit edildi. Uygulamanın arka planda kapanmaması için optimizasyonu devre dışı bırakın.",
                    "Battery optimization restriction detected. Disable optimization to prevent the app from closing in the background.",
                    lang
                )
                _statusAlert.emit(msg)
                eventRepository.addEventLog(
                    type = "BATTERY_OPTIMIZATION_RESTORED",
                    title = trGlobal("Pil Kısıtlaması Aktif", "Battery Restriction Active", lang),
                    detail = msg,
                    status = "UYARI"
                )
            }
        }
    }

    fun toggleGpsSimulation() {
        _deviceStatus.value = _deviceStatus.value.copy(isGpsEnabled = !_deviceStatus.value.isGpsEnabled)
        val lang = preferencesManager.language.value
        if (!_deviceStatus.value.isGpsEnabled) {
            viewModelScope.launch {
                eventRepository.addEventLog(
                    type = "GPS_DISABLED",
                    title = trGlobal("Konum Servisleri Kapatıldı", "Location Services Disabled", lang),
                    detail = trGlobal("Saha personeli konum servislerini veya GPS antenini devre dışı bıraktı.", "Field staff disabled location services or GPS antenna.", lang),
                    status = "TEHLİKE"
                )
            }
        }
    }

    fun toggleInternetSimulation() {
        val nextOnline = !_deviceStatus.value.isInternetConnected
        val nextType = if (nextOnline) ConnectionType.Wifi else ConnectionType.None
        _deviceStatus.value = _deviceStatus.value.copy(
            isInternetConnected = nextOnline,
            connectionType = nextType
        )
        val lang = preferencesManager.language.value
        viewModelScope.launch {
            if (!nextOnline) {
                eventRepository.addEventLog(
                    type = "INTERNET_LOST",
                    title = trGlobal("İnternet Bağlantı Kaybı", "Internet Connection Lost", lang),
                    detail = trGlobal("Şebeke bağlantısı kesildi. Çevrimdışı mod devreye girdi.", "Network connection lost. Offline mode activated.", lang),
                    status = "UYARI"
                )
            } else {
                eventRepository.addEventLog(
                    type = "INTERNET_RESTORED",
                    title = trGlobal("İnternet Bağlantısı Sağlandı", "Internet Connection Restored", lang),
                    detail = trGlobal("Şebeke bağlantısı yeniden sağlandı. Çevrimdışı veriler senkronize ediliyor.", "Network connection restored. Offline data is being synchronized.", lang),
                    status = "BİLGİ"
                )
                triggerOfflineSync()
            }
        }
    }

    fun triggerOfflineSync() {
        if (_isSyncing.value) return
        val lang = preferencesManager.language.value
        viewModelScope.launch {
            _isSyncing.value = true
            _lastSyncError.value = null
            try {
                val success = syncRepository.performOfflineSync()
                if (!success) {
                    _lastSyncError.value = trGlobal("Sunucu bağlantısı kurulamadı.", "Server connection could not be established.", lang)
                }
            } catch (e: Exception) {
                Timber.tag("DeviceViewModel").e(e, "Sync failed")
                _lastSyncError.value = e.message ?: trGlobal("Beklenmeyen bir hata oluştu.", "An unexpected error occurred.", lang)
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
