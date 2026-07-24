package com.example.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

sealed class Screen(
    val route: String,
    val titleTr: String,
    val titleEn: String,
    @StringRes val stringResId: Int? = null,
    val icon: ImageVector? = null
) {
    object Auth : Screen("auth", "Kimlik Kayıt", "Identity Reg", R.string.auth_title)
    object BiometricLock : Screen("biometric_lock", "Biyometrik Giriş", "Biometric Login", R.string.biometric_title)
    object Dashboard : Screen("dashboard", "Durum Paneli", "Dashboard", R.string.nav_dashboard, Icons.Default.Dashboard)
    object TrackingMap : Screen("tracking_map", "Konum & Rota", "Tracking & Map", R.string.nav_map, Icons.Default.Map)
    object EventLogs : Screen("event_logs", "Olay Kayıtları", "Event Logs", R.string.nav_logs, Icons.Default.History)
    object LeaveRequests : Screen("leave_requests", "İzin Talepleri", "Leave Requests", R.string.nav_leave, Icons.AutoMirrored.Filled.Assignment)
    object Settings : Screen("settings", "Ayarlar", "Settings", R.string.nav_settings, Icons.Default.Settings)
}

val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.TrackingMap,
    Screen.EventLogs,
    Screen.LeaveRequests,
    Screen.Settings
)
