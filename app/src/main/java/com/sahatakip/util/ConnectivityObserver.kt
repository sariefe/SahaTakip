package com.sahatakip.util

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

enum class ConnectionType {
    Wifi, Cellular, None
}

class ConnectivityObserver(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun observe(): Flow<Pair<ConnectivityStatus, ConnectionType>> {
        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    val type = when {
                        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> ConnectionType.Wifi
                        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> ConnectionType.Cellular
                        else -> ConnectionType.None
                    }
                    launch { send(ConnectivityStatus.Available to type) }
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    launch { send(ConnectivityStatus.Losing to ConnectionType.None) }
                }

                override fun onLost(network: Network) {
                    launch { send(ConnectivityStatus.Lost to ConnectionType.None) }
                }

                override fun onUnavailable() {
                    launch { send(ConnectivityStatus.Unavailable to ConnectionType.None) }
                }
            }

            connectivityManager.registerDefaultNetworkCallback(callback)
            
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val type = when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> ConnectionType.Wifi
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> ConnectionType.Cellular
                else -> ConnectionType.None
            }
            
            if (type != ConnectionType.None) {
                launch { send(ConnectivityStatus.Available to type) }
            } else {
                launch { send(ConnectivityStatus.Unavailable to ConnectionType.None) }
            }

            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }.distinctUntilChanged()
    }
}
