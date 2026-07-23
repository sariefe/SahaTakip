package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.LocationEntity
import com.example.ui.components.CustomMapView
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.viewmodel.MainViewModel
import com.example.util.tr
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MapTrackingScreen(
    viewModel: MainViewModel
) {
    val locations by viewModel.allLocations.collectAsState()
    val latestLoc by viewModel.latestLocation.collectAsState()
    val geofences by viewModel.allGeofences.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    var showAddGeofenceDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Interactive Map Area (Takes top 55% of screen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                CustomMapView(
                    locations = locations,
                    currentLocation = latestLoc,
                    playbackLocation = playbackState.currentLocation,
                    geofences = geofences
                )

                // Top Overlay Status Badge
                Card(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(StatusGreen, shape = RoundedCornerShape(5.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${tr("24s Rota Takibi", "24h Route Tracking")}: ${locations.size} ${tr("Konum Noktası", "Location Points")}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Panel: Route Playback & Geofence Controls (Scrollable)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // TELEMETRY & LIVE METRICS CARD
                val totalDistKm = remember(locations) { calculateTotalDistanceKm(locations) }
                val avgSpeed = remember(locations) {
                    if (locations.isNotEmpty()) locations.map { it.speed }.average() else 0.0
                }

                val activeGeofenceViolation = remember(latestLoc, geofences) {
                    val current = latestLoc ?: return@remember false
                    geofences.filter { it.isActive }.any { zone ->
                        val dLat = Math.toRadians(zone.centerLat - current.latitude)
                        val dLng = Math.toRadians(zone.centerLng - current.longitude)
                        val lat1 = Math.toRadians(current.latitude)
                        val lat2 = Math.toRadians(zone.centerLat)
                        val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
                        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                        val distanceMeters = 6371000.0 * c
                        distanceMeters > zone.radiusMeters
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = tr("24s Toplam Mesafe", "24h Total Distance"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format("%.2f", totalDistKm)} km",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column {
                            Text(
                                text = tr("Ortalama Hız", "Average Speed"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${avgSpeed.toInt()} km/h",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = tr("Safe Zone Durumu", "Safe Zone Status"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (activeGeofenceViolation) StatusAmber.copy(alpha = 0.2f) else StatusGreen.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (activeGeofenceViolation) tr("BÖLGE DIŞI", "OUTSIDE ZONE") else tr("GÜVENLİ", "SAFE"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeGeofenceViolation) StatusAmber else StatusGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ROUTE PLAYBACK CONTROLS CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr("Güzergah Oynatma (Playback)", "Route Playback"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = playbackState.speedMultiplier == 1f,
                                    onClick = { viewModel.setPlaybackSpeed(1f) },
                                    label = { Text("1x") }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                FilterChip(
                                    selected = playbackState.speedMultiplier == 2f,
                                    onClick = { viewModel.setPlaybackSpeed(2f) },
                                    label = { Text("2x") }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                FilterChip(
                                    selected = playbackState.speedMultiplier == 4f,
                                    onClick = { viewModel.setPlaybackSpeed(4f) },
                                    label = { Text("4x") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress Slider
                        Slider(
                            value = playbackState.progress,
                            onValueChange = { viewModel.seekPlaybackProgress(it) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${tr("İlerleme", "Progress")}: %${(playbackState.progress * 100).toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    if (playbackState.isPlaying) viewModel.pauseRoutePlayback()
                                    else viewModel.startRoutePlayback()
                                }
                            ) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (playbackState.isPlaying) tr("Duraklat", "Pause") else tr("Oynat", "Play"))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // GEOFENCE SAFE ZONES LIST & ADD BUTTON
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = StatusGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tr("Bölge Tanımlama (Geofencing)", "Geofencing Zone Management"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { showAddGeofenceDialog = true }) {
                                Icon(Icons.Default.AddLocation, contentDescription = tr("Bölge Ekle", "Add Zone"))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        geofences.forEach { zone ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = zone.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${tr("Merkez", "Center")}: ${String.format("%.4f", zone.centerLat)}, ${String.format("%.4f", zone.centerLng)} • ${tr("Yarıçap", "Radius")}: ${zone.radiusMeters.toInt()}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = zone.isActive,
                                    onCheckedChange = { active -> viewModel.toggleGeofenceActive(zone.id, active) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ADD GEOFENCE DIALOG
    if (showAddGeofenceDialog) {
        val defaultZoneName = tr("Yeni Güvenli Saha Bölgesi", "New Safe Zone")
        var zoneName by remember { mutableStateOf(defaultZoneName) }
        var radiusStr by remember { mutableStateOf("600") }

        AlertDialog(
            onDismissRequest = { showAddGeofenceDialog = false },
            title = { Text(tr("Yeni Safe Zone (Bölge) Tanımla", "Define New Safe Zone")) },
            text = {
                Column {
                    OutlinedTextField(
                        value = zoneName,
                        onValueChange = { zoneName = it },
                        label = { Text(tr("Bölge Adı", "Zone Name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = radiusStr,
                        onValueChange = { radiusStr = it },
                        label = { Text(tr("İhlal Yarıçapı (Metre)", "Violation Radius (Meters)")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = tr("Merkez nokta olarak en son kaydedilen konum alınacaktır.", "Last recorded location will be set as center point."),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = latestLoc?.latitude ?: 41.0082
                        val lng = latestLoc?.longitude ?: 28.9784
                        val rad = radiusStr.toDoubleOrNull() ?: 500.0
                        viewModel.addGeofenceZone(zoneName, lat, lng, rad)
                        showAddGeofenceDialog = false
                    }
                ) {
                    Text(tr("Kaydet", "Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGeofenceDialog = false }) {
                    Text(tr("İptal", "Cancel"))
                }
            }
        )
    }
}

fun calculateTotalDistanceKm(locations: List<LocationEntity>): Double {
    if (locations.size < 2) return 0.0
    var totalMeters = 0.0
    for (i in 0 until locations.size - 1) {
        val l1 = locations[i]
        val l2 = locations[i + 1]
        val lat1 = Math.toRadians(l1.latitude)
        val lat2 = Math.toRadians(l2.latitude)
        val dLat = Math.toRadians(l2.latitude - l1.latitude)
        val dLng = Math.toRadians(l2.longitude - l1.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        totalMeters += 6371000.0 * c
    }
    return totalMeters / 1000.0
}


