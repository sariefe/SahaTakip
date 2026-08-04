package com.example.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.SahaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class PlaybackState(
    val isPlaying: Boolean = false,
    val speedMultiplier: Float = 1.0f,
    val progress: Float = 0.0f,
    val currentIndex: Int = 0,
    val currentLocation: LocationEntity? = null
)

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val repository: SahaRepository
) : androidx.lifecycle.ViewModel() {

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val locationsLast24h: StateFlow<List<LocationEntity>> = repository.locationDao.getLocationsSince(
        System.currentTimeMillis() - 24 * 60 * 60 * 1000L
    ).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val latestLocation: StateFlow<LocationEntity?> = repository.latestLocation
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allGeofences: StateFlow<List<GeofenceZoneEntity>> = repository.allGeofences
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var playbackJob: Job? = null

    fun startRoutePlayback() {
        val points = locationsLast24h.value
        if (points.isEmpty()) return

        playbackJob?.cancel()
        _playbackState.value = _playbackState.value.copy(isPlaying = true)

        playbackJob = viewModelScope.launch {
            var idx = _playbackState.value.currentIndex
            if (idx >= points.size - 1) idx = 0

            while (idx < points.size && _playbackState.value.isPlaying) {
                val currPoint = points[idx]
                val prog = idx.toFloat() / (points.size - 1).coerceAtLeast(1)

                _playbackState.value = _playbackState.value.copy(
                    currentIndex = idx,
                    progress = prog,
                    currentLocation = currPoint
                )

                delay((800 / _playbackState.value.speedMultiplier).toLong().milliseconds)
                idx++
            }

            _playbackState.value = _playbackState.value.copy(isPlaying = false)
        }
    }

    fun pauseRoutePlayback() {
        playbackJob?.cancel()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(speedMultiplier = speed)
    }

    fun seekPlaybackProgress(progress: Float) {
        val points = locationsLast24h.value
        if (points.isEmpty()) return
        val targetIdx = (progress * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
        _playbackState.value = _playbackState.value.copy(
            progress = progress,
            currentIndex = targetIdx,
            currentLocation = points[targetIdx]
        )
    }

    fun addGeofenceZone(name: String, lat: Double, lng: Double, radiusMeters: Double) {
        viewModelScope.launch {
            val existingZones = repository.geofenceDao.getAllGeofences().firstOrNull() ?: emptyList()
            val isDuplicate = existingZones.any { 
                it.name.equals(name, ignoreCase = true) || 
                (repository.calculateDistanceInMeters(lat, lng, it.centerLat, it.centerLng) < 10.0) 
            }

            if (!isDuplicate) {
                repository.geofenceDao.insertGeofence(
                    GeofenceZoneEntity(
                        name = name,
                        centerLat = lat,
                        centerLng = lng,
                        radiusMeters = radiusMeters,
                        isActive = true
                    )
                )
            }
        }
    }

    fun deleteGeofence(id: Long) {
        viewModelScope.launch {
            repository.deleteGeofence(id)
        }
    }

    fun toggleGeofenceActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.geofenceDao.setGeofenceActive(id, isActive)
        }
    }

    fun updateGeofenceZone(id: Long, name: String, radiusMeters: Double) {
        viewModelScope.launch {
            val zones = repository.geofenceDao.getAllGeofences().firstOrNull()
            val existing = zones?.find { it.id == id }
            existing?.let {
                repository.geofenceDao.insertGeofence(
                    it.copy(name = name, radiusMeters = radiusMeters)
                )
            }
        }
    }
}
