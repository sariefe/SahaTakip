package com.sahatakip.domain.model

data class DeviceStatus(
    val isInternetConnected: Boolean = true,
    val isGpsEnabled: Boolean = true,
    val isBackgroundLocationGranted: Boolean = true,
    val isNotificationGranted: Boolean = true,
    val isBatteryOptimizationIgnored: Boolean = true,
    val batteryLevel: Int = 90,
    val isBatteryCharging: Boolean = false,
    val isPowerSaveModeActive: Boolean = false,
    val isRooted: Boolean = false
) {
    val hasMissingCriticalPermissions: Boolean
        get() = !isGpsEnabled || !isBackgroundLocationGranted || !isNotificationGranted || !isBatteryOptimizationIgnored || isPowerSaveModeActive

    val isBackgroundExecutionOk: Boolean
        get() = isBackgroundLocationGranted && isBatteryOptimizationIgnored && !isPowerSaveModeActive

}
