package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.domain.service.LocationTrackingService
import com.example.ui.navigation.AppNavGraph
import com.example.ui.theme.SahaTakipTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create privacy notification channel
        NotificationHelper.createNotificationChannel(this)

        // Start Foreground Location Tracking Service
        try {
            val serviceIntent = Intent(this, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val themeMode by viewModel.theme.collectAsState()
            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            SahaTakipTheme(darkTheme = isDarkTheme) {
                AppNavGraph(viewModel = viewModel)
            }
        }
    }
}
