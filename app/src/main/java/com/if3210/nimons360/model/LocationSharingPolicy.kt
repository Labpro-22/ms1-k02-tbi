package com.if3210.nimons360.model

data class LocationSharingPolicy(
    val hasConsent: Boolean,
    val isSharingEnabled: Boolean,
) {
    val isSharingAllowed: Boolean
        get() = hasConsent && isSharingEnabled
}
