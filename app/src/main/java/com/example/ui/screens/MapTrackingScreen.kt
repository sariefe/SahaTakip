package com.example.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.LocationEntity
import com.example.ui.components.CustomMapView
import com.example.ui.theme.StatusGreen
import com.example.ui.viewmodel.TrackingViewModel
import com.example.util.tr
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapTrackingScreen(
    viewModel: TrackingViewModel,
    windowWidthSizeClass: WindowWidthSizeClass,
) {
    val locations by viewModel.locationsLast24h.collectAsStateWithLifecycle()
    val latestLoc by viewModel.latestLocation.collectAsStateWithLifecycle()
    val geofences by viewModel.allGeofences.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    var showAddGeofenceDialog by remember { mutableStateOf(value = false) }
    var geofenceToDelete by remember { mutableStateOf<Long?>(null) }
    var geofenceToEdit by remember { mutableStateOf<com.example.data.local.entity.GeofenceZoneEntity?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Interactive Map Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f)
                ) {
                    MapArea(locations, latestLoc, geofences, playbackState)
                }

                // Bottom Panel: Telemetry & Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    MapControls(
                        viewModel = viewModel,
                        locations = locations,
                        playbackState = playbackState,
                        geofences = geofences,
                        onAddGeofence = { showAddGeofenceDialog = true },
                        onDeleteGeofence = { geofenceToDelete = it }
                    ) { geofenceToEdit = it }
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                ) {
                    MapArea(locations, latestLoc, geofences, playbackState)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    MapControls(
                        viewModel = viewModel,
                        locations = locations,
                        playbackState = playbackState,
                        geofences = geofences,
                        onAddGeofence = { showAddGeofenceDialog = true },
                        onDeleteGeofence = { geofenceToDelete = it }
                    ) { geofenceToEdit = it }
                }
            }
        }
    }

    if (geofenceToDelete != null) {
        AlertDialog(
            onDismissRequest = { geofenceToDelete = null },
            shape = RoundedCornerShape(24.dp),
            title = { Text(tr("Bölgeyi Sil", "Delete Zone"), fontWeight = FontWeight.Bold) },
            text = { Text(tr("Bu güvenli bölgeyi silmek istediğinize emin misiniz?", "Are you sure you want to delete this safe zone?")) },
            confirmButton = {
                Button(
                    onClick = {
                        geofenceToDelete?.let { viewModel.deleteGeofence(it) }
                        geofenceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.StatusRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(tr("Evet, Sil", "Yes, Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { geofenceToDelete = null }) {
                    Text(tr("İptal", "Cancel"))
                }
            }
        )
    }

    if (showAddGeofenceDialog) {
        AddGeofenceDialog(
            onDismiss = { showAddGeofenceDialog = false }
        ) { name, radius ->
            val lat = latestLoc?.latitude ?: 41.0082
            val lng = latestLoc?.longitude ?: 28.9784
            viewModel.addGeofenceZone(name, lat, lng, radius)
            showAddGeofenceDialog = false
        }
    }

    if (geofenceToEdit != null) {
        AddGeofenceDialog(
            isEdit = true,
            initialName = geofenceToEdit?.name ?: "",
            initialRadius = geofenceToEdit?.radiusMeters?.toInt()?.toString() ?: "500",
            onDismiss = { geofenceToEdit = null },
            onConfirm = { name, radius ->
                geofenceToEdit?.let {
                    viewModel.updateGeofenceZone(it.id, name, radius)
                }
                geofenceToEdit = null
            }
        )
    }
}

@Composable
private fun MapArea(
    locations: List<LocationEntity>,
    latestLoc: LocationEntity?,
    geofences: List<com.example.data.local.entity.GeofenceZoneEntity>,
    playbackState: com.example.ui.viewmodel.PlaybackState,
) {
    CustomMapView(
        locations = locations,
        currentLocation = latestLoc,
        playbackLocation = playbackState.currentLocation,
        geofences = geofences
    )

    // Premium Top Badge
    Card(
        modifier = Modifier
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(StatusGreen, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${tr("Rota Takibi", "Route Tracking")}: ${locations.size} pts",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun MapControls(
    viewModel: TrackingViewModel,
    locations: List<LocationEntity>,
    playbackState: com.example.ui.viewmodel.PlaybackState,
    geofences: List<com.example.data.local.entity.GeofenceZoneEntity>,
    onAddGeofence: () -> Unit,
    onDeleteGeofence: (Long) -> Unit,
    onEditGeofence: (com.example.data.local.entity.GeofenceZoneEntity) -> Unit
) {
    // QUICK TELEMETRY ROW
    val totalDistKm = remember(locations) { calculateTotalDistanceKm(locations) }
    val avgSpeed = remember(locations) {
        if (locations.isNotEmpty()) locations.asSequence().map { it.speed }.average() else 0.0
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TelemetryCard(
            modifier = Modifier.weight(1f),
            label = tr("Mesafe", "Distance"),
            value = "${String.format("%.1f", totalDistKm)} km",
            icon = Icons.Default.Route,
            color = MaterialTheme.colorScheme.primary
        )
        TelemetryCard(
            modifier = Modifier.weight(1f),
            label = tr("Ort. Hız", "Avg Speed"),
            value = "${avgSpeed.toInt()} km/h",
            icon = Icons.Default.Speed,
            color = MaterialTheme.colorScheme.secondary
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // COMPACT PLAYBACK CONTROLS
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    tr("Güzergah Oynat", "Route Playback"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf(1, 2, 4).forEach { multiplier ->
                        val isSelected = playbackState.speedMultiplier == multiplier.toFloat()
                        Surface(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clickable { viewModel.setPlaybackSpeed(multiplier.toFloat()) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Text(
                                text = "${multiplier}x",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

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
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                IconButton(
                    onClick = {
                        if (playbackState.isPlaying) viewModel.pauseRoutePlayback()
                        else viewModel.startRoutePlayback()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // GEOFENCE SECTION
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tr("Güvenli Bölgeler", "Safe Zones"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onAddGeofence) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(tr("Bölge Ekle", "Add Zone"), style = MaterialTheme.typography.labelLarge)
        }
    }

    geofences.forEach { zone ->
        GeofenceItem(
            zoneName = zone.name,
            details = "${tr("Yarıçap", "Radius")}: ${zone.radiusMeters.toInt()}m",
            isActive = zone.isActive,
            onToggle = { active -> viewModel.toggleGeofenceActive(zone.id, active) },
            onDelete = { onDeleteGeofence(zone.id) },
            onEdit = { onEditGeofence(zone) }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun TelemetryCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GeofenceItem(
    zoneName: String,
    details: String,
    isActive: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isActive) StatusGreen.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = if (isActive) StatusGreen else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = zoneName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(text = details, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }

            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = StatusGreen)
            )
        }
    }
}

@Composable
fun AddGeofenceDialog(
    isEdit: Boolean = false,
    initialName: String = "",
    initialRadius: String = "500",
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var radius by remember { mutableStateOf(initialRadius) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { 
            Text(
                if (isEdit) tr("Bölgeyi Düzenle", "Edit Safe Zone") else tr("Yeni Güvenli Bölge", "New Safe Zone"), 
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 25) name = it },
                    label = { Text(tr("Bölge İsmi", "Zone Name")) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = radius,
                    onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 6) radius = it },
                    label = { Text(tr("Yarıçap (Metre)", "Radius (Meters)")) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    maxLines = 1,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, radius.toDoubleOrNull() ?: 500.0) }) {
                Text(if (isEdit) tr("Güncelle", "Update") else tr("Ekle", "Add"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("İptal", "Cancel"))
            }
        }
    )
}

fun calculateTotalDistanceKm(locations: List<LocationEntity>): Double {
    if (locations.size < 2) return 0.0
    var totalMeters = 0.0
    for (i in 0 until (locations.size - 1)) {
        val l1 = locations[i]
        val l2 = locations[i + 1]
        val lat1 = Math.toRadians(l1.latitude)
        val lat2 = Math.toRadians(l2.latitude)
        val dLat = Math.toRadians(l2.latitude - l1.latitude)
        val dLng = Math.toRadians(l2.longitude - l1.longitude)
        val a = (sin(dLat / 2) * sin(dLat / 2)) + (cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        totalMeters += 6371000.0 * c
    }
    return totalMeters / 1000.0
}
