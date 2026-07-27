package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("saha_preferences", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "tr") ?: "tr")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _updateInterval = MutableStateFlow(prefs.getInt(KEY_UPDATE_INTERVAL, 60))
    val updateInterval: StateFlow<Int> = _updateInterval.asStateFlow()

    private val _theme = MutableStateFlow(prefs.getString(KEY_THEME, "system") ?: "system")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _mockServerUrl = MutableStateFlow(
        prefs.getString(KEY_SERVER_URL, "https://mock-api.example.com/v1/telemetry/sync") ?: "https://mock-api.example.com/v1/telemetry/sync"
    )
    val mockServerUrl: StateFlow<String> = _mockServerUrl.asStateFlow()

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
        const val DEFAULT_ACTIVATION_CODE = "SAHA2026"
    }
}
