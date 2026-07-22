package com.example.domain.model

data class DeviceStatus(
    val isInternetConnected: Boolean = true,
    val isGpsEnabled: Boolean = true,
    val isBackgroundLocationGranted: Boolean = true,
    val isNotificationGranted: Boolean = true,
    val batteryLevel: Int = 85,
    val isBatteryCharging: Boolean = false,
    val isRooted: Boolean = false,
    val lastCheckedTimestamp: Long = System.currentTimeMillis()
) {
    val hasMissingCriticalPermissions: Boolean
        get() = !isGpsEnabled || !isBackgroundLocationGranted || !isNotificationGranted

    val missingPermissionsCount: Int
        get() {
            var count = 0
            if (!isGpsEnabled) count++
            if (!isBackgroundLocationGranted) count++
            if (!isNotificationGranted) count++
            if (!isInternetConnected) count++
            return count
        }
}
