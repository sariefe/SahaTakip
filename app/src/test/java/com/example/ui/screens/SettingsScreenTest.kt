package com.example.ui.screens

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.ui.viewmodel.SettingsViewModel
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
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSettingsScreenContent() {
        val mockViewModel = mockk<SettingsViewModel>(relaxed = true)
        
        every { mockViewModel.language } returns MutableStateFlow("tr")
        every { mockViewModel.updateInterval } returns MutableStateFlow(60)
        every { mockViewModel.theme } returns MutableStateFlow("system")
        every { mockViewModel.mockServerUrl } returns MutableStateFlow("https://mock-api.example.com")

        composeTestRule.setContent {
            SettingsScreen(
                viewModel = mockViewModel,
                windowWidthSizeClass = WindowWidthSizeClass.Compact
            )
        }

        // Verify sections
        composeTestRule.onNodeWithText("Ayarlar", substring = true).assertExists()
        composeTestRule.onNodeWithText("Görünüm ve Dil", substring = true).assertExists()
        composeTestRule.onNodeWithText("Takip Yapılandırması", substring = true).assertExists()

        // Verify chips
        composeTestRule.onNodeWithText("TR").assertExists()
        composeTestRule.onNodeWithText("EN").assertExists()
        composeTestRule.onNodeWithText("60s").assertExists()
    }
}
