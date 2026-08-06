package com.example.data.repository

import com.example.data.local.PreferencesManager
import com.example.data.local.dao.OfflineActivityReportDao
import com.example.data.local.entity.EventLogEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.remote.MockSyncApi
import com.example.data.remote.SyncResponse
import com.example.domain.repository.EventRepository
import com.example.domain.repository.LocationRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
        every { mockPreferencesManager.language } returns MutableStateFlow("tr")

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
