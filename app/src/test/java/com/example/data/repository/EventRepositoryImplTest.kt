package com.example.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.dao.EventLogDao
import com.example.util.NotificationService
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
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
class EventRepositoryImplTest {

    private lateinit var app: Application
    @MockK lateinit var mockEventLogDao: EventLogDao
    @MockK lateinit var mockNotificationService: NotificationService

    private lateinit var eventRepository: EventRepositoryImpl

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        app = ApplicationProvider.getApplicationContext()
        
        every { mockNotificationService.sendPrivacySafeAlert(any(), any()) } just Runs
        every { mockEventLogDao.getAllEventLogs() } returns emptyFlow()
        coEvery { mockEventLogDao.insertEventLog(any()) } returns 1L

        eventRepository = EventRepositoryImpl(
            app,
            mockEventLogDao,
            mockNotificationService
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `addEventLog inserts to dao and sends notification`() = runTest {
        eventRepository.addEventLog("TYPE", "TITLE", "DETAIL", "STATUS", true)
        
        coVerify { mockEventLogDao.insertEventLog(match { it.type == "TYPE" && it.title == "TITLE" }) }
        verify { mockNotificationService.sendPrivacySafeAlert(any(), "TITLE") }
    }
}
