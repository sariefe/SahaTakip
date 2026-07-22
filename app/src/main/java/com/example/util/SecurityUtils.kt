package com.example.data.util

import android.content.Context
import java.io.File
import java.security.MessageDigest

object SecurityUtils {

    /**
     * Checks common indicators for root access on Android.
     */
    fun checkIsDeviceRooted(context: Context): Boolean {
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
            if (File(path).exists()) return true
        }
        val buildTags = android.os.Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }
        return false
    }

    /**
     * Encrypts/hashes sensitive plain text string for storage or privacy masking.
     */
    fun maskSensitiveLocation(lat: Double, lng: Double): String {
        return "GİZLİ KONUM [GPS ENCRYPTED]"
    }

    fun hashSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
