package com.sahatakip.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PreferencesManagerTest {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        preferencesManager = PreferencesManager(context)
    }

    @Test
    fun `setLanguage updates StateFlow and SharedPreferences`() = runTest {
        preferencesManager.setLanguage("en")
        
        // Verify StateFlow
        assertEquals("en", preferencesManager.language.value)
        assertEquals("en", preferencesManager.language.first())
        
        // Verify SharedPrefs directly
        val prefs = context.getSharedPreferences("saha_preferences", Context.MODE_PRIVATE)
        assertEquals("en", prefs.getString(PreferencesManager.KEY_LANGUAGE, null))
    }

    @Test
    fun `setUpdateInterval updates StateFlow correctly`() = runTest {
        preferencesManager.setUpdateInterval(120)
        assertEquals(120, preferencesManager.updateInterval.value)
    }

    @Test
    fun `initial values are loaded correctly from defaults`() = runTest {
        assertEquals("tr", preferencesManager.language.value)
        assertEquals(60, preferencesManager.updateInterval.value)
        assertEquals("system", preferencesManager.theme.value)
    }
}
