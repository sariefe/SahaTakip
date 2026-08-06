package com.example.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PreferencesManager
import com.example.data.local.dao.GeofenceDao
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.domain.repository.EventRepository
import com.example.util.NotificationService
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeofenceRepositoryImplTest {

    private lateinit var app: Application
    @MockK lateinit var mockGeofenceDao: GeofenceDao
    @MockK lateinit var mockEventRepository: EventRepository
    @MockK lateinit var mockNotificationService: NotificationService
    @MockK lateinit var mockPreferencesManager: PreferencesManager

    private lateinit var geofenceRepository: GeofenceRepositoryImpl

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        app = ApplicationProvider.getApplicationContext()
        
        every { mockNotificationService.sendPrivacySafeAlert(any(), any()) } just Runs
        every { mockGeofenceDao.getAllGeofences() } returns emptyFlow()
        every { mockPreferencesManager.language } returns MutableStateFlow("tr")
        coEvery { mockEventRepository.insertEventLog(any()) } returns 1L

        geofenceRepository = GeofenceRepositoryImpl(
            app,
            mockGeofenceDao,
            mockEventRepository,
            mockNotificationService,
            mockPreferencesManager
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `checkGeofenceBreach triggers breach when outside active zone`() = runTest {
        val lat = 41.0
        val lng = 29.0

        val zone = GeofenceZoneEntity(
            id = 1,
            name = "Safe Zone",
            centerLat = 40.0,
            centerLng = 28.0,
            radiusMeters = 1000.0,
            isActive = true
        )
        
        coEvery { mockGeofenceDao.getActiveGeofences() } returns listOf(zone)

        geofenceRepository.checkGeofenceBreach(lat, lng)

        coVerify { mockEventRepository.insertEventLog(match { it.type == "GEOFENCE_VIOLATION" }) }
        verify { mockNotificationService.sendPrivacySafeAlert(any(), "Güvenlik & Bölge İhlali Uyarısı") }
    }

    @Test
    fun `checkGeofenceBreach does NOT trigger breach when inside active zone`() = runTest {
        val lat = 41.0
        val lng = 29.0

        val zone = GeofenceZoneEntity(
            id = 1,
            name = "Safe Zone",
            centerLat = 41.0,
            centerLng = 29.0,
            radiusMeters = 1000.0,
            isActive = true
        )
        
        coEvery { mockGeofenceDao.getActiveGeofences() } returns listOf(zone)

        geofenceRepository.checkGeofenceBreach(lat, lng)

        coVerify(exactly = 0) { mockEventRepository.insertEventLog(any()) }
        verify(exactly = 0) { mockNotificationService.sendPrivacySafeAlert(any(), any()) }
    }
}
