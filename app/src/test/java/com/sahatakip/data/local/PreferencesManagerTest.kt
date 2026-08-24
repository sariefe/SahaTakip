package com.sahatakip.data.local

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PreferencesManagerTest {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        val testFile = File(context.filesDir, "test_datastore_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { testFile }
        )
        
        preferencesManager = PreferencesManager(context, dataStore)
    }

    @Test
    fun `setLanguage updates StateFlow`() = runTest {
        preferencesManager.setLanguage("en")

        val result = preferencesManager.language.first { it == "en" }
        assertEquals("en", result)
    }

    @Test
    fun `setUpdateInterval updates StateFlow correctly`() = runTest {
        preferencesManager.setUpdateInterval(120)
        val result = preferencesManager.updateInterval.first { it == 120 }
        assertEquals(120, result)
    }

    @Test
    @Config(qualifiers = "tr")
    fun `initial values are loaded correctly from defaults`() = runTest {
        assertEquals("tr", preferencesManager.language.value)
        assertEquals(60, preferencesManager.updateInterval.value)
        assertEquals("system", preferencesManager.theme.value)
    }

    @Test
    @Config(qualifiers = "en")
    fun `initial values default to en for non-tr systems`() = runTest {
        // Resetting preference manager to pick up new locale from @Config
        preferencesManager = PreferencesManager(context, PreferenceDataStoreFactory.create(
            produceFile = { File(context.filesDir, "test_datastore_en_${System.nanoTime()}.preferences_pb") }
        ))
        assertEquals("en", preferencesManager.language.value)
    }
}
