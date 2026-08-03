package com.example.ui.screens

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.ui.viewmodel.MainViewModel
import com.example.data.local.entity.LeaveRequestEntity
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
class LeaveRequestScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLeaveRequestScreenContent() {
        val mockViewModel = mockk<MainViewModel>(relaxed = true)
        val requests = listOf(
            LeaveRequestEntity(id = 1, startDate = "01.01.2026", endDate = "02.01.2026", requestType = "Annual", reason = "Holiday", status = "APPROVED"),
            LeaveRequestEntity(id = 2, startDate = "10.01.2026", endDate = "11.01.2026", requestType = "Sick", reason = "Flu", status = "PENDING")
        )
        
        every { mockViewModel.allLeaveRequests } returns MutableStateFlow(requests)

        composeTestRule.setContent {
            LeaveRequestScreen(
                viewModel = mockViewModel,
                windowWidthSizeClass = WindowWidthSizeClass.Compact
            )
        }

        // Verify title
        composeTestRule.onNodeWithText("İzin Talepleri", ignoreCase = true).assertExists()
        
        // Verify requests
        composeTestRule.onNodeWithText("Annual").assertExists()
        composeTestRule.onNodeWithText("Holiday").assertExists()
        composeTestRule.onNodeWithText("Sick").assertExists()
    }

    @Test
    fun testLeaveRequestEmptyState() {
        val mockViewModel = mockk<MainViewModel>(relaxed = true)
        every { mockViewModel.allLeaveRequests } returns MutableStateFlow(emptyList())

        composeTestRule.setContent {
            LeaveRequestScreen(
                viewModel = mockViewModel,
                windowWidthSizeClass = WindowWidthSizeClass.Compact
            )
        }

        composeTestRule.onNodeWithText("Talep bulunamadı.", substring = true).assertExists()
    }
}
