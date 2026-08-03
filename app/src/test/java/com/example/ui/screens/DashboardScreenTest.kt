package com.example.ui.screens

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.data.local.entity.UserProfileEntity
import com.example.domain.model.DeviceStatus
import com.example.ui.viewmodel.MainViewModel
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
        val mockViewModel = mockk<MainViewModel>(relaxed = true)
        val userProfile = UserProfileEntity(fullName = "Test User", staffId = "ID-123")
        
        every { mockViewModel.deviceStatus } returns MutableStateFlow(DeviceStatus(batteryLevel = 85))
        every { mockViewModel.userProfile } returns MutableStateFlow(userProfile)
        every { mockViewModel.latestLocation } returns MutableStateFlow(null)
        every { mockViewModel.isSyncing } returns MutableStateFlow(false)

        composeTestRule.setContent {
            DashboardScreen(
                viewModel = mockViewModel,
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
