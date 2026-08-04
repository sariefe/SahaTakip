package com.example.ui.screens

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.viewmodel.AuthViewModel
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
class AuthScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAuthScreenContent() {
        val mockViewModel = mockk<AuthViewModel>(relaxed = true)
        
        every { mockViewModel.ocrScanningState } returns MutableStateFlow(null)
        every { mockViewModel.ocrIsLoading } returns MutableStateFlow(false)
        every { mockViewModel.ocrStability } returns MutableStateFlow(0f)
        every { mockViewModel.authErrorMessage } returns MutableStateFlow(null)
        every { mockViewModel.isAuthenticated } returns MutableStateFlow(false)
        every { mockViewModel.ocrAuthError } returns MutableStateFlow(null)
        every { mockViewModel.userProfile } returns MutableStateFlow(null)
        every { mockViewModel.ocrScanSuggested } returns MutableStateFlow(true)

        composeTestRule.setContent {
            AuthScreen(
                viewModel = mockViewModel,
                onAuthSuccess = {}
            )
        }

        composeTestRule.onNodeWithText("SAHA TAKİP", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Personel Aktivasyon", substring = true).assertExists()
        
        composeTestRule.onNodeWithText("Aktivasyon Kodu", substring = true).assertExists()
        composeTestRule.onNodeWithText("AKTİVASYONU TAMAMLA", ignoreCase = true).performClick()
    }
}
