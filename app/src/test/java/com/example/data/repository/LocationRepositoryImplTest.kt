package com.example.data.repository

import com.example.data.local.dao.LocationDao
import com.example.domain.repository.GeofenceRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationRepositoryImplTest {

    @MockK lateinit var mockLocationDao: LocationDao
    @MockK lateinit var mockGeofenceRepository: GeofenceRepository

    private lateinit var locationRepository: LocationRepositoryImpl

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        every { mockLocationDao.getLatestLocation() } returns emptyFlow()
        coEvery { mockLocationDao.insertLocation(any()) } returns 1L
        coEvery { mockGeofenceRepository.checkGeofenceBreach(any(), any()) } returns null

        locationRepository = LocationRepositoryImpl(
            mockLocationDao,
            mockGeofenceRepository
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `recordNewLocation inserts to dao and checks geofence`() = runTest {
        val lat = 41.0
        val lng = 29.0
        
        locationRepository.recordNewLocation(lat, lng, 0f, 5f, 80, "Test Address")
        
        coVerify { mockLocationDao.insertLocation(match { it.latitude == lat && it.longitude == lng }) }
        coVerify { mockGeofenceRepository.checkGeofenceBreach(lat, lng) }
    }
}
