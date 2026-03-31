package com.if3210.nimons360.sensor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.if3210.nimons360.di.IoDispatcher
import com.if3210.nimons360.model.InternetStatus
import com.if3210.nimons360.model.NetworkStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

@Singleton
class NetworkSensor @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) {

    private val connectivityManager: ConnectivityManager =
        context.getSystemService(ConnectivityManager::class.java)

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.tag(TAG).e(throwable, "NetworkSensor scope failure")
    }
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher + coroutineExceptionHandler)

    val networkStatus: StateFlow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(resolveCurrentStatus())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(mapNetworkCapabilities(networkCapabilities))
            }

            override fun onLost(network: Network) {
                trySend(resolveCurrentStatus())
            }

            override fun onUnavailable() {
                trySend(NetworkStatus.Disconnected)
            }
        }

        trySend(resolveCurrentStatus())
        val registerResult = runCatching {
            connectivityManager.registerDefaultNetworkCallback(callback)
        }
        if (registerResult.isFailure) {
            close(registerResult.exceptionOrNull())
        }

        awaitClose {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(UNSUBSCRIBE_STOP_TIMEOUT_MS),
            initialValue = resolveCurrentStatus(),
        )

    private fun resolveCurrentStatus(): NetworkStatus {
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkStatus.Disconnected
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return NetworkStatus.Disconnected
        return mapNetworkCapabilities(capabilities)
    }

    private fun mapNetworkCapabilities(capabilities: NetworkCapabilities): NetworkStatus {
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkStatus.Disconnected
        }

        val type = if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            InternetStatus.WIFI
        } else {
            InternetStatus.MOBILE
        }

        return NetworkStatus.Connected(type = type)
    }

    companion object {
        private const val TAG = "NetworkSensor"
        private const val UNSUBSCRIBE_STOP_TIMEOUT_MS = 5_000L
    }
}
