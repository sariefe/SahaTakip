package com.example.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.domain.repository.GeofenceRepository
import com.example.domain.repository.LocationRepository
import com.example.domain.repository.UserRepository
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackingViewModelTest {

    private lateinit var app: Application
    private lateinit var viewModel: TrackingViewModel

    @MockK lateinit var userRepository: UserRepository
    @MockK lateinit var locationRepository: LocationRepository
    @MockK lateinit var geofenceRepository: GeofenceRepository

    private val userProfileFlow = MutableStateFlow<UserProfileEntity?>(null)
    private val locationsFlow = MutableStateFlow<List<LocationEntity>>(emptyList())
    private val geofencesFlow = MutableStateFlow<List<GeofenceZoneEntity>>(emptyList())

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()

        every { userRepository.userProfile } returns userProfileFlow
        every { locationRepository.latestLocation } returns MutableStateFlow(null)
        every { locationRepository.getLocationsSince(any()) } returns locationsFlow
        every { geofenceRepository.allGeofences } returns geofencesFlow
        coEvery { userRepository.initializeAndSyncDefaultData() } just Runs

        viewModel = TrackingViewModel(userRepository, locationRepository, geofenceRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `startRoutePlayback updates state correctly`() = runTest {
        val points = listOf(LocationEntity(latitude = 41.0, longitude = 29.0), LocationEntity(latitude = 41.1, longitude = 29.1))
        locationsFlow.value = points

        viewModel.startRoutePlayback()
        assertTrue(viewModel.playbackState.value.isPlaying)
        
        viewModel.pauseRoutePlayback()
        assertFalse(viewModel.playbackState.value.isPlaying)
    }

    @Test
    fun `addGeofenceZone prevents duplicates`() = runTest {
        val existing = GeofenceZoneEntity(id = 1, name = "ZONE1", centerLat = 41.0, centerLng = 29.0)
        geofencesFlow.value = listOf(existing)
        
        coEvery { geofenceRepository.getAllGeofencesOnce() } returns listOf(existing)
        every { geofenceRepository.calculateDistanceInMeters(any(), any(), any(), any()) } returns 0.0
        
        viewModel.addGeofenceZone("ZONE1", 42.0, 30.0, 500.0)
        coVerify(exactly = 0) { geofenceRepository.insertGeofence(any()) }
    }

    @Test
    fun `deleteGeofence calls repository`() = runTest {
        coEvery { geofenceRepository.deleteGeofence(any()) } just Runs
        viewModel.deleteGeofence(10L)
        coVerify { geofenceRepository.deleteGeofence(10L) }
    }

    @Test
    fun `seekPlaybackProgress updates current location`() = runTest {
        val points = listOf(
            LocationEntity(latitude = 1.0, longitude = 1.0),
            LocationEntity(latitude = 2.0, longitude = 2.0),
            LocationEntity(latitude = 3.0, longitude = 3.0)
        )
        locationsFlow.value = points
        
        viewModel.seekPlaybackProgress(0.5f) // Should point to index 1
        assertEquals(points[1], viewModel.playbackState.value.currentLocation)
    }
}
