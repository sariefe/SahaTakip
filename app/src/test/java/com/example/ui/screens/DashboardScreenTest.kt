package com.example.ui.screens

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.data.local.entity.UserProfileEntity
import com.example.domain.model.DeviceStatus
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.DeviceViewModel
import com.example.ui.viewmodel.TrackingViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDashboardScreenContent() {
        val mockDeviceViewModel = mockk<DeviceViewModel>(relaxed = true)
        val mockTrackingViewModel = mockk<TrackingViewModel>(relaxed = true)
        val mockAuthViewModel = mockk<AuthViewModel>(relaxed = true)
        val userProfile = UserProfileEntity(fullName = "Test User", staffId = "ID-123")
        
        every { mockDeviceViewModel.deviceStatus } returns MutableStateFlow(DeviceStatus(batteryLevel = 85))
        every { mockDeviceViewModel.isSyncing } returns MutableStateFlow(false)
        every { mockTrackingViewModel.userProfile } returns MutableStateFlow(userProfile)
        every { mockTrackingViewModel.latestLocation } returns MutableStateFlow(null)

        composeTestRule.setContent {
            DashboardScreen(
                deviceViewModel = mockDeviceViewModel,
                trackingViewModel = mockTrackingViewModel,
                authViewModel = mockAuthViewModel,
                onNavigateToMap = {},
                windowWidthSizeClass = WindowWidthSizeClass.Compact
            )
        }

        // Verify user info
        composeTestRule.onNodeWithText("Hoş Geldin,", substring = true).assertExists()
        composeTestRule.onNodeWithText("Test User").assertExists()

        // Verify battery level
        composeTestRule.onNodeWithText("Pil", substring = true).assertExists()
        composeTestRule.onNodeWithText("%85", substring = true).assertExists()

        // Verify service status items
        composeTestRule.onNodeWithText("İnternet", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("GPS", ignoreCase = true).assertExists()
    }
}
