package com.if3210.nimons360.data.remote

import javax.inject.Inject
import kotlin.math.min
import kotlin.math.pow

class WebSocketReconnectPolicy @Inject constructor() {

    private var reconnectAttempts = 0

    fun resetAttempts() {
        reconnectAttempts = 0
    }

    fun nextBackoffDelayMs(): Long? {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            return null
        }

        val delayMs = min(
            MAX_RECONNECT_DELAY_MS,
            (INITIAL_RECONNECT_DELAY_MS * 2.0.pow(reconnectAttempts.toDouble())).toLong(),
        )
        reconnectAttempts++
        return delayMs
    }

    companion object {
        private const val INITIAL_RECONNECT_DELAY_MS = 1_000L
        private const val MAX_RECONNECT_DELAY_MS = 30_000L
        private const val MAX_RECONNECT_ATTEMPTS = 8
    }
}
