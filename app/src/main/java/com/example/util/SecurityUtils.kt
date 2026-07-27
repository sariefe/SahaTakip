package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SecurityUtils {

    /**
     * Checks common indicators for root access on Android.
     * Run on IO thread to avoid blocking main thread with file lookups.
     */
    suspend fun checkIsDeviceRooted(): Boolean = withContext(Dispatchers.IO) {
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in rootPaths) {
            try {
                if (File(path).exists()) return@withContext true
            } catch (_: Exception) {
                // Ignore permission issues for specific paths
            }
        }
        val buildTags = android.os.Build.TAGS
        buildTags != null && buildTags.contains("test-keys")
    }
}
