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
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    
                    val type = when {
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.Wifi
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.Cellular
                        else -> ConnectionType.None
                    }
                    
                    if (hasInternet) {
                        launch { send(ConnectivityStatus.Available to type) }
                    } else {
                        launch { send(ConnectivityStatus.Unavailable to type) }
                    }
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

            val request = android.net.NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()

            connectivityManager.registerNetworkCallback(request, callback)

            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val hasInternet = capabilities?.let {
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } ?: false

            val type = when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> ConnectionType.Wifi
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> ConnectionType.Cellular
                else -> ConnectionType.None
            }
            
            if (hasInternet) {
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
