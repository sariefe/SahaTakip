package com.example.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.domain.repository.EventRepository
import com.example.domain.repository.LeaveRepository
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RequestLogViewModelTest {

    private lateinit var app: Application
    private lateinit var viewModel: RequestLogViewModel

    @MockK lateinit var eventRepository: EventRepository
    @MockK lateinit var leaveRepository: LeaveRepository

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()

        every { eventRepository.allEventLogs } returns MutableStateFlow(emptyList())
        every { leaveRepository.allLeaveRequests } returns MutableStateFlow(emptyList())

        viewModel = RequestLogViewModel(eventRepository, leaveRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `addNoteToEventLog calls repository`() = runTest {
        coEvery { eventRepository.updateNote(any(), any()) } just Runs
        viewModel.addNoteToEventLog(1L, "Test Note")
        coVerify { eventRepository.updateNote(1L, "Test Note") }
    }

    @Test
    fun `submitLeaveRequest calls repository`() = runTest {
        coEvery { leaveRepository.insertLeaveRequest(any()) } returns 1L
        viewModel.submitLeaveRequest("Annual", "2026-01-01", "2026-01-02", "Vacation")
        coVerify { leaveRepository.insertLeaveRequest(any()) }
    }

    @Test
    fun `deleteLeaveRequest calls repository`() = runTest {
        coEvery { leaveRepository.deleteLeaveRequest(any()) } just Runs
        viewModel.deleteLeaveRequest(100L)
        coVerify { leaveRepository.deleteLeaveRequest(100L) }
    }
}
