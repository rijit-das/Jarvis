package com.jarvis.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Connectivity state exposed to the UI and the offline-mode gate. */
enum class ConnectivityState { Online, Offline }

@Singleton
class ConnectivityMonitor @Inject constructor(
    @ApplicationContext context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _state = MutableStateFlow(currentState())
    val state: StateFlow<ConnectivityState> = _state.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _state.value = ConnectivityState.Online
        }

        override fun onLost(network: Network) {
            _state.value = currentState()
        }
    }

    fun register() {
        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (_: SecurityException) {
            // ACCESS_NETWORK_STATE missing — state stays at initial value.
        }
    }

    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (_: IllegalArgumentException) {
            // Not registered.
        }
    }

    private fun currentState(): ConnectivityState {
        val network = connectivityManager.activeNetwork ?: return ConnectivityState.Offline
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return ConnectivityState.Offline
        val hasTransport = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return if (hasTransport) ConnectivityState.Online else ConnectivityState.Offline
    }
}
