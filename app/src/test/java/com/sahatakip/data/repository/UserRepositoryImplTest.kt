package com.sahatakip.data.repository

import com.sahatakip.data.local.PreferencesManager
import com.sahatakip.data.local.dao.UserDao
import com.sahatakip.domain.repository.EventRepository
import com.sahatakip.domain.repository.GeofenceRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryImplTest {

    @MockK lateinit var mockUserDao: UserDao
    @MockK lateinit var mockEventRepository: EventRepository
    @MockK lateinit var mockGeofenceRepository: GeofenceRepository
    @MockK lateinit var mockPreferencesManager: PreferencesManager

    private lateinit var userRepository: UserRepositoryImpl

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        every { mockUserDao.getUserProfile() } returns MutableStateFlow(null)
        coEvery { mockUserDao.insertOrUpdateUser(any()) } just Runs
        coEvery { mockUserDao.updateLastLogin() } just Runs
        coEvery { mockUserDao.deactivateUser() } just Runs
        coEvery { mockEventRepository.addEventLog(any(), any(), any(), any(), any()) } just Runs
        coEvery { mockGeofenceRepository.getAllGeofencesOnce() } returns emptyList()
        coEvery { mockGeofenceRepository.insertGeofence(any()) } returns 1L
        every { mockPreferencesManager.language } returns MutableStateFlow("tr")

        userRepository = UserRepositoryImpl(
            mockUserDao,
            mockEventRepository,
            mockGeofenceRepository,
            mockPreferencesManager
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `deactivateUser calls dao and logs event`() = runTest {
        userRepository.deactivateUser()
        
        coVerify { mockUserDao.deactivateUser() }
        coVerify { mockEventRepository.addEventLog(type = "APP_DEACTIVATED", any(), any(), any(), any()) }
    }
}
