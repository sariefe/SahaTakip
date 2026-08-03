package com.example.util

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationHelperTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)
    }

    @Test
    fun testCreateNotificationChannel() {
        NotificationHelper.createNotificationChannel(context)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel("saha_security_alerts")
            assertNotNull(channel)
            assertEquals("Saha Güvenlik ve Bölge İhlal Bildirimleri", channel!!.name)
        }
    }

    @Test
    fun testSendPrivacySafeAlert() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
                .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        NotificationHelper.sendPrivacySafeAlert(context, "Test Alert")
        
        val notifications = shadowNotificationManager.allNotifications
        assertEquals(1, notifications.size)
        
        val notification = notifications[0]
        val title = notification.extras.getCharSequence("android.title")
        assertEquals("Test Alert", title.toString())
    }
}
