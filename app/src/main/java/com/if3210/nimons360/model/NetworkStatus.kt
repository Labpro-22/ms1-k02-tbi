package com.if3210.nimons360.model

sealed class NetworkStatus {
    data class Connected(val type: InternetStatus) : NetworkStatus()
    data object Disconnected : NetworkStatus()
}
