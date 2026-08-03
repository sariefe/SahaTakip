package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.EventLogEntity
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.data.local.entity.LeaveRequestEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.local.entity.OfflineActivityReportEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.util.NotificationHelper
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SahaRepositoryCriticalTest {

    private lateinit var repository: SahaRepository
    
    @MockK lateinit var mockDb: AppDatabase
    @MockK lateinit var mockLocationDao: LocationDao
    @MockK lateinit var mockEventLogDao: EventLogDao
    @MockK lateinit var mockOfflineReportDao: OfflineActivityReportDao
    @MockK lateinit var mockGeofenceDao: GeofenceDao
    @MockK lateinit var mockUserDao: UserDao
    @MockK lateinit var mockLeaveRequestDao: LeaveRequestDao

    private val userProfileFlow = MutableStateFlow<UserProfileEntity?>(null)
    private val latestLocationFlow = MutableStateFlow<LocationEntity?>(null)
    private val allEventLogsFlow = MutableStateFlow<List<EventLogEntity>>(emptyList())
    private val allLeaveRequestsFlow = MutableStateFlow<List<LeaveRequestEntity>>(emptyList())
    private val allGeofencesFlow = MutableStateFlow<List<GeofenceZoneEntity>>(emptyList())

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Mock DAO flows BEFORE Repository initialization
        every { mockLocationDao.getLatestLocation() } returns latestLocationFlow
        every { mockEventLogDao.getAllEventLogs() } returns allEventLogsFlow
        every { mockLeaveRequestDao.getAllLeaveRequests() } returns allLeaveRequestsFlow
        every { mockGeofenceDao.getAllGeofences() } returns allGeofencesFlow
        every { mockUserDao.getUserProfile() } returns userProfileFlow

        every { mockDb.locationDao() } returns mockLocationDao
        every { mockDb.eventLogDao() } returns mockEventLogDao
        every { mockDb.offlineActivityReportDao() } returns mockOfflineReportDao
        every { mockDb.geofenceDao() } returns mockGeofenceDao
        every { mockDb.userDao() } returns mockUserDao
        every { mockDb.leaveRequestDao() } returns mockLeaveRequestDao
        
        coEvery { mockLocationDao.getUnsyncedLocations() } returns emptyList()
        coEvery { mockEventLogDao.getUnsyncedLogs() } returns emptyList()
        coEvery { mockOfflineReportDao.getUnsyncedReports() } returns emptyList()
        coEvery { mockGeofenceDao.getActiveGeofences() } returns emptyList()
        coEvery { mockUserDao.insertOrUpdateUser(any()) } just Runs
        coEvery { mockEventLogDao.insertEventLog(any()) } returns 1L

        mockkObject(NotificationHelper)
        every { NotificationHelper.sendPrivacySafeAlert(any(), any()) } just Runs
        
        // DAOs and their flows are mocked now, so properties in SahaRepository will be initialized correctly
        repository = SahaRepository(context, mockDb)
    }

    @Test
    fun `recordNewLocation inserts location and checks geofence`() = runTest {
        coEvery { mockLocationDao.insertLocation(any()) } returns 1L
        coEvery { mockGeofenceDao.getActiveGeofences() } returns emptyList()
        
        val id = repository.recordNewLocation(41.0, 29.0)
        
        assertTrue(id == 1L)
        coVerify { mockLocationDao.insertLocation(any()) }
        coVerify { mockGeofenceDao.getActiveGeofences() }
    }

    @Test
    fun `performOfflineSync returns true when nothing to sync`() = runTest {
        coEvery { mockLocationDao.getUnsyncedLocations() } returns emptyList()
        coEvery { mockEventLogDao.getUnsyncedLogs() } returns emptyList()
        coEvery { mockOfflineReportDao.getUnsyncedReports() } returns emptyList()
        
        val result = repository.performOfflineSync()
        
        assertTrue(result)
    }

    @Test
    fun `performOfflineSync marks as synced on success`() = runTest {
        val loc = LocationEntity(id = 1, latitude = 0.0, longitude = 0.0)
        val log = EventLogEntity(id = 2, type = "T", title = "T", detail = "D")
        val report = OfflineActivityReportEntity(id = 3, title = "T", description = "D")
        
        coEvery { mockLocationDao.getUnsyncedLocations() } returns listOf(loc)
        coEvery { mockEventLogDao.getUnsyncedLogs() } returns listOf(log)
        coEvery { mockOfflineReportDao.getUnsyncedReports() } returns listOf(report)
        
        coEvery { mockLocationDao.markAsSynced(any()) } just Runs
        coEvery { mockEventLogDao.markAsSynced(any()) } just Runs
        coEvery { mockOfflineReportDao.markAsSynced(any()) } just Runs
        coEvery { mockEventLogDao.insertEventLog(any()) } returns 1L
        
        val result = repository.performOfflineSync()
        
        assertTrue(result)
        coVerify { mockLocationDao.markAsSynced(listOf(1L)) }
        coVerify { mockEventLogDao.markAsSynced(listOf(2L)) }
        coVerify { mockOfflineReportDao.markAsSynced(listOf(3L)) }
    }

    @Test
    fun `initializeAndSyncDefaultData seeds data if user missing`() = runTest {
        userProfileFlow.value = null
        coEvery { mockUserDao.insertOrUpdateUser(any()) } just Runs
        coEvery { mockGeofenceDao.getActiveGeofences() } returns emptyList()
        coEvery { mockGeofenceDao.insertGeofence(any()) } returns 1L
        coEvery { mockLocationDao.getUnsyncedLocations() } returns listOf(LocationEntity(latitude = 0.0, longitude = 0.0))
        coEvery { mockEventLogDao.insertEventLog(any()) } returns 1L
        coEvery { mockLeaveRequestDao.insertLeaveRequest(any()) } returns 1L
        coEvery { mockOfflineReportDao.insertReport(any()) } returns 1L
        coEvery { mockLocationDao.insertLocation(any()) } returns 1L

        repository.initializeAndSyncDefaultData()
        
        coVerify { mockUserDao.insertOrUpdateUser(any()) }
        coVerify(atLeast = 1) { mockGeofenceDao.insertGeofence(any()) }
    }
}
