package com.example.util

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLocationManager
import org.robolectric.shadows.ShadowPowerManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PermissionUtilsTest {

    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testHasLocationPermissions() {
        val shadowApp = shadowOf(app)
        shadowApp.denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowApp.denyPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        assertFalse(PermissionUtils.hasLocationPermissions(app))

        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        assertTrue(PermissionUtils.hasLocationPermissions(app))
    }

    @Test
    fun testHasBackgroundLocationPermission() {
        val shadowApp = shadowOf(app)
        shadowApp.denyPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        assertFalse(PermissionUtils.hasBackgroundLocationPermission(app))

        shadowApp.grantPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        assertTrue(PermissionUtils.hasBackgroundLocationPermission(app))
    }

    @Test
    @Config(sdk = [28])
    fun testHasBackgroundLocationPermissionOldSdk() {
        assertTrue(PermissionUtils.hasBackgroundLocationPermission(app))
    }

    @Test
    fun testHasNotificationPermission() {
        val shadowApp = shadowOf(app)
        shadowApp.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(PermissionUtils.hasNotificationPermission(app))

        shadowApp.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(PermissionUtils.hasNotificationPermission(app))
    }

    @Test
    fun testIsGpsEnabled() {
        val locationManager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val shadowLocationManager: ShadowLocationManager = shadowOf(locationManager)
        
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, false)
        assertFalse(PermissionUtils.isGpsEnabled(app))

        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        assertTrue(PermissionUtils.isGpsEnabled(app))
    }

    @Test
    fun testIsPowerSaveMode() {
        val powerManager = app.getSystemService(Context.POWER_SERVICE) as PowerManager
        val shadowPowerManager: ShadowPowerManager = shadowOf(powerManager)

        shadowPowerManager.setIsPowerSaveMode(false)
        assertFalse(PermissionUtils.isPowerSaveMode(app))

        shadowPowerManager.setIsPowerSaveMode(true)
        assertTrue(PermissionUtils.isPowerSaveMode(app))
    }

    @Test
    fun testIsIgnoringBatteryOptimizations() {
        val powerManager = app.getSystemService(Context.POWER_SERVICE) as PowerManager
        val shadowPowerManager: ShadowPowerManager = shadowOf(powerManager)
        
        shadowPowerManager.setIgnoringBatteryOptimizations(app.packageName, false)
        assertFalse(PermissionUtils.isIgnoringBatteryOptimizations(app))

        shadowPowerManager.setIgnoringBatteryOptimizations(app.packageName, true)
        assertTrue(PermissionUtils.isIgnoringBatteryOptimizations(app))
    }

    @Test
    @Config(sdk = [32]) // Tiramisu is 33, test 32 for old notification logic
    fun testHasNotificationPermissionOldSdk() {
        assertTrue(PermissionUtils.hasNotificationPermission(app))
    }

    @Test
    fun testGetRequiredPermissions() {
        val permissions = PermissionUtils.getRequiredPermissions()
        assertTrue(permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
        assertTrue(permissions.contains(Manifest.permission.CAMERA))
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            assertTrue(permissions.contains(Manifest.permission.POST_NOTIFICATIONS))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            assertTrue(permissions.contains(Manifest.permission.FOREGROUND_SERVICE_LOCATION))
        }
    }
}
