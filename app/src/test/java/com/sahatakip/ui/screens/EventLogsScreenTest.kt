package com.sahatakip.ui.screens

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sahatakip.ui.viewmodel.RequestLogViewModel
import com.sahatakip.data.local.entity.EventLogEntity
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
class EventLogsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testEventLogsScreenContent() {
        val mockViewModel = mockk<RequestLogViewModel>(relaxed = true)
        val logs = listOf(
            EventLogEntity(id = 1, type = "INFO", title = "Log 1", detail = "Detail 1", timestamp = System.currentTimeMillis(), isSensitive = false),
            EventLogEntity(id = 2, type = "WARNING", title = "Log 2", detail = "Detail 2", timestamp = System.currentTimeMillis(), isSensitive = false)
        )
        
        every { mockViewModel.allEventLogs } returns MutableStateFlow(logs)

        composeTestRule.setContent {
            EventLogsScreen(
                viewModel = mockViewModel,
                windowWidthSizeClass = WindowWidthSizeClass.Compact
            )
        }

        // Verify title
        composeTestRule.onNodeWithText("Olay Günlüğü", ignoreCase = true).assertExists()
        
        // Verify logs
        composeTestRule.onNodeWithText("Log 1").assertExists()
        composeTestRule.onNodeWithText("Detail 1").assertExists()
        composeTestRule.onNodeWithText("Log 2").assertExists()
    }

    @Test
    fun testEventLogsEmptyState() {
        val mockViewModel = mockk<RequestLogViewModel>(relaxed = true)
        every { mockViewModel.allEventLogs } returns MutableStateFlow(emptyList())

        composeTestRule.setContent {
            EventLogsScreen(
                viewModel = mockViewModel,
                windowWidthSizeClass = WindowWidthSizeClass.Compact
            )
        }

        composeTestRule.onNodeWithText("Henüz kaydedilmiş olay günlüğü yok.", substring = true).assertExists()
    }
}
