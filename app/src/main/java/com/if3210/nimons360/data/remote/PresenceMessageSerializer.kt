package com.if3210.nimons360.data.remote

import com.if3210.nimons360.model.PresencePayload
import java.time.Instant
import javax.inject.Inject
import org.json.JSONObject

class PresenceMessageSerializer @Inject constructor() {

    fun buildUpdatePresenceMessage(payload: PresencePayload): String {
        val payloadJson = JSONObject()
            .put("name", payload.name)
            .put("latitude", payload.latitude)
            .put("longitude", payload.longitude)
            .put("rotation", payload.rotation)
            .put("batteryLevel", payload.batteryLevel)
            .put("isCharging", payload.isCharging)
            .put("internetStatus", payload.internetStatus)
            .put("metadata", JSONObject(payload.metadata))

        return JSONObject()
            .put(KEY_TYPE, TYPE_UPDATE_PRESENCE)
            .put(KEY_PAYLOAD, payloadJson)
            .put(KEY_TIMESTAMP, Instant.now().toString())
            .toString()
    }

    fun buildPingMessage(): String {
        return JSONObject()
            .put(KEY_TYPE, TYPE_PING)
            .put(KEY_PAYLOAD, JSONObject())
            .put(KEY_TIMESTAMP, Instant.now().toString())
            .toString()
    }

    companion object {
        const val KEY_TYPE = "type"
        const val KEY_PAYLOAD = "payload"
        const val KEY_TIMESTAMP = "timestamp"

        const val TYPE_PING = "ping"
        const val TYPE_PONG = "pong"
        const val TYPE_UPDATE_PRESENCE = "update_presence"
        const val TYPE_MEMBER_PRESENCE_UPDATED = "member_presence_updated"
    }
}
