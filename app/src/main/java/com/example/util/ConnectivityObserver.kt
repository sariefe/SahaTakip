package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

enum class ConnectivityStatus {
    Available, Unavailable, Losing, Lost
}

class ConnectivityObserver(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun observe(): Flow<ConnectivityStatus> {
        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    launch { send(ConnectivityStatus.Available) }
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    launch { send(ConnectivityStatus.Losing) }
                }

                override fun onLost(network: Network) {
                    launch { send(ConnectivityStatus.Lost) }
                }

                override fun onUnavailable() {
                    launch { send(ConnectivityStatus.Unavailable) }
                }
            }

            connectivityManager.registerDefaultNetworkCallback(callback)
            
            // Initial state
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val isOnline = capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            
            if (isOnline) {
                launch { send(ConnectivityStatus.Available) }
            } else {
                launch { send(ConnectivityStatus.Unavailable) }
            }

            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }.distinctUntilChanged()
    }
}
