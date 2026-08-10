package com.sahatakip.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.sahatakip.util.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "saha_settings")

class PreferencesManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("key_language")
        private val KEY_UPDATE_INTERVAL = intPreferencesKey("key_update_interval")
        private val KEY_THEME = stringPreferencesKey("key_theme")
        private val KEY_SERVER_URL = stringPreferencesKey("key_server_url")
        private val KEY_DEVICE_ID_ENC = stringPreferencesKey("key_device_id_enc")
        private val KEY_DYNAMIC_CODE_ENC = stringPreferencesKey("key_dynamic_code_enc")
        
        const val DEFAULT_ACTIVATION_CODE = "SAHA2026"
    }

    val language: StateFlow<String> = context.dataStore.data
        .map { it[KEY_LANGUAGE] ?: "tr" }
        .stateIn(scope, SharingStarted.Eagerly, "tr")

    val updateInterval: StateFlow<Int> = context.dataStore.data
        .map { it[KEY_UPDATE_INTERVAL] ?: 60 }
        .stateIn(scope, SharingStarted.Eagerly, 60)

    val theme: StateFlow<String> = context.dataStore.data
        .map { it[KEY_THEME] ?: "system" }
        .stateIn(scope, SharingStarted.Eagerly, "system")

    val mockServerUrl: StateFlow<String> = context.dataStore.data
        .map { it[KEY_SERVER_URL] ?: "https://mock-api.example.com/v1/telemetry/sync" }
        .stateIn(scope, SharingStarted.Eagerly, "https://mock-api.example.com/v1/telemetry/sync")

    val deviceId: StateFlow<String> = context.dataStore.data
        .map { enc ->
            enc[KEY_DEVICE_ID_ENC]?.let { SecurityUtils.decrypt(it) } ?: ""
        }
        .stateIn(scope, SharingStarted.Eagerly, "")

    val dynamicActivationCode: StateFlow<String?> = context.dataStore.data
        .map { enc ->
            enc[KEY_DYNAMIC_CODE_ENC]?.let { SecurityUtils.decrypt(it) }
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    init {
        ensureDeviceIdExists()
    }

    private fun ensureDeviceIdExists() {
        scope.launch {
            val current = deviceId.value
            if (current.isBlank()) {
                val newId = UUID.randomUUID().toString()
                val encryptedId = SecurityUtils.encrypt(newId)
                context.dataStore.edit { it[KEY_DEVICE_ID_ENC] = encryptedId }
            }
        }
    }

    fun setLanguage(lang: String) {
        scope.launch {
            context.dataStore.edit { it[KEY_LANGUAGE] = lang }
        }
    }

    fun setUpdateInterval(seconds: Int) {
        scope.launch {
            context.dataStore.edit { it[KEY_UPDATE_INTERVAL] = seconds }
        }
    }

    fun setTheme(themeMode: String) {
        scope.launch {
            context.dataStore.edit { it[KEY_THEME] = themeMode }
        }
    }

    fun setMockServerUrl(url: String) {
        scope.launch {
            context.dataStore.edit { it[KEY_SERVER_URL] = url }
        }
    }

}
