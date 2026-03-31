package com.if3210.nimons360.model

sealed class WebSocketState {
    data object Connecting : WebSocketState()
    data object Connected : WebSocketState()
    data object Disconnected : WebSocketState()
    data class Error(val message: String, val throwable: Throwable? = null) : WebSocketState()
}
