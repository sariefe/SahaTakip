package com.sahatakip.data.repository

import com.sahatakip.data.local.PreferencesManager
import com.sahatakip.data.local.dao.OfflineActivityReportDao
import com.sahatakip.data.local.entity.EventLogEntity
import com.sahatakip.data.local.entity.LocationEntity
import com.sahatakip.data.remote.MockSyncApi
import com.sahatakip.data.remote.SyncResponse
import com.sahatakip.domain.repository.EventRepository
import com.sahatakip.domain.repository.LocationRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class SyncRepositoryImplTest {

    @MockK lateinit var mockLocationRepository: LocationRepository
    @MockK lateinit var mockEventRepository: EventRepository
    @MockK lateinit var mockOfflineDao: OfflineActivityReportDao
    @MockK lateinit var mockSyncApi: MockSyncApi
    @MockK lateinit var mockPreferencesManager: PreferencesManager

    private lateinit var syncRepository: SyncRepositoryImpl

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        coEvery { mockLocationRepository.getUnsyncedLocations() } returns emptyList()
        coEvery { mockEventRepository.getUnsyncedLogs() } returns emptyList()
        coEvery { mockOfflineDao.getUnsyncedReports() } returns emptyList()
        
        // Use relaxed mock for preferences manager to avoid "no answer found" issues
        mockPreferencesManager = mockk(relaxed = true)
        every { mockPreferencesManager.language } returns MutableStateFlow("tr")
        every { mockPreferencesManager.deviceId } returns MutableStateFlow("test-device-id")

        syncRepository = SyncRepositoryImpl(
            mockLocationRepository,
            mockEventRepository,
            mockOfflineDao,
            mockSyncApi,
            mockPreferencesManager
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
        
        coEvery { mockLocationRepository.getUnsyncedLocations() } returns listOf(location)
        coEvery { mockEventRepository.getUnsyncedLogs() } returns listOf(log)
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
        
        coEvery { mockLocationRepository.markLocationsAsSynced(any()) } just Runs
        coEvery { mockEventRepository.markLogsAsSynced(any()) } just Runs
        coEvery { mockEventRepository.insertEventLog(any()) } returns 1L

        val result = syncRepository.performOfflineSync()

        assertTrue(result)
        coVerify { mockLocationRepository.markLocationsAsSynced(listOf(1L)) }
        coVerify { mockEventRepository.markLogsAsSynced(listOf(1L)) }
        coVerify { mockEventRepository.insertEventLog(match { it.type == "SYNC_SUCCESS" }) }
    }
}
