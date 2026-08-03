package com.example.domain.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.example.data.repository.SahaRepository
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
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

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = io.mockk.mockk(relaxed = true)
        
        // Mock SahaRepository construction
        mockkConstructor(SahaRepository::class)
        every { anyConstructed<SahaRepository>().preferencesManager.updateInterval } returns MutableStateFlow(1)
        coEvery { anyConstructed<SahaRepository>().recordNewLocation(any(), any(), any(), any(), any(), any()) } returns 1L
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testServiceLifecycle() {
        val controller = Robolectric.buildService(LocationTrackingService::class.java)
        val service = controller.get()
        shadowService = shadowOf(service)

        controller.create()
        assertNotNull(service)
        
        // Check foreground notification
        val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNM = shadowOf(notificationManager)
        assertEquals(1, shadowNM.allNotifications.size)

        controller.startCommand(0, 0)
        
        controller.destroy()
        // verify repo interactions if possible, but it's in a loop
    }
}
