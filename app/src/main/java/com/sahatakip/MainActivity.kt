package com.sahatakip

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sahatakip.domain.repository.UserRepository
import com.sahatakip.domain.service.LocationTrackingService
import com.sahatakip.ui.navigation.AppNavGraph
import com.sahatakip.ui.theme.SahaTakipTheme
import com.sahatakip.ui.viewmodel.DeviceViewModel
import com.sahatakip.ui.viewmodel.SettingsViewModel
import com.sahatakip.util.LocalLanguage
import com.sahatakip.util.NotificationHelper
import com.sahatakip.util.PermissionUtils
import com.sahatakip.util.SecurityUtils
import com.sahatakip.util.tr
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

    private var showRootWarning by mutableStateOf(false)

    private val requestBackgroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
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
            if (SecurityUtils.checkIsDeviceRooted()) {
                showRootWarning = true
            }
        }

        NotificationHelper.createNotificationChannel(this)
        lifecycleScope.launch {
            delay(500.milliseconds)
            checkAndRequestPermissions()
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themeMode by settingsViewModel.theme.collectAsStateWithLifecycle()
            val language by settingsViewModel.language.collectAsStateWithLifecycle()
            
            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            CompositionLocalProvider(LocalLanguage provides language) {
                SahaTakipTheme(darkTheme = isDarkTheme) {
                    AppNavGraph(
                        windowSizeClass = windowSizeClass
                    )

                    if (showRootWarning) {
                        AlertDialog(
                            onDismissRequest = { showRootWarning = false },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.GppBad,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            title = {
                                Text(
                                    text = tr("Güvenlik Uyarısı", "Security Warning"),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = tr(
                                        "Cihazınızda root (yönetici) erişimi tespit edildi. Saha takibi verilerinin güvenliği ve doğruluğu için orijinal işletim sistemi kullanmanız önerilir. Devam etmeniz halinde oluşabilecek güvenlik açıklarından kullanıcı sorumludur.",
                                        "Root (administrator) access has been detected on your device. For the security and accuracy of field tracking data, it is recommended to use the original operating system. The user is responsible for any security vulnerabilities that may occur if you continue."
                                    ),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = { showRootWarning = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(text = tr("Anladım, Devam Et", "I Understand, Continue"))
                                }
                            },
                            shape = RoundedCornerShape(28.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        )
                    }
                }
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
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) && (e is android.app.ForegroundServiceStartNotAllowedException)) {
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
