package com.sahatakip.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.sahatakip.data.local.PreferencesManager
import com.sahatakip.domain.repository.EventRepository
import com.sahatakip.domain.repository.SyncRepository
import com.sahatakip.util.ConnectivityObserver
import com.sahatakip.util.PermissionUtils
import com.sahatakip.util.SecurityUtils
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceViewModelTest {

    private lateinit var app: Application
    private lateinit var viewModel: DeviceViewModel

    @MockK lateinit var eventRepository: EventRepository
    @MockK lateinit var syncRepository: SyncRepository
    @MockK lateinit var preferencesManager: PreferencesManager
    @MockK lateinit var mockConnectivityObserver: ConnectivityObserver

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()

        every { preferencesManager.language } returns MutableStateFlow(value = "tr")
        coEvery { eventRepository.addEventLog(any(), any(), any(), any(), any()) } just Runs
        coEvery { syncRepository.performOfflineSync() } returns true
        
        every { mockConnectivityObserver.observe() } returns kotlinx.coroutines.flow.emptyFlow()

        mockkObject(PermissionUtils)
        every { PermissionUtils.hasLocationPermissions(any()) } returns true
        every { PermissionUtils.hasBackgroundLocationPermission(any()) } returns true
        every { PermissionUtils.hasNotificationPermission(any()) } returns true
        every { PermissionUtils.hasCameraPermission(any()) } returns true
        every { PermissionUtils.isGpsEnabled(any()) } returns true
        every { PermissionUtils.isIgnoringBatteryOptimizations(any()) } returns true
        every { PermissionUtils.isPowerSaveMode(any()) } returns false

        mockkObject(SecurityUtils)
        coEvery { SecurityUtils.checkIsDeviceRooted() } returns false

        viewModel = DeviceViewModel(app, eventRepository, syncRepository, preferencesManager, mockConnectivityObserver)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleGpsSimulation logs event when disabled`() = runTest {
        if (!viewModel.deviceStatus.value.isGpsEnabled) {
            viewModel.toggleGpsSimulation()
        }
        assertTrue(viewModel.deviceStatus.value.isGpsEnabled)

        viewModel.toggleGpsSimulation()
        assertFalse(viewModel.deviceStatus.value.isGpsEnabled)
        coVerify { eventRepository.addEventLog(type = "GPS_DISABLED", any(), any(), any(), any()) }
    }

    @Test
    fun `toggleInternetSimulation logs event and syncs when restored`() = runTest {
        viewModel.toggleInternetSimulation()
        assertFalse(viewModel.deviceStatus.value.isInternetConnected)
        coVerify { eventRepository.addEventLog(type = "INTERNET_LOST", any(), any(), any(), any()) }

        viewModel.toggleInternetSimulation()
        assertTrue(viewModel.deviceStatus.value.isInternetConnected)
        coVerify { eventRepository.addEventLog(type = "INTERNET_RESTORED", any(), any(), any(), any()) }
        coVerify { syncRepository.performOfflineSync() }
    }

    @Test
    fun `triggerOfflineSync updates syncing state`() = runTest {
        viewModel.triggerOfflineSync()
        assertFalse(viewModel.isSyncing.value)
        coVerify { syncRepository.performOfflineSync() }
    }
}
