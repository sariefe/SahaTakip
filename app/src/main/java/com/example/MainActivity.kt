package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.domain.service.LocationTrackingService
import com.example.ui.navigation.AppNavGraph
import com.example.ui.theme.SahaTakipTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.util.NotificationHelper
import com.example.util.PermissionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestBackgroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.updateDeviceStatus()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            requestBackgroundLocationPermission()
            startTrackingService()
        } else {
            Toast.makeText(this, "Saha takibi için konum izni gereklidir.", Toast.LENGTH_LONG).show()
        }
        
        viewModel.updateDeviceStatus()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createNotificationChannel(this)
        lifecycleScope.launch {
            delay(500.milliseconds)
            checkAndRequestPermissions()
        }

        // Automatic Sync on Connectivity
        lifecycleScope.launch {
            val observer = com.example.util.ConnectivityObserver(this@MainActivity)
            observer.observe().collect { status ->
                if (status == com.example.util.ConnectivityStatus.Available) {
                    viewModel.triggerOfflineSync()
                }
            }
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themeMode by viewModel.theme.collectAsState()
            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            SahaTakipTheme(darkTheme = isDarkTheme) {
                AppNavGraph(
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.updateDeviceStatus()
    }

    private fun checkAndRequestPermissions() {
        val required = PermissionUtils.getRequiredPermissions()
        val toRequest = required.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (toRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(toRequest)
        } else {
            requestBackgroundLocationPermission()
            startTrackingService()
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Arka Plan Konum İzni")
                    .setMessage("Saha takibinin kesintisiz devam etmesi için konum iznini 'Her zaman izin ver' olarak ayarlamanız gerekmektedir.")
                    .setPositiveButton("Ayarlara Git") { _, _ ->
                        requestBackgroundPermissionLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                    .setNegativeButton("İptal", null)
                    .show()
            }
        }
    }

    private fun startTrackingService() {
        try {
            val serviceIntent = Intent(this, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    startForegroundService(serviceIntent)
                } catch (e: Exception) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is android.app.ForegroundServiceStartNotAllowedException) {
                        android.util.Log.e("MainActivity", "Foreground service start not allowed from background", e)
                    } else {
                        throw e
                    }
                }
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start tracking service", e)
        }
    }
}
