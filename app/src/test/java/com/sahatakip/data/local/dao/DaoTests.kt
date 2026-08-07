package com.sahatakip.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sahatakip.data.local.AppDatabase
import com.sahatakip.data.local.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DaoTests {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var locationDao: LocationDao
    private lateinit var eventLogDao: EventLogDao
    private lateinit var geofenceDao: GeofenceDao
    private lateinit var leaveRequestDao: LeaveRequestDao
    private lateinit var offlineDao: OfflineActivityReportDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = db.userDao()
        locationDao = db.locationDao()
        eventLogDao = db.eventLogDao()
        geofenceDao = db.geofenceDao()
        leaveRequestDao = db.leaveRequestDao()
        offlineDao = db.offlineActivityReportDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testUserDao() = runBlocking {
        val user = UserProfileEntity(id = 1, firstName = "Test", lastName = "User", isActivated = true)
        userDao.insertOrUpdateUser(user)
        
        val retrieved = userDao.getUserProfile().first()
        assertEquals("Test", retrieved?.firstName)
        assertTrue(retrieved?.isActivated == true)

        userDao.updateLastLogin(123456789L)
        val afterLogin = userDao.getUserProfile().first()
        assertEquals(123456789L, afterLogin?.lastLoginAt)

        userDao.updateBiometricPreference(false)
        val afterBio = userDao.getUserProfile().first()
        assertFalse(afterBio?.isBiometricEnabled == true)

        userDao.deactivateUser()
        val afterDeactivate = userDao.getUserProfile().first()
        assertFalse(afterDeactivate?.isActivated == true)
    }

    @Test
    fun testLocationDao() = runBlocking {
        val loc1 = LocationEntity(latitude = 41.0, longitude = 29.0, timestamp = 1000L, isSynced = false)
        val loc2 = LocationEntity(latitude = 41.1, longitude = 29.1, timestamp = 2000L, isSynced = false)
        
        locationDao.insertLocation(loc1)
        locationDao.insertLocation(loc2)

        val all = locationDao.getAllLocations().first()
        assertEquals(2, all.size)
        assertEquals(2000L, all[0].timestamp) // Sorted by timestamp DESC

        val latest = locationDao.getLatestLocation().first()
        assertEquals(2000L, latest?.timestamp)

        val since = locationDao.getLocationsSince(1500L).first()
        assertEquals(1, since.size)
        assertEquals(2000L, since[0].timestamp)

        val unsynced = locationDao.getUnsyncedLocations()
        assertEquals(2, unsynced.size)

        locationDao.markAsSynced(listOf(unsynced[0].id))
        assertEquals(1, locationDao.getUnsyncedLocations().size)

        locationDao.clearAll()
        assertTrue(locationDao.getAllLocations().first().isEmpty())
    }

    @Test
    fun testEventLogDao() = runBlocking {
        val log = EventLogEntity(type = "INFO", title = "Test", detail = "Test Detail", timestamp = 1000L)
        val id = eventLogDao.insertEventLog(log)

        val all = eventLogDao.getAllEventLogs().first()
        assertEquals(1, all.size)
        assertEquals("Test", all[0].title)

        eventLogDao.updateNote(id, "Updated Note")
        val updated = eventLogDao.getAllEventLogs().first()
        assertEquals("Updated Note", updated[0].note)

        val unsynced = eventLogDao.getUnsyncedLogs()
        assertEquals(1, unsynced.size)

        eventLogDao.markAsSynced(listOf(id))
        assertTrue(eventLogDao.getUnsyncedLogs().isEmpty())

        eventLogDao.clearAll()
        assertTrue(eventLogDao.getAllEventLogs().first().isEmpty())
    }

    @Test
    fun testGeofenceDao() = runBlocking {
        val zone = GeofenceZoneEntity(name = "Zone1", centerLat = 41.0, centerLng = 29.0, radiusMeters = 100.0, isActive = true)
        val id = geofenceDao.insertGeofence(zone)

        val all = geofenceDao.getAllGeofences().first()
        assertEquals(1, all.size)
        assertEquals("Zone1", all[0].name)

        val active = geofenceDao.getActiveGeofences()
        assertEquals(1, active.size)

        geofenceDao.setGeofenceActive(id, false)
        assertTrue(geofenceDao.getActiveGeofences().isEmpty())

        geofenceDao.deleteById(id)
        assertTrue(geofenceDao.getAllGeofences().first().isEmpty())
    }

    @Test
    fun testLeaveRequestDao() = runBlocking {
        val request = LeaveRequestEntity(startDate = "2026-01-01", endDate = "2026-01-02", requestType = "Annual", reason = "Vacation", status = "PENDING")
        val id = leaveRequestDao.insertLeaveRequest(request)

        val all = leaveRequestDao.getAllLeaveRequests().first()
        assertEquals(1, all.size)

        leaveRequestDao.updateStatus(id, "APPROVED")
        val updated = leaveRequestDao.getAllLeaveRequests().first()
        assertEquals("APPROVED", updated[0].status)

        leaveRequestDao.deleteById(id)
        assertTrue(leaveRequestDao.getAllLeaveRequests().first().isEmpty())
    }

    @Test
    fun testOfflineReportDao() = runBlocking {
        val report = OfflineActivityReportEntity(title = "Test", description = "Offline report", timestamp = 1000L, isSynced = false)
        val id = offlineDao.insertReport(report)

        val all = offlineDao.getAllReports().first()
        assertEquals(1, all.size)

        val unsynced = offlineDao.getUnsyncedReports()
        assertEquals(1, unsynced.size)

        offlineDao.markAsSynced(listOf(id))
        assertTrue(offlineDao.getUnsyncedReports().isEmpty())

        offlineDao.clearAll()
        assertTrue(offlineDao.getAllReports().first().isEmpty())
    }
}
