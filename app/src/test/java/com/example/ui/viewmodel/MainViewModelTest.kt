package com.example.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.dao.LocationDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.EventLogEntity
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.data.local.entity.LeaveRequestEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.SahaRepository
import com.example.util.OcrCardScanner
import com.example.util.OcrLine
import com.example.util.ScannedStaffCardResult
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainViewModelTest {

    private lateinit var app: Application
    private lateinit var viewModel: MainViewModel

    @MockK lateinit var mockRepository: SahaRepository
    @MockK lateinit var mockUserDao: UserDao
    @MockK lateinit var mockLocationDao: LocationDao
    @MockK lateinit var mockEventLogDao: com.example.data.local.dao.EventLogDao
    @MockK lateinit var mockOfflineDao: com.example.data.local.dao.OfflineActivityReportDao
    @MockK lateinit var mockLeaveRequestDao: com.example.data.local.dao.LeaveRequestDao
    @MockK lateinit var mockGeofenceDao: com.example.data.local.dao.GeofenceDao

    private val userProfileFlow = MutableStateFlow<UserProfileEntity?>(null)
    private val latestLocationFlow = MutableStateFlow<LocationEntity?>(null)
    private val allEventLogsFlow = MutableStateFlow<List<EventLogEntity>>(emptyList())
    private val allLeaveRequestsFlow = MutableStateFlow<List<LeaveRequestEntity>>(emptyList())
    private val allGeofencesFlow = MutableStateFlow<List<GeofenceZoneEntity>>(emptyList())

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()

        // Reset flows to clean state
        userProfileFlow.value = null
        latestLocationFlow.value = null
        allEventLogsFlow.value = emptyList()
        allLeaveRequestsFlow.value = emptyList()
        allGeofencesFlow.value = emptyList()

        // Mock Repository properties and methods
        every { mockRepository.userProfile } returns userProfileFlow
        every { mockRepository.latestLocation } returns latestLocationFlow
        every { mockRepository.allEventLogs } returns allEventLogsFlow
        every { mockRepository.allLeaveRequests } returns allLeaveRequestsFlow
        every { mockRepository.allGeofences } returns allGeofencesFlow

        every { mockRepository.userDao } returns mockUserDao
        every { mockRepository.locationDao } returns mockLocationDao
        every { mockRepository.eventLogDao } returns mockEventLogDao
        every { mockRepository.offlineReportDao } returns mockOfflineDao
        every { mockRepository.leaveRequestDao } returns mockLeaveRequestDao
        every { mockRepository.geofenceDao } returns mockGeofenceDao
        val mockPrefs = mockk<com.example.data.local.PreferencesManager>(relaxed = true)
        every { mockPrefs.language } returns MutableStateFlow("tr")
        every { mockPrefs.updateInterval } returns MutableStateFlow(60)
        every { mockPrefs.theme } returns MutableStateFlow("system")
        every { mockPrefs.mockServerUrl } returns MutableStateFlow("http://test.com")
        every { mockRepository.preferencesManager } returns mockPrefs

        // Mock DAO methods used in ViewModel init / repo sync
        every { mockLocationDao.getLocationsSince(any()) } returns emptyFlow() // or some valid flow
        every { mockLocationDao.getLatestLocation() } returns latestLocationFlow
        coEvery { mockLocationDao.getUnsyncedLocations() } returns emptyList()
        every { mockEventLogDao.getAllEventLogs() } returns allEventLogsFlow
        coEvery { mockEventLogDao.getUnsyncedLogs() } returns emptyList()
        coEvery { mockOfflineDao.getUnsyncedReports() } returns emptyList()
        every { mockUserDao.getUserProfile() } returns userProfileFlow
        every { mockLeaveRequestDao.getAllLeaveRequests() } returns allLeaveRequestsFlow
        every { mockGeofenceDao.getAllGeofences() } returns allGeofencesFlow
        coEvery { mockGeofenceDao.getActiveGeofences() } returns emptyList()
        coEvery { mockGeofenceDao.insertGeofence(any()) } returns 0L

        coEvery { mockUserDao.insertOrUpdateUser(any()) } just Runs
        coEvery { mockUserDao.updateLastLogin(any()) } just Runs
        coEvery { mockRepository.deactivateUser() } just Runs
        coEvery { mockRepository.performOfflineSync() } returns true
        coEvery { mockRepository.initializeAndSyncDefaultData() } just Runs
        coEvery { mockRepository.addEventLog(any(), any(), any(), any(), any()) } just Runs
        coEvery { mockRepository.deleteGeofence(any()) } just Runs

        viewModel = MainViewModel(app, mockRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `activateWithCode returns true for correct default code`() = runTest {
        // Correct code is "SAHA2026" or "123456"
        coEvery { mockUserDao.insertOrUpdateUser(any()) } just Runs
        userProfileFlow.value = null

        val result = viewModel.activateWithCode("123456")

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result)
        assertTrue(viewModel.isAuthenticated.value)
        // Verify insertion
        coVerify { mockUserDao.insertOrUpdateUser(any()) }
    }

    @Test
    fun `activateWithCode returns false for incorrect code`() = runTest {
        val result = viewModel.activateWithCode("WRONG")

        assertFalse(result)
        assertFalse(viewModel.isAuthenticated.value)
        org.junit.Assert.assertNotNull(viewModel.authErrorMessage.value)
    }

    @Test
    fun `logout resets authentication state`() = runTest {
        // Set authenticated first
        viewModel.activateWithCode("123456")
        assertTrue(viewModel.isAuthenticated.value)
        
        viewModel.logout()
        
        assertFalse(viewModel.isAuthenticated.value)
    }

    @Test
    fun `authenticateWithBiometrics updates state`() = runTest {
        coEvery { mockUserDao.updateLastLogin(any()) } just Runs
        
        val result = viewModel.authenticateWithBiometrics()
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(result)
        assertTrue(viewModel.isAuthenticated.value)
        coVerify { mockUserDao.updateLastLogin(any()) }
    }

    @Test
    fun `authenticateWithOcr returns true when ID matches profile`() = runTest {
        val user = UserProfileEntity(staffId = "ID-123")
        
        // Setup mock first
        coEvery { mockUserDao.updateLastLogin(any()) } just Runs
        coEvery { mockUserDao.insertOrUpdateUser(any()) } just Runs
        coEvery { mockRepository.addEventLog(any(), any(), any(), any(), any()) } just Runs

        // Set value and wait for propagation to MainViewModel's internal userProfile StateFlow
        userProfileFlow.value = user
        
        // Re-create ViewModel if necessary to ensure it picks up the latest flow state
        // OR just advance and verify
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.authenticateWithOcr("ID-123")
        
        assertTrue("Expected true for matching staff ID, but userProfile.value is ${viewModel.userProfile.value}", result)
        assertTrue(viewModel.isAuthenticated.value)
    }

    @Test
    fun `authenticateWithOcr returns false when ID mismatches`() = runTest {
        val user = UserProfileEntity(staffId = "ID-123")
        userProfileFlow.value = user
        coEvery { mockRepository.addEventLog(any(), any(), any(), any()) } just Runs

        val result = viewModel.authenticateWithOcr("WRONG-ID")
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse(result)
        assertFalse(viewModel.isAuthenticated.value)
        assertEquals("Personel kartı kayıtlı personel ile eşleşmiyor!", viewModel.ocrAuthError.value)
    }

    @Test
    fun `toggleGpsSimulation logs event when disabled`() = runTest {
        coEvery { mockRepository.addEventLog(any(), any(), any(), any(), any()) } just Runs
        
        // Initial state from DeviceStatus() is isGpsEnabled = true
        // But let's ensure it's in a consistent state
        if (!viewModel.deviceStatus.value.isGpsEnabled) {
            viewModel.toggleGpsSimulation()
            testDispatcher.scheduler.advanceUntilIdle()
        }

        assertTrue(viewModel.deviceStatus.value.isGpsEnabled)

        viewModel.toggleGpsSimulation() // toggles to false
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse(viewModel.deviceStatus.value.isGpsEnabled)
        coVerify { mockRepository.addEventLog(type = "GPS_DISABLED", any(), any(), any(), any()) }
    }

    @Test
    fun `toggleInternetSimulation logs event and syncs when restored`() = runTest {
        coEvery { mockRepository.addEventLog(any(), any(), any(), any(), any()) } just Runs
        coEvery { mockRepository.performOfflineSync() } returns true
        
        // Toggle to offline
        viewModel.toggleInternetSimulation()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.deviceStatus.value.isInternetConnected)
        coVerify { mockRepository.addEventLog(type = "INTERNET_LOST", any(), any(), any(), any()) }
        
        // Toggle back to online
        viewModel.toggleInternetSimulation()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.deviceStatus.value.isInternetConnected)
        coVerify { mockRepository.addEventLog(type = "INTERNET_RESTORED", any(), any(), any(), any()) }
        coVerify { mockRepository.performOfflineSync() }
    }

    @Test
    fun `startIdCardOcrScan updates loading state and result`() = runTest {
        mockkObject(OcrCardScanner)
        val mockResult = ScannedStaffCardResult("TEST", "USER", "ID-1", "DEPT")
        coEvery { OcrCardScanner.processStaffCardScan(any()) } returns mockResult
        coEvery { mockRepository.addEventLog(any(), any(), any(), any(), any()) } just Runs

        viewModel.startIdCardOcrScan()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse(viewModel.ocrIsLoading.value)
        assertEquals(mockResult, viewModel.ocrScanningState.value)
        coVerify { mockRepository.addEventLog(type = "OCR_SCAN_SUCCESS", any(), any(), any(), any()) }
        unmockkObject(OcrCardScanner)
    }

    @Test
    fun `onRealOcrDetected handles stability logic`() = runTest {
        val lines = listOf(OcrLine("ID-123", 10, 10, 100, 20))
        val mockResult = ScannedStaffCardResult("FIRST", "LAST", "ID-123", "DEPT")
        
        mockkObject(OcrCardScanner)
        every { OcrCardScanner.parseStaffCardText(any<List<OcrLine>>()) } returns mockResult
        coEvery { mockRepository.addEventLog(any(), any(), any(), any(), any()) } just Runs

        // Call multiple times to reach stability threshold (5)
        repeat(5) {
            viewModel.onRealOcrDetected(lines, 1000, 1000)
        }
        
        assertEquals(1.0f, viewModel.ocrStability.value)
        assertEquals(mockResult, viewModel.ocrScanningState.value)
        
        unmockkObject(OcrCardScanner)
    }

    @Test
    fun `clearOcrResult resets state`() = runTest {
        viewModel.clearOcrResult()
        
        assertNull(viewModel.ocrScanningState.value)
        assertEquals(0f, viewModel.ocrStability.value)
        assertTrue(viewModel.detectedLines.value.isEmpty())
    }

    @Test
    fun `addNoteToEventLog calls repository`() = runTest {
        coEvery { mockEventLogDao.updateNote(any(), any()) } just Runs
        
        viewModel.addNoteToEventLog(1L, "New Note")
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { mockEventLogDao.updateNote(1L, "New Note") }
    }

    @Test
    fun `submitLeaveRequest calls repository`() = runTest {
        coEvery { mockLeaveRequestDao.insertLeaveRequest(any()) } returns 1L
        
        viewModel.submitLeaveRequest("Annual", "2026-01-01", "2026-01-02", "Vacation")
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { mockLeaveRequestDao.insertLeaveRequest(any()) }
    }

    @Test
    fun `deleteGeofence calls repository`() = runTest {
        coEvery { mockRepository.deleteGeofence(any()) } just Runs
        
        viewModel.deleteGeofence(1L)
        
        // Wait for coroutine to complete since it's launched in viewModelScope
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { mockRepository.deleteGeofence(1L) }
    }

    @Test
    fun `addGeofenceZone prevents duplicates by name`() = runTest {
        val existing = GeofenceZoneEntity(id = 1, name = "ZONE1", centerLat = 41.0, centerLng = 29.0)
        allGeofencesFlow.value = listOf(existing)
        
        viewModel.addGeofenceZone("ZONE1", 42.0, 30.0, 500.0)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify(exactly = 0) { mockGeofenceDao.insertGeofence(any()) }
    }

    @Test
    fun `addGeofenceZone prevents duplicates by proximity`() = runTest {
        val existing = GeofenceZoneEntity(id = 1, name = "ZONE1", centerLat = 41.0, centerLng = 29.0)
        allGeofencesFlow.value = listOf(existing)
        
        // Mock distance calculation - 5 meters apart
        every { mockRepository.calculateDistanceInMeters(any(), any(), any(), any()) } returns 5.0
        
        viewModel.addGeofenceZone("NEW ZONE", 41.00001, 29.00001, 500.0)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify(exactly = 0) { mockGeofenceDao.insertGeofence(any()) }
    }

    @Test
    fun `updateGeofenceZone updates existing zone`() = runTest {
        val existing = GeofenceZoneEntity(id = 10, name = "OLD", centerLat = 41.0, centerLng = 29.0)
        allGeofencesFlow.value = listOf(existing)
        
        viewModel.updateGeofenceZone(10L, "NEW NAME", 800.0)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { 
            mockGeofenceDao.insertGeofence(match { 
                it.id == 10L && it.name == "NEW NAME" && it.radiusMeters == 800.0 
            }) 
        }
    }
}
