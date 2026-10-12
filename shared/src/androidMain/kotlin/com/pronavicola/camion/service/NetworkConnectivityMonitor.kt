package com.pronavicola.camion.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.pronavicola.camion.util.Log

class NetworkConnectivityMonitor(
    context: Context,
    private val onConnectivityChanged: (NetworkType) -> Unit
) {
    private val tag = "NetworkConnectivityMonitor"
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    var lastKnownNetworkType: NetworkType = NetworkType.NONE
        private set

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val type = detectNetworkType()
            lastKnownNetworkType = type
            Log.i(tag, "Network available: $type")
            onConnectivityChanged(type)
        }

        override fun onLost(network: Network) {
            lastKnownNetworkType = NetworkType.NONE
            Log.i(tag, "Network lost: NONE")
            onConnectivityChanged(NetworkType.NONE)
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val type = determineTypeFromCapabilities(capabilities)
            if (type != lastKnownNetworkType) {
                lastKnownNetworkType = type
                Log.d(tag, "Network capabilities changed: $type")
                onConnectivityChanged(type)
            }
        }
    }

    fun startMonitoring() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            lastKnownNetworkType = detectNetworkType()
            Log.i(tag, "Network connectivity monitoring registered (initial state: $lastKnownNetworkType)")
        } catch (e: Exception) {
            Log.e(tag, "Failed to register network callback: ${e.message}", e)
        }
    }

    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Log.i(tag, "Network connectivity monitoring stopped")
        } catch (e: Exception) {
            Log.w(tag, "Error unregistering network callback: ${e.message}")
        }
    }

    fun detectNetworkType(): NetworkType {
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return NetworkType.NONE
        return determineTypeFromCapabilities(capabilities)
    }

    fun determineTypeFromCapabilities(capabilities: NetworkCapabilities): NetworkType {
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NetworkType.OTHER
            else -> NetworkType.NONE
        }
    }
}
