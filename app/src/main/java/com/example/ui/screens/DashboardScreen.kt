package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Transgender
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
    windowWidthSizeClass: WindowWidthSizeClass
) {
    val context = LocalContext.current
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val latestLoc by viewModel.latestLocation.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    
    var showProfileModal by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // User Header - Sleek Modern Design
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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

            Spacer(modifier = Modifier.height(20.dp))

            // CRITICAL WARNINGS
            if (deviceStatus.isRooted || deviceStatus.hasMissingCriticalPermissions) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusRed.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GppBad, contentDescription = null, tint = StatusRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val title = if (deviceStatus.isRooted) tr("Güvenlik Riski", "Security Risk") else tr("Eksik İzinler", "Missing Permissions")
                            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = StatusRed)
                            Text(
                                tr("Sistem güvenliği veya takibi için aksiyon almanız gerekiyor.", "Action required for system security or tracking."),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // TELEMETRY GRID
            Text(
                text = tr("Servis Durumları", "Service Status"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatusGridItem(
                        modifier = Modifier.weight(1f),
                        title = tr("İnternet", "Internet"),
                        isOk = deviceStatus.isInternetConnected,
                        icon = if (deviceStatus.isInternetConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        onClick = { viewModel.toggleInternetSimulation() }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    StatusGridItem(
                        modifier = Modifier.weight(1f),
                        title = tr("GPS", "GPS"),
                        isOk = deviceStatus.isGpsEnabled,
                        icon = if (deviceStatus.isGpsEnabled) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                        onClick = { viewModel.toggleGpsSimulation() }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatusGridItem(
                        modifier = Modifier.weight(1f),
                        title = tr("Arka Plan", "Background"),
                        isOk = deviceStatus.isBackgroundLocationGranted,
                        icon = Icons.Default.LocationOn,
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    StatusGridItem(
                        modifier = Modifier.weight(1f),
                        title = tr("Pil Tasarruf", "Battery Opt"),
                        isOk = deviceStatus.isBatteryOptimizationIgnored,
                        icon = Icons.Default.BatteryFull,
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatusGridItem(
                        modifier = Modifier.weight(1f),
                        title = tr("İnternet", "Internet"),
                        isOk = deviceStatus.isInternetConnected,
                        icon = if (deviceStatus.isInternetConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        onClick = { viewModel.toggleInternetSimulation() }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    StatusGridItem(
                        modifier = Modifier.weight(1f),
                        title = tr("GPS", "GPS"),
                        isOk = deviceStatus.isGpsEnabled,
                        icon = if (deviceStatus.isGpsEnabled) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                        onClick = { viewModel.toggleGpsSimulation() }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    StatusGridItem(
                        modifier = Modifier.weight(1f),
                        title = tr("Arka Plan", "Background"),
                        isOk = deviceStatus.isBackgroundLocationGranted,
                        icon = Icons.Default.LocationOn,
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    StatusGridItem(
                        modifier = Modifier.weight(1f),
                        title = tr("Pil Tasarruf", "Battery Opt"),
                        isOk = deviceStatus.isBatteryOptimizationIgnored,
                        icon = Icons.Default.BatteryFull,
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                    // Background placeholder for map
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

            Spacer(modifier = Modifier.height(24.dp))

            // BATTERY & SYNC SUMMARY
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        
                        Button(
                            onClick = { viewModel.triggerOfflineSync() },
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
                viewModel.logout()
                showProfileModal = false
            }
        )
    }
}


@Composable
fun ProfileInfoModal(
    user: com.example.data.local.entity.UserProfileEntity?,
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
                ProfileDetailRow(Icons.Default.Badge, tr("Pozisyon", "Position"), user?.position ?: "-")
                ProfileDetailRow(Icons.Default.Transgender, tr("Cinsiyet", "Gender"), user?.gender ?: "-")
                
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
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOk) MaterialTheme.colorScheme.surface else StatusRed.copy(alpha = 0.05f)
        ),
        border = if (!isOk) BorderStroke(1.dp, StatusRed.copy(alpha = 0.2f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isOk) StatusGreen else StatusRed,
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
                        .background(if (isOk) StatusGreen else StatusRed, CircleShape)
                )
            }
        }
    }
}
