package com.libredisplay.ui.monitoring

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network

class AndroidConnectivityStatusProvider(context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun start(onAvailabilityChanged: (Boolean) -> Unit) {
        if (callback != null) return

        onAvailabilityChanged(isCurrentlyAvailable())

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onAvailabilityChanged(true)
            }

            override fun onLost(network: Network) {
                onAvailabilityChanged(isCurrentlyAvailable())
            }

            override fun onUnavailable() {
                onAvailabilityChanged(false)
            }
        }
        callback = networkCallback
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    fun stop() {
        val active = callback ?: return
        runCatching { connectivityManager.unregisterNetworkCallback(active) }
        callback = null
    }

    private fun isCurrentlyAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

