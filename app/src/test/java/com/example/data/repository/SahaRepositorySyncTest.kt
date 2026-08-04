package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.EventLogEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.remote.MockSyncApi
import com.example.data.remote.SyncResponse
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SahaRepositorySyncTest {

    private lateinit var context: Context
    @MockK lateinit var mockDb: AppDatabase
    @MockK lateinit var mockLocationDao: LocationDao
    @MockK lateinit var mockEventLogDao: EventLogDao
    @MockK lateinit var mockGeofenceDao: GeofenceDao
    @MockK lateinit var mockUserDao: UserDao
    @MockK lateinit var mockOfflineDao: OfflineActivityReportDao
    @MockK lateinit var mockLeaveRequestDao: LeaveRequestDao
    @MockK lateinit var mockPrefs: com.example.data.local.PreferencesManager
    @MockK lateinit var mockSyncApi: MockSyncApi

    private lateinit var repository: SahaRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()
        
        every { mockDb.locationDao() } returns mockLocationDao
        every { mockDb.eventLogDao() } returns mockEventLogDao
        every { mockDb.geofenceDao() } returns mockGeofenceDao
        every { mockDb.userDao() } returns mockUserDao
        every { mockDb.offlineActivityReportDao() } returns mockOfflineDao
        every { mockDb.leaveRequestDao() } returns mockLeaveRequestDao
        
        // Mock Flow inits
        every { mockLocationDao.getLatestLocation() } returns emptyFlow()
        every { mockEventLogDao.getAllEventLogs() } returns emptyFlow()
        every { mockGeofenceDao.getAllGeofences() } returns emptyFlow()
        every { mockUserDao.getUserProfile() } returns emptyFlow()
        every { mockLeaveRequestDao.getAllLeaveRequests() } returns emptyFlow()
        
        // Mock init calls
        coEvery { mockUserDao.insertOrUpdateUser(any()) } just Runs
        coEvery { mockLocationDao.getUnsyncedLocations() } returns emptyList()
        coEvery { mockEventLogDao.getUnsyncedLogs() } returns emptyList()
        coEvery { mockOfflineDao.getUnsyncedReports() } returns emptyList()

        repository = SahaRepository(
            context,
            mockLocationDao,
            mockEventLogDao,
            mockLeaveRequestDao,
            mockGeofenceDao,
            mockUserDao,
            mockOfflineDao,
            mockPrefs,
            mockSyncApi
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `performOfflineSync marks data as synced on success`() = runTest {
        val location = LocationEntity(id = 1, latitude = 0.0, longitude = 0.0, speed = 0f, accuracy = 0f, batteryLevel = 0, address = "", timestamp = 0, isSynced = false)
        val log = EventLogEntity(id = 1, type = "TEST", title = "", detail = "", isSensitive = false, status = "", timestamp = 0, isSynced = false)
        
        coEvery { mockLocationDao.getUnsyncedLocations() } returns listOf(location)
        coEvery { mockEventLogDao.getUnsyncedLogs() } returns listOf(log)
        coEvery { mockOfflineDao.getUnsyncedReports() } returns emptyList()
        
        coEvery { mockSyncApi.syncOfflineData(any()) } returns SyncResponse(
            success = true,
            syncedLocationCount = 1,
            syncedLogCount = 1,
            syncedReportCount = 0,
            syncBatchId = "ID",
            serverTimestamp = 123L,
            message = "Success"
        )
        
        coEvery { mockLocationDao.markAsSynced(any()) } just Runs
        coEvery { mockEventLogDao.markAsSynced(any()) } just Runs
        coEvery { mockEventLogDao.insertEventLog(any()) } returns 1L

        val result = repository.performOfflineSync()

        assertTrue(result)
        coVerify { mockLocationDao.markAsSynced(listOf(1L)) }
        coVerify { mockEventLogDao.markAsSynced(listOf(1L)) }
        coVerify { mockEventLogDao.insertEventLog(match { it.type == "SYNC_SUCCESS" }) }
    }
}
