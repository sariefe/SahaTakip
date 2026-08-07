package com.sahatakip

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
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahatakip.domain.repository.UserRepository
import com.sahatakip.domain.service.LocationTrackingService
import com.sahatakip.ui.navigation.AppNavGraph
import com.sahatakip.ui.theme.SahaTakipTheme
import com.sahatakip.ui.viewmodel.DeviceViewModel
import com.sahatakip.ui.viewmodel.SettingsViewModel
import com.sahatakip.util.NotificationHelper
import com.sahatakip.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    private val deviceViewModel: DeviceViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val requestBackgroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            deviceViewModel.updateDeviceStatus()
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
        
        deviceViewModel.updateDeviceStatus()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )

        lifecycleScope.launch {
            userRepository.initializeAndSyncDefaultData()
        }

        NotificationHelper.createNotificationChannel(this)
        lifecycleScope.launch {
            delay(500.milliseconds)
            checkAndRequestPermissions()
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themeMode by settingsViewModel.theme.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            SahaTakipTheme(darkTheme = isDarkTheme) {
                AppNavGraph(
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        deviceViewModel.updateDeviceStatus()
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
            try {
                startForegroundService(serviceIntent)
            } catch (e: Exception) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is android.app.ForegroundServiceStartNotAllowedException) {
                    android.util.Log.e("MainActivity", "Foreground service start not allowed from background", e)
                } else {
                    throw e
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start tracking service", e)
        }
    }
}
