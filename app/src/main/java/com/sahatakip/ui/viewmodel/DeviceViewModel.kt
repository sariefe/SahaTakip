package com.sahatakip.ui.viewmodel

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.lifecycle.viewModelScope
import com.sahatakip.domain.model.DeviceStatus
import com.sahatakip.domain.repository.EventRepository
import com.sahatakip.domain.repository.SyncRepository
import com.sahatakip.util.PermissionUtils
import com.sahatakip.util.SecurityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventRepository: EventRepository,
    private val syncRepository: SyncRepository,
    private val connectivityObserver: com.sahatakip.util.ConnectivityObserver,
) : androidx.lifecycle.ViewModel() {

    private val _deviceStatus = MutableStateFlow(value = DeviceStatus())
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(value = false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(value = null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

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
                eventRepository.addEventLog(
                    type = "POWER_MODE_CHANGED",
                    title = if (isPowerSave) "Düşük Güç Modu Aktif" else "Normal Güç Moduna Geçildi",
                    detail = if (isPowerSave) 
                        "Cihaz pil tasarrufu moduna girdi. Konum hassasiyeti ve arka plan aktiviteleri kısıtlanabilir."
                    else 
                        "Cihaz normal güç moduna döndü. Takip servisleri tam kapasite çalışıyor.",
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
            connectivityObserver.observe().collect { status ->
                val isOnline = status == com.sahatakip.util.ConnectivityStatus.Available
                _deviceStatus.value = _deviceStatus.value.copy(isInternetConnected = isOnline)
                if (isOnline) triggerOfflineSync()
            }
        }

        context.registerReceiver(
            gpsReceiver,
            IntentFilter(android.location.LocationManager.PROVIDERS_CHANGED_ACTION)
        )

        context.registerReceiver(
            powerReceiver,
            IntentFilter(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        )

        context.registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        try {
            context.unregisterReceiver(gpsReceiver)
            context.unregisterReceiver(powerReceiver)
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            android.util.Log.e("DeviceViewModel", "Error unregistering receivers", e)
        }
        super.onCleared()
    }

    fun updateDeviceStatus() {
        viewModelScope.launch {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
            val isOnline = (capabilities != null) &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))

            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 85
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val batteryPct = if ((level >= 0) && (scale > 0)) (level * 100 / scale.toFloat()).toInt() else 85
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            val isRooted = cachedIsRooted ?: SecurityUtils.checkIsDeviceRooted().also {
                cachedIsRooted = it
            }

            _deviceStatus.value = DeviceStatus(
                isInternetConnected = isOnline,
                isGpsEnabled = PermissionUtils.isGpsEnabled(context) && PermissionUtils.hasLocationPermissions(context),
                isBackgroundLocationGranted = PermissionUtils.hasBackgroundLocationPermission(context),
                isNotificationGranted = PermissionUtils.hasNotificationPermission(context),
                isBatteryOptimizationIgnored = PermissionUtils.isIgnoringBatteryOptimizations(context),
                isPowerSaveModeActive = PermissionUtils.isPowerSaveMode(context),
                batteryLevel = batteryPct,
                isBatteryCharging = isCharging,
                isRooted = isRooted
            )
        }
    }

    fun toggleGpsSimulation() {
        _deviceStatus.value = _deviceStatus.value.copy(isGpsEnabled = !_deviceStatus.value.isGpsEnabled)
        if (!_deviceStatus.value.isGpsEnabled) {
            viewModelScope.launch {
                eventRepository.addEventLog(
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
                eventRepository.addEventLog(
                    type = "INTERNET_LOST",
                    title = "İnternet Bağlantı Kaybı",
                    detail = "Şebeke bağlantısı kesildi. Çevrimdışı mod devreye girdi.",
                    status = "UYARI"
                )
            } else {
                eventRepository.addEventLog(
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
        if (_isSyncing.value) return
        
        viewModelScope.launch {
            _isSyncing.value = true
            _lastSyncError.value = null
            try {
                val success = syncRepository.performOfflineSync()
                if (!success) {
                    _lastSyncError.value = "Sunucu bağlantısı kurulamadı."
                }
            } catch (e: Exception) {
                android.util.Log.e("DeviceViewModel", "Sync failed", e)
                _lastSyncError.value = e.message ?: "Beklenmeyen bir hata oluştu."
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
