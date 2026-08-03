package com.example.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BiometricPromptManagerTest {

    private lateinit var context: Context
    private lateinit var biometricPromptManager: BiometricPromptManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        biometricPromptManager = BiometricPromptManager(context)
        ShadowLog.stream = System.out
    }

    @Test
    fun `checkBiometricAvailability returns Unavailable when no hardware`() {
        val mockBiometricManager = io.mockk.mockk<BiometricManager>()
        io.mockk.mockkStatic(BiometricManager::class)
        io.mockk.every { BiometricManager.from(any()) } returns mockBiometricManager
        io.mockk.every { mockBiometricManager.canAuthenticate(any()) } returns BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        
        val status = biometricPromptManager.checkBiometricAvailability()
        
        assertTrue(status is BiometricStatus.Unavailable)
        org.junit.Assert.assertEquals("Cihazda biyometrik donanım (parmak izi / yüz tanıma) bulunmuyor.", (status as BiometricStatus.Unavailable).reason)
    }

    @Test
    fun `checkBiometricAvailability returns Available when hardware is present and enrolled`() {
        val mockBiometricManager = io.mockk.mockk<BiometricManager>()
        io.mockk.mockkStatic(BiometricManager::class)
        io.mockk.every { BiometricManager.from(any()) } returns mockBiometricManager
        io.mockk.every { mockBiometricManager.canAuthenticate(any()) } returns BiometricManager.BIOMETRIC_SUCCESS
        
        val status = biometricPromptManager.checkBiometricAvailability()
        
        assertTrue(status is BiometricStatus.Available)
    }
}
