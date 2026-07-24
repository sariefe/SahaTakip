package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.MainViewModel
import com.example.util.tr

@SuppressLint("BatteryLife")
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToLogs: () -> Unit,
) {
    val context = LocalContext.current
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val latestLoc by viewModel.latestLocation.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // User Header Card
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToLogs() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile?.fullName ?: tr("Saha Personeli", "Field Staff"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${userProfile?.roleTitle ?: tr("Saha Operatörü", "Field Operator")} • ${tr("T.C.", "ID")} ${userProfile?.tcNo ?: "---"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.updateDeviceStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = tr("Yenile", "Refresh"))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ROOT / JAILBREAK SECURITY WARNING BANNER (If Rooted)
            if (deviceStatus.isRooted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = tr("GÜVENLİK UYARISI: Root Tespiti!", "SECURITY WARNING: Root Detected!"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusRed
                            )
                            Text(
                                text = tr("Cihazınızda yetkisiz kök erişimi tespit edildi. Güvenlik politikaları gereği verileriniz şifrelenmiştir.", "Unauthorized root access detected on device. Your data is encrypted per security policy."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // CRITICAL MISSING PERMISSIONS WARNING BANNER
            if (deviceStatus.hasMissingCriticalPermissions) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusRed.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr("Eksik İzin Uyarısı (${deviceStatus.missingPermissionsCount} Eksik)", "Missing Permission Warning (${deviceStatus.missingPermissionsCount} Missing)"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusRed
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = tr("Konum veya bildirim servislerinizin bir kısmı kapalı. Lütfen ayarlardan gerekli izinleri aktif ediniz.", "Some location or notification services are disabled. Please enable required permissions in settings."),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed)
                        ) {
                            Icon(Icons.Default.PhonelinkSetup, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Sistem Ayarlarına Git", "Go to System Settings"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Text(
                text = tr("Anlık Sistem & Servis Durumu", "Live System & Service Status"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // STATUS PANEL GRID
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 1. Internet Status Card
                StatusItemCard(
                    title = tr("İnternet Bağlantısı", "Internet Connection"),
                    subtitle = if (deviceStatus.isInternetConnected) tr("Çevrimiçi (Online)", "Online") else tr("Çevrimdışı (Offline Mode)", "Offline Mode"),
                    isOk = deviceStatus.isInternetConnected,
                    icon = if (deviceStatus.isInternetConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                    actionLabel = tr("Simüle Et", "Simulate"),
                    onAction = { viewModel.toggleInternetSimulation() }
                )

                // 2. Location Services Status Card
                StatusItemCard(
                    title = tr("GPS Konum Servisleri", "GPS Location Services"),
                    subtitle = if (deviceStatus.isGpsEnabled) tr("Açık • Yüksek Hassasiyet", "On • High Accuracy") else tr("KAPALI (Eksik İzin!)", "OFF (Missing Permission!)"),
                    isOk = deviceStatus.isGpsEnabled,
                    icon = if (deviceStatus.isGpsEnabled) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                    actionLabel = tr("Değiştir", "Toggle"),
                    onAction = { viewModel.toggleGpsSimulation() }
                )

                // 3. Background Location Work Permission Card
                StatusItemCard(
                    title = tr("Arka Plan Konum İzni", "Background Location Permission"),
                    subtitle = if (deviceStatus.isBackgroundLocationGranted) tr("İzin Verildi (Her Zaman)", "Granted (All the time)") else tr("Eksik: 'Her Zaman İzin Ver' seçilmeli", "Missing: 'Allow all the time' required"),
                    isOk = deviceStatus.isBackgroundLocationGranted,
                    icon = Icons.Default.LocationOn,
                    actionLabel = tr("İzinlere Git", "Permissions"),
                    onAction = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )

                // 3b. Battery Optimization Card
                StatusItemCard(
                    title = tr("Pil Optimizasyonu", "Battery Optimization"),
                    subtitle = if (deviceStatus.isBatteryOptimizationIgnored) tr("Kısıtlama Yok", "No Restrictions") else tr("Kısıtlı (Servis Durabilir)", "Restricted (Service may stop)"),
                    isOk = deviceStatus.isBatteryOptimizationIgnored,
                    icon = Icons.Default.BatteryFull,
                    actionLabel = tr("Devre Dışı Bırak", "Disable"),
                    onAction = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Fallback to general battery optimization settings if direct request fails
                            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(fallbackIntent)
                        }
                    }
                )

                // 4. Notification Permission Card
                StatusItemCard(
                    title = tr("Bildirim İzni", "Notification Permission"),
                    subtitle = if (deviceStatus.isNotificationGranted) tr("İzin Verildi (Gizlilik Safe)", "Granted (Privacy Safe)") else tr("Bildirimler Kapalı", "Notifications Disabled"),
                    isOk = deviceStatus.isNotificationGranted,
                    icon = if (deviceStatus.isNotificationGranted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    actionLabel = tr("Ayarla", "Settings"),
                    onAction = {
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            } else {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )

                // 5. Battery Level Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (deviceStatus.isBatteryCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                            contentDescription = null,
                            tint = if (deviceStatus.batteryLevel > 20) StatusGreen else StatusRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${tr("Pil Seviyesi", "Battery Level")}: %${deviceStatus.batteryLevel} ${if (deviceStatus.isBatteryCharging) tr("(Şarj Oluyor)", "(Charging)") else ""}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { deviceStatus.batteryLevel / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = if (deviceStatus.batteryLevel > 20) StatusGreen else StatusRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // LAST RECORDED GPS POSITION CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToMap() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr("Son Kaydedilen Saha Konumu", "Last Recorded Field Location"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = latestLoc?.address ?: tr("Merkez Saha Bölgesi (41.0082, 28.9784)", "Central Field Zone (41.0082, 28.9784)"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${tr("Enlem", "Lat")}: ${latestLoc?.latitude ?: 41.0082} • ${tr("Boylam", "Lng")}: ${latestLoc?.longitude ?: 28.9784} • ${tr("Hız", "Speed")}: ${latestLoc?.speed?.toInt() ?: 0} km/h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SYNC QUEUE CONTROL
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Çevrimdışı Veri Senkronizasyonu", "Offline Data Synchronization"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (deviceStatus.isInternetConnected) tr("İnternet aktif. Veriler sunucu ile eşzamanlı.", "Internet active. Data synchronized with server.") else tr("Çevrimdışı mod: Veriler lokalde saklanıyor.", "Offline mode: Data stored locally."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { viewModel.triggerOfflineSync() },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text(tr("Senkronize Et", "Sync Now"), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusItemCard(
    title: String,
    subtitle: String,
    isOk: Boolean,
    icon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOk) MaterialTheme.colorScheme.surface else StatusRed.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isOk) StatusGreen else StatusRed,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOk) MaterialTheme.colorScheme.onSurfaceVariant else StatusRed
                )
            }
            OutlinedButton(
                onClick = onAction,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isOk) MaterialTheme.colorScheme.primary else StatusRed
                )
            ) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
