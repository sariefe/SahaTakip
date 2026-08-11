package com.sahatakip.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.sahatakip.data.local.PreferencesManager
import com.sahatakip.data.local.entity.UserProfileEntity
import com.sahatakip.domain.repository.EventRepository
import com.sahatakip.domain.repository.UserRepository
import com.sahatakip.util.OcrCardScanner
import com.sahatakip.util.OcrLine
import com.sahatakip.util.ScannedStaffCardResult
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthViewModelTest {

    private lateinit var app: Application
    private lateinit var viewModel: AuthViewModel

    @MockK lateinit var userRepository: UserRepository
    @MockK lateinit var eventRepository: EventRepository
    @MockK lateinit var preferencesManager: PreferencesManager

    private val userProfileFlow = MutableStateFlow<UserProfileEntity?>(null)
    private val dynamicCodeFlow = MutableStateFlow<String?>(null)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()

        every { userRepository.userProfile } returns userProfileFlow
        every { preferencesManager.dynamicActivationCode } returns dynamicCodeFlow.asStateFlow()
        coEvery { userRepository.initializeAndSyncDefaultData() } just Runs
        coEvery { userRepository.updateLastLogin() } just Runs
        coEvery { userRepository.insertOrUpdateUser(any()) } just Runs
        coEvery { eventRepository.addEventLog(any(), any(), any(), any(), any()) } just Runs

        viewModel = AuthViewModel(userRepository, eventRepository, preferencesManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `activateWithCode returns true for correct code`() = runTest {
        val result = viewModel.activateWithCode("123456")
        assertTrue(result)
        assertTrue(viewModel.isAuthenticated.value)
        coVerify { userRepository.insertOrUpdateUser(any()) }
    }

    @Test
    fun `activateWithCode returns false for incorrect code`() = runTest {
        val result = viewModel.activateWithCode("WRONG")
        assertFalse(result)
        assertFalse(viewModel.isAuthenticated.value)
        org.junit.Assert.assertNotNull(viewModel.authErrorMessage.value)
    }

    @Test
    fun `authenticateWithBiometrics updates state`() = runTest {
        val result = viewModel.authenticateWithBiometrics()
        assertTrue(result)
        assertTrue(viewModel.isAuthenticated.value)
    }

    @Test
    fun `authenticateWithOcr returns true when ID matches`() = runTest {
        val user = UserProfileEntity(staffId = "ID-123")
        userProfileFlow.value = user
        
        val result = viewModel.authenticateWithOcr("ID-123")
        assertTrue(result)
        assertTrue(viewModel.isAuthenticated.value)
    }

    @Test
    fun `onRealOcrDetected handles stability logic`() = runTest {
        val lines = listOf(OcrLine("ID-123", 50, 10, 100, 20))
        val mockResult = ScannedStaffCardResult("FIRST", "LAST", "ID-123", "DEPT")
        
        mockkObject(OcrCardScanner)
        every { OcrCardScanner.parseStaffCardText(any<List<OcrLine>>()) } returns mockResult

        repeat(5) {
            viewModel.onRealOcrDetected(lines, 1000, 1000)
        }
        
        assertEquals(1.0f, viewModel.ocrStability.value)
        assertEquals(mockResult, viewModel.ocrScanningState.value)
        unmockkObject(OcrCardScanner)
    }

    @Test
    fun `logout calls repository and resets state`() = runTest {
        coEvery { userRepository.deactivateUser() } just Runs
        viewModel.authenticateWithBiometrics()
        assertTrue(viewModel.isAuthenticated.value)

        viewModel.logout()
        assertFalse(viewModel.isAuthenticated.value)
        coVerify { userRepository.deactivateUser() }
    }
}
