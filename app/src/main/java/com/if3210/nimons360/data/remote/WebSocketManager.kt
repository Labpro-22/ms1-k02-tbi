package com.if3210.nimons360.data.remote

import com.if3210.nimons360.data.local.LocationSharingPreferences
import com.if3210.nimons360.data.local.SecureTokenStore
import com.if3210.nimons360.di.IoDispatcher
import com.if3210.nimons360.model.MemberPresence
import com.if3210.nimons360.model.PresencePayload
import com.if3210.nimons360.model.WebSocketState
import com.if3210.nimons360.util.Constants.WS_URL
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import timber.log.Timber

class WebSocketManager(
    private val secureTokenStore: SecureTokenStore,
    private val locationSharingPreferences: LocationSharingPreferences,
    private val presenceValidator: PresenceValidator,
    private val presenceMessageSerializer: PresenceMessageSerializer,
    private val webSocketReconnectPolicy: WebSocketReconnectPolicy,
    okHttpClient: OkHttpClient,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.tag(TAG).e(throwable, "WebSocketManager scope failure")
    }
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher + coroutineExceptionHandler)
    private val stateMutex = Mutex()

    private val client = okHttpClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val moshi = Moshi.Builder().build()
    private val memberPresenceAdapter = moshi.adapter(MemberPresence::class.java)

    private val _memberPresences = MutableStateFlow<Map<Int, MemberPresence>>(emptyMap())
    val memberPresences: StateFlow<Map<Int, MemberPresence>> = _memberPresences.asStateFlow()

    private val _webSocketState = MutableStateFlow<WebSocketState>(WebSocketState.Disconnected)
    val webSocketState: StateFlow<WebSocketState> = _webSocketState.asStateFlow()

    private val latestPresencePayload = MutableStateFlow<PresencePayload?>(null)
    private val memberLastUpdatedAt = mutableMapOf<Int, Long>()

    private val locationSharingPolicy = MutableStateFlow(false)

    private var webSocket: WebSocket? = null
    private var manuallyDisconnected = false
    private var sessionExpiredNotified = false

    private var reconnectJob: Job? = null
    private var presenceJob: Job? = null
    private var pingJob: Job? = null
    private var cleanupJob: Job? = null
    private var locationSharingPolicyJob: Job? = null

    init {
        startLocationSharingPolicyObserver()
    }

    fun connect() {
        scope.launch {
            stateMutex.withLock {
                manuallyDisconnected = false
                webSocketReconnectPolicy.resetAttempts()
                reconnectJob?.cancel()
                reconnectJob = null
            }
            connectInternal()
        }
    }

    fun disconnect() {
        scope.launch {
            stateMutex.withLock {
                manuallyDisconnected = true
                webSocketReconnectPolicy.resetAttempts()
                reconnectJob?.cancel()
                reconnectJob = null
                stopBackgroundLoopsLocked()
                webSocket?.close(CLOSE_NORMAL, "Client disconnected")
                webSocket = null
                _webSocketState.value = WebSocketState.Disconnected
                sessionExpiredNotified = false
            }
        }
    }

    fun updatePresencePayload(payload: PresencePayload) {
        if (!locationSharingPolicy.value) {
            return
        }
        latestPresencePayload.value = payload
    }

    suspend fun grantLocationSharingConsentAndEnableSharing() {
        // TODO: Call this only after the user explicitly accepts a privacy notice that explains:
        // TODO: precise location sharing, battery/charging telemetry, internet status, recipient scope, and update frequency.
        locationSharingPreferences.grantConsentAndEnableSharing()
    }

    suspend fun enableLocationSharing() {
        // TODO: Wire this to a visible in-app toggle so the user can resume location sharing without re-consenting.
        locationSharingPreferences.enableSharing()
    }

    suspend fun pauseLocationSharing() {
        // TODO: Wire this to a visible in-app toggle/button so the user can stop location sharing at any time.
        locationSharingPreferences.pauseSharing()
        latestPresencePayload.value = null
    }

    suspend fun revokeLocationSharingConsent() {
        // TODO: Wire this to a privacy/settings action and show a confirmation dialog before revoking consent.
        locationSharingPreferences.revokeConsent()
        latestPresencePayload.value = null
    }

    fun isLocationSharingEnabled(): Boolean = locationSharingPolicy.value

    fun sendPing() {
        scope.launch {
            stateMutex.withLock {
                webSocket?.send(presenceMessageSerializer.buildPingMessage())
            }
        }
    }

    private suspend fun openWebSocket(token: String) {
        val request = Request.Builder()
            .url(WS_URL)
            .header(HEADER_AUTHORIZATION, "$TOKEN_PREFIX $token")
            .build()

        val createdWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                scope.launch {
                    handleSocketOpen()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scope.launch {
                    handleSocketTermination()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scope.launch {
                    handleSocketTermination(t)
                }
            }
        })

        var shouldCloseSocket = false
        stateMutex.withLock {
            if (manuallyDisconnected) {
                shouldCloseSocket = true
            } else {
                webSocket = createdWebSocket
            }
        }
        if (shouldCloseSocket) {
            createdWebSocket.close(CLOSE_NORMAL, "Client disconnected")
        }
    }

    private suspend fun handleSocketOpen() {
        stateMutex.withLock {
            webSocketReconnectPolicy.resetAttempts()
            reconnectJob?.cancel()
            reconnectJob = null
            sessionExpiredNotified = false
            _webSocketState.value = WebSocketState.Connected
            startBackgroundLoopsLocked()
        }
    }

    private suspend fun handleSocketTermination(error: Throwable? = null) {
        stateMutex.withLock {
            webSocket = null
            stopBackgroundLoopsLocked()
            _webSocketState.value = if (error == null) {
                WebSocketState.Disconnected
            } else {
                WebSocketState.Error(error.message ?: "WebSocket connection failed", error)
            }
        }
        scheduleReconnectIfNeeded()
    }

    private fun startBackgroundLoopsLocked() {
        stopBackgroundLoopsLocked()

        presenceJob = scope.launch {
            while (isActive) {
                val message = if (locationSharingPolicy.value) {
                    latestPresencePayload.value?.let(presenceMessageSerializer::buildUpdatePresenceMessage)
                } else {
                    null
                }
                if (message != null) {
                    stateMutex.withLock {
                        webSocket?.send(message)
                    }
                }
                delay(PRESENCE_INTERVAL_MS)
            }
        }

        pingJob = scope.launch {
            while (isActive) {
                stateMutex.withLock {
                    webSocket?.send(presenceMessageSerializer.buildPingMessage())
                }
                delay(PING_INTERVAL_MS)
            }
        }

        cleanupJob = scope.launch {
            while (isActive) {
                stateMutex.withLock {
                    removeOfflineMembersLocked()
                }
                delay(CLEANUP_INTERVAL_MS)
            }
        }
    }

    private fun stopBackgroundLoopsLocked() {
        presenceJob?.cancel()
        pingJob?.cancel()
        cleanupJob?.cancel()
        presenceJob = null
        pingJob = null
        cleanupJob = null
    }

    private suspend fun scheduleReconnectIfNeeded() {
        stateMutex.withLock {
            if (manuallyDisconnected) {
                return@withLock
            }

            if (reconnectJob?.isActive == true) {
                return@withLock
            }

            val backoffMs = webSocketReconnectPolicy.nextBackoffDelayMs()
            if (backoffMs == null) {
                _webSocketState.value = WebSocketState.Error("WebSocket reconnect attempts exceeded")
                return@withLock
            }

            reconnectJob = scope.launch {
                delay(backoffMs)
                connectInternal()
            }
        }
    }

    private suspend fun connectInternal() {
        val token = resolveTokenForConnection()
        currentCoroutineContext().ensureActive()

        val connectionPreparation = transitionToConnecting(token)

        if (connectionPreparation.shouldNotifySessionExpired) {
            AuthSessionEvents.notifySessionExpired()
        }

        connectionPreparation.tokenForConnection?.let { resolvedToken ->
            openWebSocket(resolvedToken)
        }
    }

    private suspend fun resolveTokenForConnection(): String? {
        return secureTokenStore.getToken()
    }

    private suspend fun transitionToConnecting(token: String?): ConnectionPreparation {
        var shouldNotifySessionExpired = false
        var tokenForConnection: String? = null

        stateMutex.withLock {
            if (manuallyDisconnected) {
                return@withLock
            }

            if (_webSocketState.value == WebSocketState.Connected || _webSocketState.value == WebSocketState.Connecting) {
                return@withLock
            }

            if (token.isNullOrBlank()) {
                _webSocketState.value = WebSocketState.Error("Cannot connect websocket: missing token")
                if (!sessionExpiredNotified) {
                    sessionExpiredNotified = true
                    shouldNotifySessionExpired = true
                }
                return@withLock
            }

            sessionExpiredNotified = false
            _webSocketState.value = WebSocketState.Connecting
            tokenForConnection = token
        }

        return ConnectionPreparation(
            tokenForConnection = tokenForConnection,
            shouldNotifySessionExpired = shouldNotifySessionExpired,
        )
    }

    private fun handleIncomingMessage(rawMessage: String) {
        val root = try {
            JSONObject(rawMessage)
        } catch (throwable: Throwable) {
            Timber.tag(TAG).w(throwable, "Invalid WebSocket JSON payload")
            return
        }

        when (root.optString(PresenceMessageSerializer.KEY_TYPE)) {
            PresenceMessageSerializer.TYPE_MEMBER_PRESENCE_UPDATED -> {
                val payloadJson = root.optJSONObject(PresenceMessageSerializer.KEY_PAYLOAD) ?: return
                val presence = parsePresence(payloadJson) ?: return
                if (!presenceValidator.isValidPresence(presence)) {
                    Timber.tag(TAG).w("Dropped invalid member presence payload for userId=%d", presence.userId)
                    return
                }
                scope.launch {
                    stateMutex.withLock {
                        memberLastUpdatedAt[presence.userId] = System.currentTimeMillis()
                        _memberPresences.update { current ->
                            current + (presence.userId to presence)
                        }
                    }
                }
            }

            PresenceMessageSerializer.TYPE_PONG -> {
                // No-op, receiving pong confirms keepalive.
            }
        }
    }

    private fun parsePresence(payloadJson: JSONObject): MemberPresence? {
        return try {
            memberPresenceAdapter.fromJson(payloadJson.toString())
        } catch (throwable: Throwable) {
            Timber.tag(TAG).w(throwable, "Failed to parse member presence payload")
            null
        }
    }

    private fun removeOfflineMembersLocked() {
        val now = System.currentTimeMillis()
        val offlineUserIds = memberLastUpdatedAt
            .filterValues { lastUpdate -> now - lastUpdate > OFFLINE_TIMEOUT_MS }
            .keys

        if (offlineUserIds.isEmpty()) {
            return
        }

        offlineUserIds.forEach(memberLastUpdatedAt::remove)
        _memberPresences.update { current ->
            current.filterKeys { userId -> userId !in offlineUserIds }
        }
    }

    private fun startLocationSharingPolicyObserver() {
        locationSharingPolicyJob?.cancel()
        locationSharingPolicyJob = scope.launch {
            locationSharingPreferences.locationSharingPolicyFlow().collect { policy ->
                val isSharingAllowed = policy.isSharingAllowed
                stateMutex.withLock {
                    locationSharingPolicy.value = isSharingAllowed
                    if (!isSharingAllowed) {
                        latestPresencePayload.value = null
                    }
                }
            }
        }
    }

    private data class ConnectionPreparation(
        val tokenForConnection: String?,
        val shouldNotifySessionExpired: Boolean,
    )

    companion object {
        private const val TAG = "WebSocketManager"

        private const val PRESENCE_INTERVAL_MS = 1_000L
        private const val PING_INTERVAL_MS = 15_000L
        private const val CLEANUP_INTERVAL_MS = 1_000L
        private const val OFFLINE_TIMEOUT_MS = 5_000L

        private const val CLOSE_NORMAL = 1000
    }
}
