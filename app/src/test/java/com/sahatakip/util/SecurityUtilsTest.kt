package com.sahatakip.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityUtilsTest {

    @Test
    fun `checkIsDeviceRooted returns true if Build TAGS contains test-keys`() = runTest {
        // Mock Build.TAGS
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "TAGS", "test-keys")
        
        val isRooted = SecurityUtils.checkIsDeviceRooted()
        assertTrue(isRooted)
    }

    @Test
    fun `checkIsDeviceRooted returns false for clean device`() = runTest {
        // Mock Build.TAGS to something safe
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "TAGS", "release-keys")
        
        // Note: This test assumes /system/bin/su etc. don't exist in the Robolectric test environment.
        val isRooted = SecurityUtils.checkIsDeviceRooted()
        assertFalse(isRooted)
    }
}
