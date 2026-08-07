package com.sahatakip.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sahatakip.data.local.entity.GeofenceZoneEntity
import com.sahatakip.data.local.entity.LocationEntity
import com.sahatakip.ui.theme.StatusBlue
import com.sahatakip.ui.theme.StatusGreen
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

@Composable
fun CustomMapView(
    locations: List<LocationEntity>,
    currentLocation: LocationEntity?,
    playbackLocation: LocationEntity?,
    geofences: List<GeofenceZoneEntity>,
    modifier: Modifier = Modifier,
) {
    val activeLocation = playbackLocation ?: currentLocation ?: locations.lastOrNull()

    var isFollowModeActive by remember { mutableStateOf(value = true) }
    val cameraPositionState = rememberCameraPositionState {
        val initialLocation = activeLocation ?: locations.lastOrNull()
        position = if (initialLocation != null) {
            CameraPosition.fromLatLngZoom(LatLng(initialLocation.latitude, initialLocation.longitude), 17f)
        } else {
            CameraPosition.fromLatLngZoom(LatLng(41.0082, 28.9784), 12f) // Istanbul fallback
        }
    }

    var mapType by remember { mutableStateOf(MapType.NORMAL) }

    // Disable follow mode if user moves the map manually
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving && 
            (cameraPositionState.cameraMoveStartedReason == com.google.maps.android.compose.CameraMoveStartedReason.GESTURE)) {
            isFollowModeActive = false
        }
    }

    LaunchedEffect(activeLocation, isFollowModeActive) {
        if (isFollowModeActive) {
            activeLocation?.let {
                val cameraPosition = CameraPosition.builder()
                    .target(LatLng(it.latitude, it.longitude))
                    .zoom(17f)
                    .tilt(0f)
                    .build()
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(cameraPosition),
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = mapType,
                isBuildingEnabled = true,
                isIndoorEnabled = true,
                isMyLocationEnabled = false,
                isTrafficEnabled = false,
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false
            )
        ) {
            // Draw Route History
            if (locations.size > 1) {
                Polyline(
                    points = locations.map { LatLng(it.latitude, it.longitude) },
                    color = StatusBlue,
                    width = 10f
                )
            }

            // Draw Geofences
            geofences.forEach { zone ->
                if (zone.isActive) {
                    androidx.compose.runtime.key(zone.id) {
                        Circle(
                            center = LatLng(zone.centerLat, zone.centerLng),
                            radius = zone.radiusMeters,
                            fillColor = StatusGreen.copy(alpha = 0.35f),
                            strokeColor = StatusGreen,
                            strokeWidth = 5f,
                            zIndex = 1f
                        )
                        Marker(
                            state = rememberUpdatedMarkerState(position = LatLng(zone.centerLat, zone.centerLng)),
                            title = zone.name,
                            snippet = "${zone.radiusMeters.toInt()}m",
                            alpha = 0.6f
                        )
                    }
                }
            }

            // Draw Markers for History Points (Sparsely)
            locations.forEachIndexed { index, loc ->
                if ((index % 15 == 0) || (index == (locations.size - 1))) {
                    Marker(
                        state = rememberUpdatedMarkerState(position = LatLng(loc.latitude, loc.longitude)),
                        title = "Point ${index + 1}",
                        alpha = 0.5f
                    )
                }
            }

            // Active Marker (Live or Playback)
            activeLocation?.let { loc ->
                Marker(
                    state = rememberUpdatedMarkerState(position = LatLng(loc.latitude, loc.longitude)),
                    title = if (playbackLocation != null) "Playback" else "Live Location",
                    snippet = loc.address
                )
            }
        }

        // Map Control Overlays
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapControlButton(icon = Icons.Default.Layers) {
                mapType = when (mapType) {
                    MapType.NORMAL -> MapType.HYBRID
                    MapType.HYBRID -> MapType.TERRAIN
                    else -> MapType.NORMAL
                }
            }
            
            MapControlButton(
                icon = Icons.Default.MyLocation,
                iconColor = if (isFollowModeActive) StatusGreen else Color.Gray
            ) {
                isFollowModeActive = true
                activeLocation?.let {
                    val cameraPosition = CameraPosition.builder()
                        .target(LatLng(it.latitude, it.longitude))
                        .zoom(17f)
                        .tilt(0f)
                        .build()
                    cameraPositionState.position = cameraPosition
                }
            }
        }
    }
}

@Composable
fun MapControlButton(
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
