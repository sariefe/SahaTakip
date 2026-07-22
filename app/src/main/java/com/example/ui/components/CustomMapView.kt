package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.data.local.entity.LocationEntity
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed

@Composable
fun CustomMapView(
    locations: List<LocationEntity>,
    currentLocation: LocationEntity?,
    playbackLocation: LocationEntity?,
    geofences: List<GeofenceZoneEntity>,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val activeLocation = playbackLocation ?: currentLocation ?: locations.lastOrNull()

    val mapBgColor = if (MaterialTheme.colorScheme.background.red < 0.5f) Color(0xFF0F172A) else Color(0xFFE2E8F0)
    val gridColor = if (MaterialTheme.colorScheme.background.red < 0.5f) Color(0xFF1E293B) else Color(0xFFCBD5E1)
    val roadColor = if (MaterialTheme.colorScheme.background.red < 0.5f) Color(0xFF334155) else Color(0xFFFFFFFF)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(mapBgColor)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 4f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate bounding box for Lat/Lng mapping
            val minLat = (locations.minOfOrNull { it.latitude } ?: 41.000).coerceAtMost(41.000)
            val maxLat = (locations.maxOfOrNull { it.latitude } ?: 41.020).coerceAtLeast(41.020)
            val minLng = (locations.minOfOrNull { it.longitude } ?: 28.970).coerceAtMost(28.970)
            val maxLng = (locations.maxOfOrNull { it.longitude } ?: 28.995).coerceAtLeast(28.995)

            val latSpan = (maxLat - minLat).coerceAtLeast(0.005)
            val lngSpan = (maxLng - minLng).coerceAtLeast(0.005)

            fun mapToCanvas(lat: Double, lng: Double): Offset {
                val normalizedX = ((lng - minLng) / lngSpan).toFloat()
                val normalizedY = (1f - ((lat - minLat) / latSpan).toFloat()) // Invert Y
                val padding = 100f
                val x = padding + normalizedX * (canvasWidth - 2 * padding)
                val y = padding + normalizedY * (canvasHeight - 2 * padding)

                // Apply zoom & pan transformations
                val centerX = canvasWidth / 2f
                val centerY = canvasHeight / 2f
                val transformedX = (x - centerX) * scale + centerX + offsetX
                val transformedY = (y - centerY) * scale + centerY + offsetY

                return Offset(transformedX, transformedY)
            }

            // 1. Draw Grid Lines & Simulated Roads
            val step = (80f * scale).coerceAtLeast(20f)
            var xGrid = (offsetX % step)
            while (xGrid < canvasWidth) {
                drawLine(gridColor, Offset(xGrid, 0f), Offset(xGrid, canvasHeight), strokeWidth = 1f * scale)
                xGrid += step
            }
            var yGrid = (offsetY % step)
            while (yGrid < canvasHeight) {
                drawLine(gridColor, Offset(0f, yGrid), Offset(canvasWidth, yGrid), strokeWidth = 1f * scale)
                yGrid += step
            }

            // Draw major road grid
            val mainRoad1 = Path().apply {
                moveTo(0f, canvasHeight * 0.4f + offsetY)
                lineTo(canvasWidth, canvasHeight * 0.45f + offsetY)
            }
            drawPath(mainRoad1, roadColor, style = Stroke(width = 18f * scale))

            val mainRoad2 = Path().apply {
                moveTo(canvasWidth * 0.35f + offsetX, 0f)
                lineTo(canvasWidth * 0.38f + offsetX, canvasHeight)
            }
            drawPath(mainRoad2, roadColor, style = Stroke(width = 14f * scale))

            // 2. Draw Geofence Safe Zones (Circles with radius & semi-transparent fill)
            geofences.forEach { zone ->
                if (zone.isActive) {
                    val centerOffset = mapToCanvas(zone.centerLat, zone.centerLng)
                    val radiusPx = (zone.radiusMeters / 10.0).toFloat() * scale

                    // Outer circle boundary
                    drawCircle(
                        color = StatusGreen.copy(alpha = 0.18f),
                        radius = radiusPx,
                        center = centerOffset
                    )
                    drawCircle(
                        color = StatusGreen,
                        radius = radiusPx,
                        center = centerOffset,
                        style = Stroke(width = 2.5f * scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                    )
                    // Geofence center marker
                    drawCircle(StatusGreen, radius = 5f * scale, center = centerOffset)
                }
            }

            // 3. Draw 24-Hour Route Track Polyline
            if (locations.size > 1) {
                val routePath = Path()
                locations.forEachIndexed { i, loc ->
                    val point = mapToCanvas(loc.latitude, loc.longitude)
                    if (i == 0) routePath.moveTo(point.x, point.y)
                    else routePath.lineTo(point.x, point.y)
                }

                // Outer route glow
                drawPath(
                    routePath,
                    StatusBlue.copy(alpha = 0.3f),
                    style = Stroke(width = 10f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Solid route line
                drawPath(
                    routePath,
                    StatusBlue,
                    style = Stroke(width = 4f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw route waypoint dots
                locations.forEach { loc ->
                    val pt = mapToCanvas(loc.latitude, loc.longitude)
                    drawCircle(Color.White, radius = 3.5f * scale, center = pt)
                    drawCircle(StatusBlue, radius = 2f * scale, center = pt)
                }
            }

            // 4. Draw Active Marker (Current / Playback location)
            activeLocation?.let { loc ->
                val markerOffset = mapToCanvas(loc.latitude, loc.longitude)

                // Pulsing radar ripple around user marker
                drawCircle(
                    color = StatusAmber.copy(alpha = 0.25f),
                    radius = 24f * scale,
                    center = markerOffset
                )
                drawCircle(
                    color = StatusAmber.copy(alpha = 0.5f),
                    radius = 16f * scale,
                    center = markerOffset
                )
                // Inner solid pin dot
                drawCircle(
                    color = StatusAmber,
                    radius = 8f * scale,
                    center = markerOffset
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f * scale,
                    center = markerOffset
                )
            }
        }

        // Map Scale / Zoom Legend overlay
        Text(
            text = "GPS Canlı Harita • Ölçek: ${(scale * 100).toInt()}% • Dokunarak Kaydır / Büyüt",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        )
    }
}
