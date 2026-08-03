package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConnectivityObserverTest {

    @MockK
    lateinit var mockContext: Context
    
    @MockK
    lateinit var mockConnectivityManager: ConnectivityManager

    private lateinit var connectivityObserver: ConnectivityObserver

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        // When observer is created, it calls getSystemService
        every { mockContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
        
        // Mock initial state calls to avoid crashes
        every { mockConnectivityManager.activeNetwork } returns null
        every { mockConnectivityManager.getNetworkCapabilities(any()) } returns null
        
        // Mock registration method
        every { mockConnectivityManager.registerDefaultNetworkCallback(any()) } just Runs
        every { mockConnectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just Runs

        connectivityObserver = ConnectivityObserver(mockContext)
    }

    @Test
    fun `observe emits Available when callback onAvailable is triggered`() = runTest {
        val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
        every { mockConnectivityManager.registerDefaultNetworkCallback(capture(callbackSlot)) } just Runs

        val results = mutableListOf<ConnectivityStatus>()
        val job = launch(UnconfinedTestDispatcher()) {
            connectivityObserver.observe().toList(results)
        }

        // Trigger manual callback
        val mockNetwork = mockk<Network>()
        callbackSlot.captured.onAvailable(mockNetwork)

        // The first emission is usually from initial state check (Unavailable in our setup)
        // The second one is from our trigger
        assertTrue(results.contains(ConnectivityStatus.Available))
        
        job.cancel()
    }

    @Test
    fun `observe emits Lost when callback onLost is triggered`() = runTest {
        val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
        every { mockConnectivityManager.registerDefaultNetworkCallback(capture(callbackSlot)) } just Runs

        val results = mutableListOf<ConnectivityStatus>()
        val job = launch(UnconfinedTestDispatcher()) {
            connectivityObserver.observe().toList(results)
        }

        val mockNetwork = mockk<Network>()
        callbackSlot.captured.onLost(mockNetwork)

        assertTrue(results.contains(ConnectivityStatus.Lost))
        
        job.cancel()
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
