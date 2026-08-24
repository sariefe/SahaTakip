package com.sahatakip.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.sahatakip.data.local.entity.GeofenceZoneEntity
import com.sahatakip.data.local.entity.LocationEntity
import com.sahatakip.ui.theme.StatusBlue
import com.sahatakip.ui.theme.StatusGreen
import com.sahatakip.util.LocationUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.RoundCap
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
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Text("Map Placeholder", color = Color.White)
        }
        return
    }

    val activeLocation = playbackLocation ?: currentLocation ?: locations.lastOrNull()

    var isFollowModeActive by remember { mutableStateOf(value = true) }
    val cameraPositionState = rememberCameraPositionState {
        val initialLocation = activeLocation ?: locations.lastOrNull()
        position = if (initialLocation != null) {
            CameraPosition.fromLatLngZoom(LatLng(initialLocation.latitude, initialLocation.longitude), 17f)
        } else {
            CameraPosition.fromLatLngZoom(LatLng(41.0082, 28.9784), 12f)
        }
    }

    var mapType by remember { mutableStateOf(MapType.NORMAL) }

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
            val routeSegments = remember(locations) {
                val validPoints = locations.filter { 
                    it.latitude != 0.0 && it.longitude != 0.0 && it.accuracy < 35f 
                }
                
                val segments = mutableListOf<List<LatLng>>()
                if (validPoints.isNotEmpty()) {
                    var currentSegment = mutableListOf<LatLng>()
                    for (i in validPoints.indices) {
                        val point = validPoints[i]
                        val latLng = LatLng(point.latitude, point.longitude)
                        
                        if (currentSegment.isEmpty()) {
                            currentSegment.add(latLng)
                        } else {
                            val prevPoint = validPoints[i - 1]
                            val distance = LocationUtils.calculateDistanceInMeters(
                                prevPoint.latitude, prevPoint.longitude,
                                point.latitude, point.longitude
                            )
                            val timeDiff = point.timestamp - prevPoint.timestamp
                            
                            if (distance > 1000.0 || timeDiff > 15 * 60 * 1000L) {
                                segments.add(currentSegment)
                                currentSegment = mutableListOf(latLng)
                            } else {
                                currentSegment.add(latLng)
                            }
                        }
                    }
                    if (currentSegment.isNotEmpty()) segments.add(currentSegment)
                }
                segments
            }
            val pattern = listOf(Gap(10f), Dash(20f))

            routeSegments.forEach { segment ->
                if (segment.size > 1) {
                    Polyline(
                        points = segment,
                        color = StatusBlue.copy(alpha = 0.8f),
                        width = 12f,
                        jointType = JointType.ROUND,
                        startCap = RoundCap(),
                        endCap = RoundCap()
                    )
                    
                    Polyline(
                        points = segment,
                        color = Color.White.copy(alpha = 0.4f),
                        width = 4f,
                        pattern = pattern,
                        jointType = JointType.ROUND
                    )
                }
            }

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

            locations.forEachIndexed { index, loc ->
                if ((index % 15 == 0) || (index == (locations.size - 1))) {
                    Marker(
                        state = rememberUpdatedMarkerState(position = LatLng(loc.latitude, loc.longitude)),
                        title = "Point ${index + 1}",
                        alpha = 0.5f
                    )
                }
            }

            activeLocation?.let { loc ->
                val rotation = remember(loc, locations) {
                    calculateRotation(loc, locations)
                }

                Marker(
                    state = rememberUpdatedMarkerState(position = LatLng(loc.latitude, loc.longitude)),
                    title = if (playbackLocation != null) "Oynatma" else "Canlı Konum",
                    snippet = loc.address,
                    rotation = rotation,
                    anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
                )
            }
            if (locations.isNotEmpty()) {
                val start = locations.first()
                val end = locations.last()
                
                Marker(
                    state = rememberUpdatedMarkerState(position = LatLng(start.latitude, start.longitude)),
                    title = "Başlangıç Noktası",
                    alpha = 0.7f
                )

                if (activeLocation != end) {
                    Marker(
                        state = rememberUpdatedMarkerState(position = LatLng(end.latitude, end.longitude)),
                        title = "Bitiş Noktası",
                        alpha = 0.7f
                    )
                }
            }
        }

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

private fun calculateRotation(current: LocationEntity, allLocations: List<LocationEntity>): Float {
    if (allLocations.size < 2) return 0f
    
    val currentIndex = allLocations.indexOf(current)
    if (currentIndex <= 0) return 0f
    
    val prev = allLocations[currentIndex - 1]
    
    val lat1 = Math.toRadians(prev.latitude)
    val lon1 = Math.toRadians(prev.longitude)
    val lat2 = Math.toRadians(current.latitude)
    val lon2 = Math.toRadians(current.longitude)
    
    val dLon = lon2 - lon1
    val y = kotlin.math.sin(dLon) * kotlin.math.cos(lat2)
    val x = kotlin.math.cos(lat1) * kotlin.math.sin(lat2) -
            kotlin.math.sin(lat1) * kotlin.math.cos(lat2) * kotlin.math.cos(dLon)
    
    val bearing = Math.toDegrees(kotlin.math.atan2(y, x))
    return bearing.toFloat()
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
