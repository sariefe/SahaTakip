package com.example.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.data.local.entity.LocationEntity
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.util.tr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class MapLayerMode {
    VECTOR, SATELLITE, HEATMAP
}

data class PoiStation(
    val name: String,
    val lat: Double,
    val lng: Double,
    val category: String
)

@SuppressLint("DefaultLocale")
@Composable
fun CustomMapView(
    locations: List<LocationEntity>,
    currentLocation: LocationEntity?,
    playbackLocation: LocationEntity?,
    geofences: List<GeofenceZoneEntity>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()

        var scale by rememberSaveable { mutableFloatStateOf(1f) }
        var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
        var offsetY by rememberSaveable { mutableFloatStateOf(0f) }

        var selectedLayer by rememberSaveable { mutableStateOf(MapLayerMode.VECTOR) }
        var selectedLocation by remember { mutableStateOf<LocationEntity?>(null) }
        var hasCenteredOnce by rememberSaveable { mutableStateOf(false) }

        val activeLocation = playbackLocation ?: currentLocation ?: locations.lastOrNull()

        val allPointsLat = remember(locations, currentLocation, geofences) {
            // Sadece aktif ve merkeze yakın noktaları baz alarak harita sınırlarını belirleyelim
            // Çok uzaktaki geofenceler (örn: farklı şehirdeki) harita projeksiyonunu bozmasın
            val baseLat = currentLocation?.latitude ?: locations.lastOrNull()?.latitude ?: 41.0082
            val list = (locations.map { it.latitude } + 
                       (currentLocation?.latitude?.let { listOf(it) } ?: emptyList()) + 
                       geofences.map { it.centerLat }).filter { abs(it - baseLat) < 0.5 } // ~50km filtre
            list.ifEmpty { listOf(41.0082) }
        }
        val allPointsLng = remember(locations, currentLocation, geofences) {
            val baseLng = currentLocation?.longitude ?: locations.lastOrNull()?.longitude ?: 28.9784
            val list = (locations.map { it.longitude } + 
                       (currentLocation?.longitude?.let { listOf(it) } ?: emptyList()) + 
                       geofences.map { it.centerLng }).filter { abs(it - baseLng) < 0.5 }
            list.ifEmpty { listOf(28.9784) }
        }

        val minLat = (allPointsLat.minOrNull() ?: 41.000) - 0.005
        val maxLat = (allPointsLat.maxOrNull() ?: 41.020) + 0.005
        val minLng = (allPointsLng.minOrNull() ?: 28.970) - 0.005
        val maxLng = (allPointsLng.maxOrNull() ?: 28.995) + 0.005

        val latSpan = (maxLat - minLat).coerceAtLeast(0.001)
        val lngSpan = (maxLng - minLng).coerceAtLeast(0.001)

        val pixelsPerDegreeLat = (canvasHeight - 200f) / latSpan.toFloat()
        val metersPerDegreeLat = 111319.9f 

        fun mapToCanvas(lat: Double, lng: Double, currentScale: Float, currentOffsetX: Float, currentOffsetY: Float): Offset {
            val normalizedX = ((lng - minLng) / lngSpan).toFloat()
            val normalizedY = (1f - ((lat - minLat) / latSpan).toFloat())
            val padding = 100f
            val x = padding + normalizedX * (canvasWidth - 2 * padding)
            val y = padding + normalizedY * (canvasHeight - 2 * padding)

            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f
            val transformedX = (x - centerX) * currentScale + centerX + currentOffsetX
            val transformedY = (y - centerY) * currentScale + centerY + currentOffsetY

            return Offset(transformedX, transformedY)
        }

        fun centerOn(lat: Double, lng: Double) {
            val normalizedX = ((lng - minLng) / lngSpan).toFloat()
            val normalizedY = (1f - ((lat - minLat) / latSpan).toFloat())
            val padding = 100f
            val x = padding + normalizedX * (canvasWidth - 2 * padding)
            val y = padding + normalizedY * (canvasHeight - 2 * padding)
            
            scale = 2.0f
            offsetX = (canvasWidth / 2f - x) * scale
            offsetY = (canvasHeight / 2f - y) * scale
        }

        LaunchedEffect(activeLocation) {
            if (activeLocation != null && !hasCenteredOnce) {
                centerOn(activeLocation.latitude, activeLocation.longitude)
                hasCenteredOnce = true
            }
        }

        val poiStations = remember {
            listOf(
                PoiStation("Merkez Saha Deposu", 41.0125, 28.9810, "HQ"),
                PoiStation("Güvenlik Kontrol Noktası Alpha", 41.0060, 28.9740, "CHECKPOINT"),
                PoiStation("Tesis Araç Şarj İstasyonu", 41.0180, 28.9890, "STATION")
            )
        }

        val isDark = MaterialTheme.colorScheme.background.red < 0.5f
        val mapBgColor = when (selectedLayer) {
            MapLayerMode.VECTOR -> if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0)
            MapLayerMode.SATELLITE -> Color(0xFF09131D)
            MapLayerMode.HEATMAP -> Color(0xFF0B0F19)
        }

        val gridColor = when (selectedLayer) {
            MapLayerMode.VECTOR -> if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)
            MapLayerMode.SATELLITE -> Color(0xFF1E3A8A).copy(alpha = 0.3f)
            MapLayerMode.HEATMAP -> Color(0xFF1F2937)
        }

        val roadColor = when (selectedLayer) {
            MapLayerMode.VECTOR -> if (isDark) Color(0xFF334155) else Color(0xFFFFFFFF)
            MapLayerMode.SATELLITE -> Color(0xFF3B82F6).copy(alpha = 0.25f)
            MapLayerMode.HEATMAP -> Color(0xFF374151)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .background(mapBgColor)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 4.5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
                .pointerInput(locations, scale, offsetX, offsetY) {
                    detectTapGestures { tapOffset ->
                        var closestLoc: LocationEntity? = null
                        var minDistancePx = 50f * scale

                        locations.forEach { loc ->
                            val pt = mapToCanvas(loc.latitude, loc.longitude, scale, offsetX, offsetY)
                            val dx = tapOffset.x - pt.x
                            val dy = tapOffset.y - pt.y
                            val dist = sqrt(dx * dx + dy * dy)

                            if (dist < minDistancePx) {
                                minDistancePx = dist
                                closestLoc = loc
                            }
                        }

                        selectedLocation = closestLoc
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (selectedLayer == MapLayerMode.SATELLITE) {
                    drawCircle(
                        color = Color(0xFF1E3A1E).copy(alpha = 0.5f),
                        radius = 280f * scale,
                        center = Offset(canvasWidth * 0.3f + offsetX, canvasHeight * 0.4f + offsetY)
                    )
                    drawCircle(
                        color = Color(0xFF142B28).copy(alpha = 0.6f),
                        radius = 350f * scale,
                        center = Offset(canvasWidth * 0.7f + offsetX, canvasHeight * 0.7f + offsetY)
                    )
                }
                val step = (80f * scale).coerceAtLeast(25f)
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

                if (selectedLayer == MapLayerMode.HEATMAP) {
                    locations.forEach { loc ->
                        val pt = mapToCanvas(loc.latitude, loc.longitude, scale, offsetX, offsetY)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Red.copy(alpha = 0.6f),
                                    Color.Yellow.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                center = pt,
                                radius = 60f * scale
                            ),
                            radius = 60f * scale,
                            center = pt
                        )
                    }
                }

                geofences.forEach { zone ->
                    if (zone.isActive) {
                        val centerOffset = mapToCanvas(zone.centerLat, zone.centerLng, scale, offsetX, offsetY)
                        val radiusPx = (zone.radiusMeters.toFloat() / metersPerDegreeLat) * pixelsPerDegreeLat * scale
                        drawCircle(
                            color = StatusGreen.copy(alpha = 0.25f),
                            radius = radiusPx,
                            center = centerOffset
                        )
                        drawCircle(
                            color = StatusGreen,
                            radius = radiusPx,
                            center = centerOffset,
                            style = Stroke(
                                width = 3f * scale, 
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                            )
                        )

                        drawCircle(StatusGreen, radius = 6f * scale, center = centerOffset)
                        drawCircle(Color.White, radius = 2f * scale, center = centerOffset)
                    }
                }

                poiStations.forEach { poi ->
                    val poiPt = mapToCanvas(poi.lat, poi.lng, scale, offsetX, offsetY)
                    drawCircle(Color(0xFF38BDF8), radius = 8f * scale, center = poiPt)
                    drawCircle(Color.White, radius = 4f * scale, center = poiPt)
                }

                if (locations.size > 1 && selectedLayer != MapLayerMode.HEATMAP) {
                    val routePath = Path()
                    locations.forEachIndexed { i, loc ->
                        val point = mapToCanvas(loc.latitude, loc.longitude, scale, offsetX, offsetY)
                        if (i == 0) routePath.moveTo(point.x, point.y)
                        else routePath.lineTo(point.x, point.y)
                    }

                    currentLocation?.let { live ->
                        val livePoint = mapToCanvas(live.latitude, live.longitude, scale, offsetX, offsetY)
                        routePath.lineTo(livePoint.x, livePoint.y)
                    }

                    drawPath(
                        routePath,
                        StatusBlue.copy(alpha = 0.3f),
                        style = Stroke(width = 10f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    drawPath(
                        routePath,
                        StatusBlue,
                        style = Stroke(width = 4f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    val arrowStep = (locations.size / 5).coerceAtLeast(1)
                    for (i in 0 until locations.size - 1 step arrowStep) {
                        val p1 = mapToCanvas(locations[i].latitude, locations[i].longitude, scale, offsetX, offsetY)
                        val p2 = mapToCanvas(locations[i + 1].latitude, locations[i + 1].longitude, scale, offsetX, offsetY)
                        
                        val midX = (p1.x + p2.x) / 2f
                        val midY = (p1.y + p2.y) / 2f
                        val angle = atan2(p2.y - p1.y, p2.x - p1.x) * (180f / Math.PI.toFloat())

                        rotate(degrees = angle, pivot = Offset(midX, midY)) {
                            val arrowPath = Path().apply {
                                moveTo(midX + 6f * scale, midY)
                                lineTo(midX - 5f * scale, midY - 4f * scale)
                                lineTo(midX - 5f * scale, midY + 4f * scale)
                                close()
                            }
                            drawPath(arrowPath, Color.White)
                        }
                    }

                    locations.forEach { loc ->
                        val pt = mapToCanvas(loc.latitude, loc.longitude, scale, offsetX, offsetY)
                        val isSelected = selectedLocation?.id == loc.id
                        val color = if (isSelected) StatusAmber else StatusBlue
                        drawCircle(Color.White, radius = if (isSelected) 6f * scale else 3.5f * scale, center = pt)
                        drawCircle(color, radius = if (isSelected) 4f * scale else 2f * scale, center = pt)
                    }
                }

                activeLocation?.let { loc ->
                    val markerOffset = mapToCanvas(loc.latitude, loc.longitude, scale, offsetX, offsetY)

                    val activeZones = geofences.filter { it.isActive }
                    val isInsideAny = if (activeZones.isEmpty()) true else activeZones.any { zone ->
                        val dLat = Math.toRadians(zone.centerLat - loc.latitude)
                        val dLng = Math.toRadians(zone.centerLng - loc.longitude)
                        val lat1 = Math.toRadians(loc.latitude)
                        val lat2 = Math.toRadians(zone.centerLat)
                        val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
                        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                        (6371000.0 * c) <= zone.radiusMeters
                    }

                    val isPlayback = playbackLocation != null
                    val markerBaseColor = if (isPlayback) {
                        if (isInsideAny) StatusAmber else StatusRed
                    } else {
                        StatusBlue
                    }
                    val glowColor = if (isPlayback) {
                        if (isInsideAny) StatusGreen else StatusRed
                    } else {
                        StatusBlue
                    }

                    drawCircle(
                        color = glowColor.copy(alpha = 0.25f),
                        radius = 26f * scale,
                        center = markerOffset
                    )
                    drawCircle(
                        color = markerBaseColor.copy(alpha = 0.5f),
                        radius = 16f * scale,
                        center = markerOffset
                    )
                    drawCircle(
                        color = markerBaseColor,
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

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MapControlButton(
                    icon = Icons.Default.Layers,
                    onClick = {
                        selectedLayer = when (selectedLayer) {
                            MapLayerMode.VECTOR -> MapLayerMode.SATELLITE
                            MapLayerMode.SATELLITE -> MapLayerMode.HEATMAP
                            MapLayerMode.HEATMAP -> MapLayerMode.VECTOR
                        }
                    }
                )

                MapControlButton(
                    icon = Icons.Default.Add,
                    onClick = { scale = (scale * 1.25f).coerceAtMost(4.5f) }
                )

                MapControlButton(
                    icon = Icons.Default.Remove,
                    onClick = { scale = (scale / 1.25f).coerceAtLeast(0.5f) }
                )

                MapControlButton(
                    icon = Icons.Default.MyLocation,
                    iconColor = StatusGreen,
                    onClick = {
                        activeLocation?.let { centerOn(it.latitude, it.longitude) } ?: run {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                val layerName = when (selectedLayer) {
                    MapLayerMode.VECTOR -> tr("Vektör Haritası", "Vector Map")
                    MapLayerMode.SATELLITE -> tr("Uydu Görünümü", "Satellite Hybrid")
                    MapLayerMode.HEATMAP -> tr("Yoğunluk Haritası", "Density Heatmap")
                }
                Text(
                    text = "$layerName • ${(scale * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            selectedLocation?.let { loc ->
                val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                val formattedTime = timeFormat.format(Date(loc.timestamp))

                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 40.dp)
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(StatusAmber.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = StatusAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = tr("Konum Detayı", "Location Detail"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { selectedLocation = null },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = loc.address,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DetailBadge(icon = Icons.Default.Speed, text = "${loc.speed.toInt()} km/h", color = MaterialTheme.colorScheme.primary)
                            DetailBadge(icon = Icons.Default.BatteryChargingFull, text = "%${loc.batteryLevel}", color = StatusGreen)
                            DetailBadge(icon = Icons.Default.MyLocation, text = formattedTime, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
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

@Composable
fun DetailBadge(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}
