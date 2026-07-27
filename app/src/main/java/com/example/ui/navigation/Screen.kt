package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val titleTr: String,
    val titleEn: String,
    val icon: ImageVector? = null
) {
    object Auth : Screen("auth", "Kimlik Kayıt", "Identity Reg")
    object BiometricLock : Screen("biometric_lock", "Biyometrik Giriş", "Biometric Login")
    object Dashboard : Screen("dashboard", "Durum Paneli", "Dashboard", Icons.Default.Dashboard)
    object TrackingMap : Screen("tracking_map", "Konum", "Map", Icons.Default.Map)
    object EventLogs : Screen("event_logs", "Olay Kayıtları", "Event Logs", Icons.Default.History)
    object LeaveRequests : Screen("leave_requests", "İzin Talebi", "Leave Req", Icons.AutoMirrored.Filled.Assignment)
    object Settings : Screen("settings", "Ayarlar", "Settings", Icons.Default.Settings)
}

val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.TrackingMap,
    Screen.EventLogs,
    Screen.LeaveRequests,
    Screen.Settings
)
