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
import com.example.ui.components.CustomMapView
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.viewmodel.MainViewModel

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
                            text = "24s Rota Takibi: ${locations.size} Konum Noktası",
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
                                text = "Güzergah Oynatma (Playback)",
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
                                text = "İlerleme: %${(playbackState.progress * 100).toInt()}",
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
                                Text(if (playbackState.isPlaying) "Duraklat" else "Oynat")
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
                                    text = "Bölge Tanımlama (Geofencing)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { showAddGeofenceDialog = true }) {
                                Icon(Icons.Default.AddLocation, contentDescription = "Bölge Ekle")
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
                                        text = "Merkez: ${String.format("%.4f", zone.centerLat)}, ${String.format("%.4f", zone.centerLng)} • Yarıçap: ${zone.radiusMeters.toInt()}m",
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
        var zoneName by remember { mutableStateOf("Yeni Güvenli Saha Bölgesi") }
        var radiusStr by remember { mutableStateOf("600") }

        AlertDialog(
            onDismissRequest = { showAddGeofenceDialog = false },
            title = { Text("Yeni Safe Zone (Bölge) Tanımla") },
            text = {
                Column {
                    OutlinedTextField(
                        value = zoneName,
                        onValueChange = { zoneName = it },
                        label = { Text("Bölge Adı") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = radiusStr,
                        onValueChange = { radiusStr = it },
                        label = { Text("İhlal Yarıçapı (Metre)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Merkez nokta olarak en son kaydedilen konum alınacaktır.",
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
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGeofenceDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}
