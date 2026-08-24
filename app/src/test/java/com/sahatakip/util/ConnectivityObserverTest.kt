package com.sahatakip.util

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
        
        // Mock registration methods
        every { mockConnectivityManager.registerNetworkCallback(any(), any<ConnectivityManager.NetworkCallback>()) } just Runs
        every { mockConnectivityManager.registerDefaultNetworkCallback(any()) } just Runs
        every { mockConnectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just Runs

        connectivityObserver = ConnectivityObserver(mockContext)
    }

    @Test
    fun `observe emits Available when callback onAvailable is triggered`() = runTest {
        val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
        every { mockConnectivityManager.registerNetworkCallback(any(), capture(callbackSlot)) } just Runs

        val results = mutableListOf<Pair<ConnectivityStatus, ConnectionType>>()
        val job = launch(UnconfinedTestDispatcher()) {
            connectivityObserver.observe().toList(results)
        }

        // Trigger manual callback
        val mockNetwork = mockk<Network>()
        callbackSlot.captured.onAvailable(mockNetwork)
        
        // ConnectivityObserver logic now relies on onCapabilitiesChanged for Available status
        val mockCapabilities = mockk<android.net.NetworkCapabilities>()
        every { mockCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { mockCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { mockCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { mockCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        
        callbackSlot.captured.onCapabilitiesChanged(mockNetwork, mockCapabilities)

        assertTrue(results.any { it.first == ConnectivityStatus.Available && it.second == ConnectionType.Wifi })
        
        job.cancel()
    }

    @Test
    fun `observe emits Lost when callback onLost is triggered`() = runTest {
        val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
        every { mockConnectivityManager.registerNetworkCallback(any(), capture(callbackSlot)) } just Runs

        val results = mutableListOf<Pair<ConnectivityStatus, ConnectionType>>()
        val job = launch(UnconfinedTestDispatcher()) {
            connectivityObserver.observe().toList(results)
        }

        val mockNetwork = mockk<Network>()
        callbackSlot.captured.onLost(mockNetwork)

        assertTrue(results.any { it.first == ConnectivityStatus.Lost })
        
        job.cancel()
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
