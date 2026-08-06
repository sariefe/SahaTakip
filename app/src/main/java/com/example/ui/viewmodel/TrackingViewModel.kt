package com.example.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.domain.repository.GeofenceRepository
import com.example.domain.repository.LocationRepository
import com.example.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class PlaybackState(
    val isPlaying: Boolean = false,
    val speedMultiplier: Float = 1.0f,
    val progress: Float = 0.0f,
    val currentIndex: Int = 0,
    val currentLocation: LocationEntity? = null,
)

@HiltViewModel
class TrackingViewModel @Inject constructor(
    userRepository: UserRepository,
    locationRepository: LocationRepository,
    private val geofenceRepository: GeofenceRepository,
) : androidx.lifecycle.ViewModel() {

    val userProfile: StateFlow<UserProfileEntity?> = userRepository.userProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val locationsLast24h: StateFlow<List<LocationEntity>> = locationRepository.getLocationsSince(
        System.currentTimeMillis() - (24 * 60 * 60 * 1000L),
    ).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val latestLocation: StateFlow<LocationEntity?> = locationRepository.latestLocation
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allGeofences: StateFlow<List<GeofenceZoneEntity>> = geofenceRepository.allGeofences
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
            if (_playbackState.value.currentIndex >= (points.size - 1)) {
                _playbackState.value = _playbackState.value.copy(currentIndex = 0)
            }

            while ((_playbackState.value.currentIndex < points.size) && _playbackState.value.isPlaying) {
                val idx = _playbackState.value.currentIndex
                val currPoint = points[idx]
                val prog = idx.toFloat() / (points.size - 1).coerceAtLeast(1)

                _playbackState.value = _playbackState.value.copy(
                    progress = prog,
                    currentLocation = currPoint,
                )

                delay((800 / _playbackState.value.speedMultiplier).toLong().milliseconds)
                
                if (_playbackState.value.isPlaying) {
                    _playbackState.value = _playbackState.value.copy(currentIndex = idx + 1)
                }
            }

            if (_playbackState.value.currentIndex >= points.size) {
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
            }
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
            currentLocation = points[targetIdx],
        )
    }

    fun addGeofenceZone(name: String, lat: Double, lng: Double, radiusMeters: Double) {
        viewModelScope.launch {
            val existingZones = geofenceRepository.getAllGeofencesOnce()
            val collator = java.text.Collator.getInstance(java.util.Locale.forLanguageTag("tr")).apply {
                strength = java.text.Collator.PRIMARY
            }
            val isNameDuplicate = existingZones.any { 
                collator.compare(it.name.trim(), name.trim()) == 0 
            }
            
            val isLocationDuplicate = existingZones.any {
                 geofenceRepository.calculateDistanceInMeters(lat, lng, it.centerLat, it.centerLng) < 50.0
            }

            if (!isNameDuplicate && !isLocationDuplicate) {
                geofenceRepository.insertGeofence(
                    GeofenceZoneEntity(
                        name = name.trim(),
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
            geofenceRepository.deleteGeofence(id)
        }
    }

    fun toggleGeofenceActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            geofenceRepository.setGeofenceActive(id, isActive)
        }
    }

    fun updateGeofenceZone(id: Long, name: String, radiusMeters: Double) {
        viewModelScope.launch {
            val zones = geofenceRepository.getAllGeofencesOnce()
            val existing = zones.find { it.id == id }
            
            val collator = java.text.Collator.getInstance(java.util.Locale.forLanguageTag("tr")).apply {
                strength = java.text.Collator.PRIMARY
            }
            val isNameDuplicate = zones.any { 
                it.id != id && collator.compare(it.name.trim(), name.trim()) == 0 
            }

            if (existing != null && !isNameDuplicate) {
                geofenceRepository.insertGeofence(
                    existing.copy(name = name.trim(), radiusMeters = radiusMeters)
                )
            }
        }
    }
}
