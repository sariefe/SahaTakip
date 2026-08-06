package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("saha_preferences", Context.MODE_PRIVATE)

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            KEY_LANGUAGE -> _language.value = sharedPreferences.getString(KEY_LANGUAGE, "tr") ?: "tr"
            KEY_UPDATE_INTERVAL -> _updateInterval.value = sharedPreferences.getInt(KEY_UPDATE_INTERVAL, 60)
            KEY_THEME -> _theme.value = sharedPreferences.getString(KEY_THEME, "system") ?: "system"
            KEY_SERVER_URL -> _mockServerUrl.value = sharedPreferences.getString(KEY_SERVER_URL, "https://mock-api.example.com/v1/telemetry/sync") ?: ""
            KEY_DEVICE_ID -> _deviceId.value = sharedPreferences.getString(KEY_DEVICE_ID, "") ?: ""
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        ensureDeviceIdExists()
    }

    private val _language = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "tr") ?: "tr")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _updateInterval = MutableStateFlow(prefs.getInt(KEY_UPDATE_INTERVAL, 60))
    val updateInterval: StateFlow<Int> = _updateInterval.asStateFlow()

    private val _theme = MutableStateFlow(prefs.getString(KEY_THEME, "system") ?: "system")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _mockServerUrl = MutableStateFlow(
        prefs.getString(KEY_SERVER_URL, "https://mock-api.example.com/v1/telemetry/sync")
            ?: "https://mock-api.example.com/v1/telemetry/sync",
    )
    val mockServerUrl: StateFlow<String> = _mockServerUrl.asStateFlow()

    private val _deviceId = MutableStateFlow(prefs.getString(KEY_DEVICE_ID, "") ?: "")
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    private fun ensureDeviceIdExists() {
        if (prefs.getString(KEY_DEVICE_ID, "").isNullOrBlank()) {
            val newId = java.util.UUID.randomUUID().toString()
            prefs.edit { putString(KEY_DEVICE_ID, newId) }
            _deviceId.value = newId
        }
    }

    fun setLanguage(lang: String) {
        prefs.edit { putString(KEY_LANGUAGE, lang) }
        _language.value = lang
    }

    fun setUpdateInterval(seconds: Int) {
        prefs.edit { putInt(KEY_UPDATE_INTERVAL, seconds) }
        _updateInterval.value = seconds
    }

    fun setTheme(themeMode: String) {
        prefs.edit { putString(KEY_THEME, themeMode) }
        _theme.value = themeMode
    }

    fun setMockServerUrl(url: String) {
        prefs.edit { putString(KEY_SERVER_URL, url) }
        _mockServerUrl.value = url
    }

    companion object {
        const val KEY_LANGUAGE = "key_language"
        const val KEY_UPDATE_INTERVAL = "key_update_interval"
        const val KEY_THEME = "key_theme"
        const val KEY_SERVER_URL = "key_server_url"
        const val KEY_DEVICE_ID = "key_device_id"
        const val DEFAULT_ACTIVATION_CODE = "SAHA2026"
    }
}
