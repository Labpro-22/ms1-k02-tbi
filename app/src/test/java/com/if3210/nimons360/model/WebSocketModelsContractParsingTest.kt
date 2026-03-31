package com.if3210.nimons360.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebSocketModelsContractParsingTest {

    private val moshi = Moshi.Builder().build()

    @Test
    fun memberPresenceUpdated_parsesAsWebSocketMessageMemberPresence() {
        val json =
            """
            {
              "type": "member_presence_updated",
              "payload": {
                "userId": 12,
                "email": "13522084@std.stei.itb.ac.id",
                "fullName": "Dhafin Fawwaz Ikramullah",
                "latitude": -6.8915,
                "longitude": 107.6107,
                "rotation": 120.5,
                "batteryLevel": 85,
                "isCharging": false,
                "internetStatus": "wifi",
                "metadata": {}
              },
              "timestamp": "2026-03-15T10:00:00Z"
            }
            """.trimIndent()

        val type = Types.newParameterizedType(WebSocketMessage::class.java, MemberPresence::class.java)
        val adapter = moshi.adapter<WebSocketMessage<MemberPresence>>(type)

        val parsed = adapter.fromJson(json)

        assertNotNull(parsed)
        assertEquals("member_presence_updated", parsed!!.type)
        assertEquals(12, parsed.payload.userId)
        assertEquals(85, parsed.payload.batteryLevel)
        assertEquals("wifi", parsed.payload.internetStatus)
        assertEquals("2026-03-15T10:00:00Z", parsed.timestamp)
    }

    @Test
    fun presencePayload_serializesWithRequiredContractFields() {
        val payload = PresencePayload(
            name = "Dhafin Fawwaz",
            latitude = -6.8915,
            longitude = 107.6107,
            rotation = 120.5,
            batteryLevel = 85,
            isCharging = false,
            internetStatus = "wifi",
            metadata = mapOf("key" to "value"),
        )

        val adapter = moshi.adapter(PresencePayload::class.java)
        val json = adapter.toJson(payload)

        assertTrue(json.contains("\"name\""))
        assertTrue(json.contains("\"latitude\""))
        assertTrue(json.contains("\"longitude\""))
        assertTrue(json.contains("\"rotation\""))
        assertTrue(json.contains("\"batteryLevel\""))
        assertTrue(json.contains("\"isCharging\""))
        assertTrue(json.contains("\"internetStatus\""))
        assertTrue(json.contains("\"metadata\""))
    }
}
