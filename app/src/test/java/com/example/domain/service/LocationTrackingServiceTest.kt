package com.example.domain.service

import android.app.NotificationManager
import android.content.Context
import com.example.domain.repository.LocationRepository
import com.example.data.local.PreferencesManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowService

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocationTrackingServiceTest {

    private lateinit var context: Context
    private lateinit var shadowService: ShadowService
    
    private val mockLocationRepository = mockk<LocationRepository>(relaxed = true)
    private val mockPreferencesManager = mockk<PreferencesManager>(relaxed = true)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = mockk(relaxed = true)
        
        every { mockPreferencesManager.updateInterval } returns MutableStateFlow(1)
        coEvery { mockLocationRepository.recordNewLocation(any(), any(), any(), any(), any(), any()) } returns 1L
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testServiceLifecycle() {
        val controller = Robolectric.buildService(LocationTrackingService::class.java)
        val service = controller.get()

        service.locationRepository = mockLocationRepository
        service.preferencesManager = mockPreferencesManager
        
        shadowService = shadowOf(service)

        controller.create()
        assertNotNull(service)
        
        // Check foreground notification
        val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNM = shadowOf(notificationManager)
        assertEquals(1, shadowNM.allNotifications.size)

        controller.startCommand(0, 0)
        
        controller.destroy()
    }
}
