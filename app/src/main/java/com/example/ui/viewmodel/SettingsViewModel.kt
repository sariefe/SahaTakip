package com.example.ui.viewmodel

import com.example.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
) : androidx.lifecycle.ViewModel() {

    val language = preferencesManager.language
    val updateInterval = preferencesManager.updateInterval
    val theme = preferencesManager.theme
    val mockServerUrl = preferencesManager.mockServerUrl

    fun setLanguage(lang: String) = preferencesManager.setLanguage(lang)
    fun setUpdateInterval(seconds: Int) = preferencesManager.setUpdateInterval(seconds)
    fun setTheme(themeMode: String) = preferencesManager.setTheme(themeMode)
    fun setMockServerUrl(url: String) = preferencesManager.setMockServerUrl(url)
}
