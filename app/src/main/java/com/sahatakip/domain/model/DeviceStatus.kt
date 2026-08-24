package com.sahatakip.domain.model

import com.sahatakip.util.ConnectionType

data class DeviceStatus(
    val isInternetConnected: Boolean = true,
    val connectionType: ConnectionType = ConnectionType.None,
    val isGpsEnabled: Boolean = true,
    val isBackgroundLocationGranted: Boolean = true,
    val isNotificationGranted: Boolean = true,
    val isCameraPermissionGranted: Boolean = true,
    val isBatteryOptimizationIgnored: Boolean = true,
    val batteryLevel: Int = 90,
    val isBatteryCharging: Boolean = false,
    val isPowerSaveModeActive: Boolean = false,
    val isRooted: Boolean = false
) {
    val hasMissingCriticalPermissions: Boolean
        get() = !isGpsEnabled || !isBackgroundLocationGranted || !isNotificationGranted || !isCameraPermissionGranted
}
