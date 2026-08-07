package com.sahatakip.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.sahatakip.data.local.PreferencesManager
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsViewModelTest {

    private lateinit var app: Application
    private lateinit var viewModel: SettingsViewModel

    @MockK lateinit var mockPrefs: PreferencesManager

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()

        every { mockPrefs.language } returns MutableStateFlow("tr")
        every { mockPrefs.theme } returns MutableStateFlow("system")
        every { mockPrefs.updateInterval } returns MutableStateFlow(60)
        every { mockPrefs.mockServerUrl } returns MutableStateFlow("http://test.com")
        every { mockPrefs.setLanguage(any()) } just Runs
        every { mockPrefs.setTheme(any()) } just Runs
        every { mockPrefs.setUpdateInterval(any()) } just Runs
        every { mockPrefs.setMockServerUrl(any()) } just Runs

        viewModel = SettingsViewModel(mockPrefs)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `setLanguage updates preference manager`() {
        viewModel.setLanguage("en")
        verify { mockPrefs.setLanguage("en") }
    }

    @Test
    fun `setTheme updates preference manager`() {
        viewModel.setTheme("dark")
        verify { mockPrefs.setTheme("dark") }
    }

    @Test
    fun `setUpdateInterval updates preference manager`() {
        viewModel.setUpdateInterval(120)
        verify { mockPrefs.setUpdateInterval(120) }
    }

    @Test
    fun `setMockServerUrl updates preference manager`() {
        viewModel.setMockServerUrl("http://new.com")
        verify { mockPrefs.setMockServerUrl("http://new.com") }
    }
}
