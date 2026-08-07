package com.sahatakip.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahatakip.ui.theme.StatusAmber
import com.sahatakip.ui.theme.StatusGreen
import com.sahatakip.ui.theme.StatusRed
import com.sahatakip.ui.viewmodel.AuthViewModel
import com.sahatakip.ui.viewmodel.DeviceViewModel
import com.sahatakip.ui.viewmodel.TrackingViewModel
import com.sahatakip.util.tr

@SuppressLint("BatteryLife")
@Composable
fun DashboardScreen(
    deviceViewModel: DeviceViewModel,
    trackingViewModel: TrackingViewModel,
    authViewModel: AuthViewModel,
    onNavigateToMap: () -> Unit,
    windowWidthSizeClass: WindowWidthSizeClass,
) {
    val context = LocalContext.current
    val deviceStatus by deviceViewModel.deviceStatus.collectAsState()
    val userProfile by trackingViewModel.userProfile.collectAsState()
    val latestLoc by trackingViewModel.latestLocation.collectAsState()
    val isSyncing by deviceViewModel.isSyncing.collectAsState()
    val syncError by deviceViewModel.lastSyncError.collectAsState()
    
    var showProfileModal by remember { mutableStateOf(value = false) }
    var showBackgroundPermissionRationale by remember { mutableStateOf(false) }

    if (showBackgroundPermissionRationale) {
        BackgroundPermissionDialog(
            onDismiss = { showBackgroundPermissionRationale = false },
            onConfirm = {
                showBackgroundPermissionRationale = false
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // User Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = tr("Hoş Geldin,", "Welcome back,"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = userProfile?.fullName ?: tr("Saha Personeli", "Field Staff"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { showProfileModal = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // CRITICAL WARNINGS
            if (deviceStatus.isRooted || deviceStatus.hasMissingCriticalPermissions) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when {
                                !deviceStatus.isGpsEnabled -> {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                    } catch (_: Exception) {}
                                }
                                !deviceStatus.isNotificationGranted -> {
                                    try {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                                !deviceStatus.isBackgroundLocationGranted -> {
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = android.net.Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                                !deviceStatus.isBatteryOptimizationIgnored -> {
                                    try {
                                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            }
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = (if (deviceStatus.isRooted || !deviceStatus.isGpsEnabled) StatusRed else StatusAmber).copy(alpha = 0.1f)
                    )
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GppBad, contentDescription = null, tint = StatusRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val (title, color) = when {
                                deviceStatus.isRooted -> tr("Güvenlik Riski", "Security Risk") to StatusRed
                                !deviceStatus.isGpsEnabled || !deviceStatus.isNotificationGranted || !deviceStatus.isBackgroundLocationGranted -> tr("Eksik İzinler", "Missing Permissions") to StatusRed
                                deviceStatus.isPowerSaveModeActive || !deviceStatus.isBatteryOptimizationIgnored -> tr("Pil Kısıtlaması", "Battery Restriction") to StatusAmber
                                else -> tr("Sistem Uyarısı", "System Warning") to StatusAmber
                            }
                            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
                            val desc = when {
                                deviceStatus.isPowerSaveModeActive -> tr("Cihaz tasarruf modunda. Takip hassasiyeti azalabilir. Kapatmak için dokunun.", "Device in save mode. Tracking accuracy may decrease. Tap to disable.")
                                deviceStatus.isRooted -> tr("Cihaz rootlu tespit edildi. Güvenlik politikaları gereği bazı özellikler kısıtlanmış olabilir.", "Device is rooted. Some features may be restricted due to security policies.")
                                !deviceStatus.isGpsEnabled -> tr("Konum servisleri kapalı. Takip yapılamıyor.", "GPS is disabled. Tracking is unavailable.")
                                !deviceStatus.isNotificationGranted -> tr("Bildirim izni eksik. Servis durumu takip edilemiyor.", "Notifications disabled. Service status cannot be monitored.")
                                else -> tr("Uygulamanın arka planda kesintisiz çalışması için ek izinler gerekiyor.", "Additional permissions required for seamless background operation.")
                            }
                            Text(desc, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // TELEMETRY GRID
            Column {
                Text(
                    text = tr("Servis Durumları", "Service Status"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatusGridItem(
                                modifier = Modifier.weight(1f),
                                title = tr("İnternet", "Internet"),
                                isOk = deviceStatus.isInternetConnected,
                                icon = if (deviceStatus.isInternetConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                            ) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                                } catch (_: Exception) {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                                    } catch (_: Exception) {}
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            StatusGridItem(
                                modifier = Modifier.weight(1f),
                                title = tr("GPS", "GPS"),
                                isOk = deviceStatus.isGpsEnabled,
                                icon = if (deviceStatus.isGpsEnabled) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                            ) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                } catch (_: Exception) {}
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatusGridItem(
                                modifier = Modifier.weight(1f),
                                title = tr("Arka Plan", "Background"),
                                isOk = deviceStatus.isBackgroundLocationGranted,
                                statusColor = if (deviceStatus.isBackgroundLocationGranted) StatusGreen else StatusAmber,
                                icon = Icons.Default.LocationOn,
                            ) {
                                if (!deviceStatus.isBackgroundLocationGranted) {
                                    showBackgroundPermissionRationale = true
                                } else {
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = android.net.Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            StatusGridItem(
                                modifier = Modifier.weight(1f),
                                title = tr("Bildirimler", "Notifications"),
                                isOk = deviceStatus.isNotificationGranted,
                                icon = if (deviceStatus.isNotificationGranted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            ) {
                                try {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatusGridItem(
                            modifier = Modifier.weight(1f),
                            title = tr("İnternet", "Internet"),
                            isOk = deviceStatus.isInternetConnected,
                            icon = if (deviceStatus.isInternetConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        ) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                                } catch (_: Exception) {}
                            }
                        }
                        StatusGridItem(
                            modifier = Modifier.weight(1f),
                            title = tr("GPS", "GPS"),
                            isOk = deviceStatus.isGpsEnabled,
                            icon = if (deviceStatus.isGpsEnabled) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                        ) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            } catch (_: Exception) {}
                        }
                        StatusGridItem(
                            modifier = Modifier.weight(1f),
                            title = tr("Arka Plan", "Background"),
                            isOk = deviceStatus.isBackgroundLocationGranted,
                            statusColor = if (deviceStatus.isBackgroundLocationGranted) StatusGreen else StatusAmber,
                            icon = Icons.Default.LocationOn,
                        ) {
                            if (!deviceStatus.isBackgroundLocationGranted) {
                                showBackgroundPermissionRationale = true
                            } else {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                        StatusGridItem(
                            modifier = Modifier.weight(1f),
                            title = tr("Bildirimler", "Notifications"),
                            isOk = deviceStatus.isNotificationGranted,
                            icon = if (deviceStatus.isNotificationGranted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        ) {
                            try {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }

            // INTERACTIVE MAP PREVIEW CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .clickable { onNavigateToMap() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceVariant)
                                )
                            )
                    )
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                tr("CANLI KONUM", "LIVE LOCATION"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = latestLoc?.address ?: tr("Konum alınıyor...", "Fetching location..."),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = tr("Detaylı rotayı görmek için dokunun", "Tap to view detailed route"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // BATTERY & SYNC SUMMARY
            Card(
                modifier = Modifier.fillMaxWidth().testTag("BatteryCard"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (deviceStatus.isBatteryCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                contentDescription = null,
                                tint = if (deviceStatus.batteryLevel > 20) StatusGreen else StatusRed
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${tr("Pil", "Battery")}: %${deviceStatus.batteryLevel}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Button(
                                onClick = { deviceViewModel.triggerOfflineSync() },
                                enabled = !isSyncing,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Text(tr("Senkronize", "Sync"), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            syncError?.let {
                                Text(
                                    text = it,
                                    color = StatusRed,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LinearProgressIndicator(
                        progress = { deviceStatus.batteryLevel / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = if (deviceStatus.batteryLevel > 20) MaterialTheme.colorScheme.primary else StatusRed,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showProfileModal) {
        ProfileInfoModal(
            user = userProfile,
            onDismiss = { showProfileModal = false },
            onLogout = { 
                authViewModel.logout()
                showProfileModal = false
            }
        )
    }
}


@Composable
fun ProfileInfoModal(
    user: com.sahatakip.data.local.entity.UserProfileEntity?,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 8.dp,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Kapat", "Close"), fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(tr("Personel Bilgileri", "Staff Information"), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileDetailRow(Icons.Default.Person, tr("Ad Soyad", "Full Name"), user?.fullName ?: "-")
                ProfileDetailRow(Icons.Default.Badge, tr("Personel ID", "Staff ID"), user?.staffId ?: "-")
                ProfileDetailRow(Icons.Default.Info, tr("Departman", "Department"), user?.department ?: "-")
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                Text(
                    text = tr("Kayıt Tarihi: ", "Registered: ") + (user?.registeredAt?.let { java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(it) } ?: "-"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("Çıkış Yap", "Log Out"), fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
fun ProfileDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun StatusGridItem(
    modifier: Modifier = Modifier,
    title: String,
    isOk: Boolean,
    statusColor: Color = if (isOk) StatusGreen else StatusRed,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOk) MaterialTheme.colorScheme.surface else statusColor.copy(alpha = 0.05f)
        ),
        border = if (!isOk) BorderStroke(1.dp, statusColor.copy(alpha = 0.2f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(24.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
            }
        }
    }
}

@Composable
fun BackgroundPermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(tr("Arka Plan Konum İzni", "Background Location"), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    tr(
                        "Saha takibinin kesintisiz çalışması için konum iznini 'Her zaman izin ver' olarak ayarlamanız gerekmektedir.",
                        "To keep tracking active while the app is in the background, please set location permission to 'Allow all the time'."
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PermissionStepItem(1, tr("Ayarlara gidin", "Go to Settings"))
                        PermissionStepItem(2, tr("'İzinler' bölümünü açın", "Open 'Permissions'"))
                        PermissionStepItem(3, tr("'Konum' seçeneğine dokunun", "Tap on 'Location'"))
                        PermissionStepItem(4, tr("'Her zaman izin ver'i seçin", "Select 'Allow all the time'"))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(tr("Ayarları Aç", "Open Settings"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("İptal", "Cancel"))
            }
        }
    )
}

@Composable
private fun PermissionStepItem(index: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(20.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(index.toString(), style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
