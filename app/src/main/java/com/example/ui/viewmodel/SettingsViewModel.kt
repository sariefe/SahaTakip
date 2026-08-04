package com.example.ui.viewmodel

import com.example.data.repository.SahaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SahaRepository
) : androidx.lifecycle.ViewModel() {

    val language = repository.preferencesManager.language
    val updateInterval = repository.preferencesManager.updateInterval
    val theme = repository.preferencesManager.theme
    val mockServerUrl = repository.preferencesManager.mockServerUrl

    fun setLanguage(lang: String) = repository.preferencesManager.setLanguage(lang)
    fun setUpdateInterval(seconds: Int) = repository.preferencesManager.setUpdateInterval(seconds)
    fun setTheme(themeMode: String) = repository.preferencesManager.setTheme(themeMode)
    fun setMockServerUrl(url: String) = repository.preferencesManager.setMockServerUrl(url)
}
