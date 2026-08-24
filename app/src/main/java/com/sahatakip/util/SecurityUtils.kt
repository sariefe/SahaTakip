package com.sahatakip.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecurityUtils {

    private const val KEY_ALIAS = "saha_takip_master_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun generateMasterKeyIfNeeded() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                keyGenerator.init(
                    KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build(),
                )
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            Timber.tag("SecurityUtils").e(e, "Failed to initialize AndroidKeyStore")
        }
    }

    private fun getMasterKey(): SecretKey? {
        return try {
            generateMasterKeyIfNeeded()
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        } catch (_: Exception) {
            null
        }
    }

    fun encrypt(text: String): String {
        val masterKey = getMasterKey() ?: return Base64.encodeToString(text.toByteArray(), Base64.DEFAULT)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decrypt(encryptedBase64: String): String? {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.DEFAULT)
            val masterKey = getMasterKey() ?: return String(combined, Charsets.UTF_8)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val ivSize = 12
            val iv = combined.sliceArray(0 until ivSize)
            val encryptedBytes = combined.sliceArray(ivSize until combined.size)

            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

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
            "/data/local/su",
            "/su/bin/su",
            "/system/xbin/daemonsu",
            "/system/etc/init.d/99SuperSUDaemon",
            "/system/bin/.ext/.su",
            "/system/etc/.has_su_daemon",
            "/system/etc/.installed_su_daemon",
            "/dev/com.koushikdutta.superuser.daemon/",
            "/system/xbin/busybox",
            "/system/bin/busybox",
            "/sbin/magisk",
            "/cache/magisk.log",
            "/data/magisk.img",
            "/data/magisk/"
        )
        for (path in rootPaths) {
            try {
                if (File(path).exists()) return@withContext true
            } catch (_: Exception) {
            }
        }

        val buildTags = android.os.Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) return@withContext true

        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = process.inputStream.bufferedReader()
            if (reader.readLine() != null) return@withContext true
        } catch (_: Throwable) {
        } finally {
            process?.destroy()
        }

        val properties = mapOf("ro.debuggable" to "1", "ro.secure" to "0")
        for ((prop, rootedValue) in properties) {
            try {
                val p = Runtime.getRuntime().exec(arrayOf("getprop", prop))
                val value = p.inputStream.bufferedReader().readLine()
                if (value == rootedValue) return@withContext true
            } catch (_: Throwable) {
            }
        }

        false
    }
}
