package com.if3210.nimons360.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WebSocketMessage<T>(
    @param:Json(name = "type")
    val type: String,
    @param:Json(name = "payload")
    val payload: T,
    @param:Json(name = "timestamp")
    val timestamp: String,
)

@JsonClass(generateAdapter = true)
data class PresencePayload(
    @param:Json(name = "name")
    val name: String,
    @param:Json(name = "latitude")
    val latitude: Double,
    @param:Json(name = "longitude")
    val longitude: Double,
    @param:Json(name = "rotation")
    val rotation: Double,
    @param:Json(name = "batteryLevel")
    val batteryLevel: Int,
    @param:Json(name = "isCharging")
    val isCharging: Boolean,
    @param:Json(name = "internetStatus")
    val internetStatus: String,
    @param:Json(name = "metadata")
    val metadata: Map<String, Any?> = emptyMap(),
)

@JsonClass(generateAdapter = true)
data class MemberPresence(
    @param:Json(name = "userId")
    val userId: Int,
    @param:Json(name = "email")
    val email: String,
    @param:Json(name = "fullName")
    val fullName: String,
    @param:Json(name = "latitude")
    val latitude: Double,
    @param:Json(name = "longitude")
    val longitude: Double,
    @param:Json(name = "rotation")
    val rotation: Double,
    @param:Json(name = "batteryLevel")
    val batteryLevel: Int,
    @param:Json(name = "isCharging")
    val isCharging: Boolean,
    @param:Json(name = "internetStatus")
    val internetStatus: String,
    @param:Json(name = "metadata")
    val metadata: Map<String, Any?> = emptyMap(),
)
