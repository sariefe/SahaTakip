package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.dao.EventLogDao
import com.example.data.local.dao.GeofenceDao
import com.example.data.local.dao.LocationDao
import com.example.data.local.dao.OfflineActivityReportDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.util.NotificationHelper
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SahaRepositoryAdvancedTest {

    private lateinit var context: Context
    @MockK
    lateinit var mockDb: AppDatabase
    @MockK
    lateinit var mockLocationDao: LocationDao
    @MockK
    lateinit var mockEventLogDao: EventLogDao
    @MockK
    lateinit var mockLeaveRequestDao: com.example.data.local.dao.LeaveRequestDao
    @MockK
    lateinit var mockGeofenceDao: GeofenceDao
    @MockK
    lateinit var mockUserDao: UserDao
    @MockK
    lateinit var mockOfflineDao: OfflineActivityReportDao

    private lateinit var repository: SahaRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()
        
        every { mockDb.locationDao() } returns mockLocationDao
        every { mockDb.eventLogDao() } returns mockEventLogDao
        every { mockDb.leaveRequestDao() } returns mockLeaveRequestDao
        every { mockDb.geofenceDao() } returns mockGeofenceDao
        every { mockDb.userDao() } returns mockUserDao
        every { mockDb.offlineActivityReportDao() } returns mockOfflineDao
        
        // Mock DAO Flow methods called in SahaRepository initialization
        every { mockLocationDao.getLatestLocation() } returns kotlinx.coroutines.flow.emptyFlow()
        every { mockEventLogDao.getAllEventLogs() } returns kotlinx.coroutines.flow.emptyFlow()
        every { mockLeaveRequestDao.getAllLeaveRequests() } returns kotlinx.coroutines.flow.emptyFlow()
        every { mockGeofenceDao.getAllGeofences() } returns kotlinx.coroutines.flow.emptyFlow()
        every { mockUserDao.getUserProfile() } returns kotlinx.coroutines.flow.emptyFlow()
        
        // Mock methods called during SahaRepository.init -> initializeAndSyncDefaultData
        coEvery { mockUserDao.insertOrUpdateUser(any()) } just Runs
        coEvery { mockLocationDao.getUnsyncedLocations() } returns emptyList()
        coEvery { mockEventLogDao.getUnsyncedLogs() } returns emptyList()
        coEvery { mockOfflineDao.getUnsyncedReports() } returns emptyList()
        
        // Mock NotificationHelper
        mockkObject(NotificationHelper)
        every { NotificationHelper.sendPrivacySafeAlert(any(), any()) } just Runs

        repository = SahaRepository(context, mockDb)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `recordNewLocation triggers geofence breach when outside active zone`() = runTest {
        val lat = 41.0
        val lng = 29.0

        val zone = GeofenceZoneEntity(
            id = 1,
            name = "Safe Zone",
            centerLat = 40.0,
            centerLng = 28.0,
            radiusMeters = 1000.0,
            isActive = true
        )
        
        coEvery { mockGeofenceDao.getActiveGeofences() } returns listOf(zone)
        coEvery { mockLocationDao.insertLocation(any()) } returns 1L
        coEvery { mockEventLogDao.insertEventLog(any()) } returns 1L

        repository.recordNewLocation(lat, lng)

        coVerify { mockEventLogDao.insertEventLog(match { it.type == "GEOFENCE_VIOLATION" }) }
        verify { NotificationHelper.sendPrivacySafeAlert(any(), any()) }
    }

    @Test
    fun `recordNewLocation does NOT trigger breach when inside active zone`() = runTest {
        val lat = 41.0
        val lng = 29.0

        val zone = GeofenceZoneEntity(
            id = 1,
            name = "Safe Zone",
            centerLat = 41.0,
            centerLng = 29.0,
            radiusMeters = 1000.0,
            isActive = true
        )
        
        coEvery { mockGeofenceDao.getActiveGeofences() } returns listOf(zone)
        coEvery { mockLocationDao.insertLocation(any()) } returns 1L


        repository.recordNewLocation(lat, lng)

        coVerify(exactly = 0) { mockEventLogDao.insertEventLog(any()) }
        verify(exactly = 0) { NotificationHelper.sendPrivacySafeAlert(any(), any()) }
    }
}
