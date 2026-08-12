package com.sahatakip.ui.screens

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.sahatakip.data.local.entity.UserProfileEntity
import com.sahatakip.domain.model.DeviceStatus
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
        val userProfile = UserProfileEntity(fullName = "Test User", staffId = "ID-123")
        
        composeTestRule.setContent {
            com.sahatakip.ui.theme.SahaTakipTheme {
                DashboardScreenContent(
                    deviceStatus = DeviceStatus(batteryLevel = 85),
                    userProfile = userProfile,
                    latestLoc = null,
                    isSyncing = false,
                    syncError = null,
                    windowWidthSizeClass = WindowWidthSizeClass.Compact,
                    onNavigateToMap = {},
                    onTriggerSync = {},
                    onLogout = {}
                )
            }
        }

        // Verify user info
        composeTestRule.onNodeWithText("Hoş Geldin,", substring = true).assertExists()
        composeTestRule.onNodeWithText("Test User").assertExists()

        // Verify battery level
        composeTestRule.onNodeWithTag("BatteryCard", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Pil", substring = true).assertExists()
        composeTestRule.onNodeWithText("%85", substring = true).assertExists()

        // Verify service status items
        composeTestRule.onNodeWithText("İnternet", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("GPS", ignoreCase = true).assertExists()
    }
}
