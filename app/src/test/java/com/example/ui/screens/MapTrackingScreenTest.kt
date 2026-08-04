package com.example.ui.screens

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.ui.viewmodel.TrackingViewModel
import com.example.ui.viewmodel.PlaybackState
import com.example.data.local.entity.LocationEntity
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
class MapTrackingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testMapTrackingScreenContent() {
        val mockViewModel = mockk<TrackingViewModel>(relaxed = true)
        val locations = listOf(
            LocationEntity(id = 1, latitude = 41.0, longitude = 28.0, address = "Point 1"),
            LocationEntity(id = 2, latitude = 41.1, longitude = 28.1, address = "Point 2")
        )
        
        every { mockViewModel.locationsLast24h } returns MutableStateFlow(locations)
        every { mockViewModel.latestLocation } returns MutableStateFlow(locations.last())
        every { mockViewModel.allGeofences } returns MutableStateFlow(emptyList())
        every { mockViewModel.playbackState } returns MutableStateFlow(PlaybackState())

        composeTestRule.setContent {
            MapTrackingScreen(
                viewModel = mockViewModel,
                windowWidthSizeClass = WindowWidthSizeClass.Compact
            )
        }

        // Verify telemetry
        composeTestRule.onNodeWithText("Mesafe", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Ort. Hız", ignoreCase = true).assertExists()
        
        // Verify playback card
        composeTestRule.onNodeWithText("Güzergah Oynat", ignoreCase = true).assertExists()
        
        // Verify geofence section
        composeTestRule.onNodeWithText("Güvenli Bölgeler", ignoreCase = true).assertExists()
    }
}
