package com.if3210.nimons360.util

object Constants {
    const val API_HOST = "mad.labpro.hmif.dev"
    const val BASE_URL = "https://$API_HOST/"
    const val WS_URL = "wss://$API_HOST/ws/live"
    const val CURRENT_API_CERTIFICATE_PIN = "sha256/Hod//fULy4qSeaufSKJh7e9ZMoSnpkxT7N099wPNji0="
    // TODO: Before release, add at least one backup certificate pin for the next server public key rotation.
    // TODO: Re-verify this pin whenever the backend TLS certificate/public key changes.

    private const val FAMILY_ICON_BASE_URL = "https://$API_HOST/assets"

    val FAMILY_ICON_URLS: List<String> = (1..8).map { index ->
        "$FAMILY_ICON_BASE_URL/family_icon_${index}.png"
    }
}
