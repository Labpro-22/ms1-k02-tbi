package com.if3210.nimons360.data.remote

import com.if3210.nimons360.model.InternetStatus
import com.if3210.nimons360.model.MemberPresence
import javax.inject.Inject

class PresenceValidator @Inject constructor() {

    fun isValidPresence(presence: MemberPresence): Boolean {
        return hasValidUserIdentity(presence) &&
            hasValidCoordinates(presence) &&
            hasValidTelemetry(presence)
    }

    private fun hasValidUserIdentity(presence: MemberPresence): Boolean {
        return presence.userId > 0 &&
            presence.fullName.isNotBlank() &&
            presence.fullName.length <= MAX_FULL_NAME_LENGTH &&
            presence.email.isNotBlank() &&
            presence.email.length <= MAX_EMAIL_LENGTH
    }

    private fun hasValidCoordinates(presence: MemberPresence): Boolean {
        return presence.latitude.isFinite() &&
            presence.latitude in MIN_LATITUDE..MAX_LATITUDE &&
            presence.longitude.isFinite() &&
            presence.longitude in MIN_LONGITUDE..MAX_LONGITUDE &&
            presence.rotation.isFinite() &&
            presence.rotation in MIN_ROTATION..MAX_ROTATION
    }

    private fun hasValidTelemetry(presence: MemberPresence): Boolean {
        return presence.batteryLevel in MIN_BATTERY_LEVEL..MAX_BATTERY_LEVEL &&
            hasValidInternetStatus(presence.internetStatus)
    }

    private fun hasValidInternetStatus(rawInternetStatus: String): Boolean {
        return InternetStatus.fromApiValue(rawInternetStatus) != null
    }

    companion object {
        private const val MIN_LATITUDE = -90.0
        private const val MAX_LATITUDE = 90.0
        private const val MIN_LONGITUDE = -180.0
        private const val MAX_LONGITUDE = 180.0
        private const val MIN_ROTATION = 0.0
        private const val MAX_ROTATION = 360.0
        private const val MIN_BATTERY_LEVEL = 0
        private const val MAX_BATTERY_LEVEL = 100
        private const val MAX_FULL_NAME_LENGTH = 200
        private const val MAX_EMAIL_LENGTH = 254
    }
}
