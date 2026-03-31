package com.if3210.nimons360.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class BackendContractSmokeTest {

    private val baseUrl = System.getenv("NIMONS_BASE_URL")?.ifBlank { DEFAULT_BASE_URL } ?: DEFAULT_BASE_URL
    private val email = System.getenv("NIMONS_EMAIL")
    private val password = System.getenv("NIMONS_PASSWORD")
    private val runWebSocketSmoke =
        System.getenv("NIMONS_RUN_WS_SMOKE")?.equals("true", ignoreCase = true) == true

    private val client = HttpClient.newHttpClient()
    private val moshi = Moshi.Builder().build()

    @Test
    fun login_and_get_profile_match_contract() {
        logStep("START login_and_get_profile_match_contract")
        assumeHasCredentials()

        val tokenData = loginAndGetTokenData()

        assertNotNull(tokenData.user)
        assertFalse(tokenData.token.isBlank())
        assertFalse(tokenData.user.fullName.isBlank())

        val profile = getProfile(tokenData.token)

        assertEquals(tokenData.user.email, profile.email)
        assertEquals(tokenData.user.nim, profile.nim)
        logStep("END login_and_get_profile_match_contract")
    }

    @Test
    fun list_endpoints_match_contract_shapes() {
        logStep("START list_endpoints_match_contract_shapes")
        assumeHasCredentials()

        val token = loginAndGetTokenData().token

        val allFamilies = getAllFamilies(token)
        val myFamilies = getMyFamilies(token)
        val discoverFamilies = discoverFamilies(token)

        assertNotNull(allFamilies)
        assertNotNull(myFamilies)
        assertNotNull(discoverFamilies)

        if (allFamilies.isNotEmpty()) {
            val detail = getFamilyDetail(token, allFamilies.first().id)
            assertEquals(allFamilies.first().id, detail.id)
            assertFalse(detail.name.isBlank())
        }
        logStep("END list_endpoints_match_contract_shapes")
    }

    @Test
    fun websocket_ping_pong_matches_contract() {
        logStep("START websocket_ping_pong_matches_contract")
        assumeTrue(
            "Skipping live websocket smoke test. Set NIMONS_RUN_WS_SMOKE=true to enable.",
            runWebSocketSmoke,
        )
        assumeHasCredentials()

        val token = loginAndGetTokenData().token
        val openLatch = CountDownLatch(1)
        val pongLatch = CountDownLatch(1)
        val failureRef = AtomicReference<Throwable?>(null)
        val closeCodeRef = AtomicReference<Int?>(null)
        val closeReasonRef = AtomicReference<String?>(null)

        val listener = object : WebSocket.Listener {
            override fun onOpen(webSocket: WebSocket) {
                logStep("WebSocket opened")
                openLatch.countDown()
                webSocket.request(1)
            }

            override fun onText(
                webSocket: WebSocket,
                data: CharSequence,
                last: Boolean,
            ): CompletionStage<*> {
                if (data.toString().contains("\"type\":\"pong\"")) {
                    logStep("Received pong message")
                    pongLatch.countDown()
                }
                webSocket.request(1)
                return CompletableFuture.completedFuture(null)
            }

            override fun onError(webSocket: WebSocket, error: Throwable) {
                logStep("WebSocket onError: ${error.message}")
                failureRef.set(error)
            }

            override fun onClose(
                webSocket: WebSocket,
                statusCode: Int,
                reason: String,
            ): CompletionStage<*> {
                logStep("WebSocket closed: statusCode=$statusCode, reason=$reason")
                closeCodeRef.set(statusCode)
                closeReasonRef.set(reason)
                return CompletableFuture.completedFuture(null)
            }
        }

        logStep("Connecting to websocket endpoint")
        val webSocket = client.newWebSocketBuilder()
            .header("Authorization", "Bearer $token")
            .buildAsync(URI.create(toWebSocketUrl(baseUrl)), listener)
            .join()

        assertTrue("WebSocket connection did not open in time", openLatch.await(8, TimeUnit.SECONDS))

        val pingJson = """{"type":"ping","payload":{},"timestamp":"${Instant.now()}"}"""
        try {
            logStep("Sending ping")
            webSocket.sendText(pingJson, true).join()
        } catch (ex: Exception) {
            val closeInfo = "closeCode=${closeCodeRef.get()}, closeReason=${closeReasonRef.get()}"
            throw AssertionError("Failed to send ping over websocket ($closeInfo): ${ex.message}", ex)
        }

        assertTrue(
            "Pong was not received from server in time. closeCode=${closeCodeRef.get()}, closeReason=${closeReasonRef.get()}",
            pongLatch.await(8, TimeUnit.SECONDS),
        )

        failureRef.get()?.let { throwable ->
            throw AssertionError("WebSocket error: ${throwable.message}", throwable)
        }

        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join()
        logStep("END websocket_ping_pong_matches_contract")
    }

    private fun assumeHasCredentials() {
        assumeTrue(!email.isNullOrBlank())
        assumeTrue(!password.isNullOrBlank())
    }

    private fun loginAndGetTokenData(): TokenData {
        logStep("HTTP POST /api/login")
        val bodyJson = moshi.adapter(LoginRequest::class.java)
            .toJson(LoginRequest(email = email!!, password = password!!))

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        run {
            logStep("HTTP /api/login status=${response.statusCode()}")
            assertTrue(
                "Login failed with HTTP ${response.statusCode()}. Ensure NIMONS_EMAIL/NIMONS_PASSWORD are valid.",
                response.statusCode() in 200..299,
            )

            val bodyString = response.body()
            assertNotNull(bodyString)

            val type = Types.newParameterizedType(ApiWrapper::class.java, TokenData::class.java)
            val adapter = moshi.adapter<ApiWrapper<TokenData>>(type)
            val parsed = adapter.fromJson(bodyString)

            assertNotNull(parsed)
            return parsed!!.data
        }
    }

    private fun getProfile(token: String): UserInfo {
        logStep("HTTP GET /api/me")
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/me"))
            .header("Authorization", "Bearer $token")
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        run {
            logStep("HTTP /api/me status=${response.statusCode()}")
            assertTrue("GET /api/me failed with HTTP ${response.statusCode()}", response.statusCode() in 200..299)
            val bodyString = response.body()
            assertNotNull(bodyString)

            val type = Types.newParameterizedType(ApiWrapper::class.java, UserInfo::class.java)
            val adapter = moshi.adapter<ApiWrapper<UserInfo>>(type)
            val parsed = adapter.fromJson(bodyString)

            assertNotNull(parsed)
            return parsed!!.data
        }
    }

    private fun getAllFamilies(token: String): List<FamilyBasic> {
        logStep("HTTP GET /api/families")
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/families"))
            .header("Authorization", "Bearer $token")
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        run {
            logStep("HTTP /api/families status=${response.statusCode()}")
            assertTrue("GET /api/families failed with HTTP ${response.statusCode()}", response.statusCode() in 200..299)
            val bodyString = response.body()
            assertNotNull(bodyString)

            val listType = Types.newParameterizedType(List::class.java, FamilyBasic::class.java)
            val wrapperType = Types.newParameterizedType(ApiWrapper::class.java, listType)
            val adapter = moshi.adapter<ApiWrapper<List<FamilyBasic>>>(wrapperType)
            val parsed = adapter.fromJson(bodyString)

            assertNotNull(parsed)
            return parsed!!.data
        }
    }

    private fun getMyFamilies(token: String): List<FamilyDetail> {
        logStep("HTTP GET /api/me/families")
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/me/families"))
            .header("Authorization", "Bearer $token")
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        run {
            logStep("HTTP /api/me/families status=${response.statusCode()}")
            assertTrue("GET /api/me/families failed with HTTP ${response.statusCode()}", response.statusCode() in 200..299)
            val bodyString = response.body()
            assertNotNull(bodyString)

            val listType = Types.newParameterizedType(List::class.java, FamilyDetail::class.java)
            val wrapperType = Types.newParameterizedType(ApiWrapper::class.java, listType)
            val adapter = moshi.adapter<ApiWrapper<List<FamilyDetail>>>(wrapperType)
            val parsed = adapter.fromJson(bodyString)

            assertNotNull(parsed)
            return parsed!!.data
        }
    }

    private fun discoverFamilies(token: String): List<DiscoverFamily> {
        logStep("HTTP GET /api/families/discover")
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/families/discover"))
            .header("Authorization", "Bearer $token")
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        run {
            logStep("HTTP /api/families/discover status=${response.statusCode()}")
            assertTrue("GET /api/families/discover failed with HTTP ${response.statusCode()}", response.statusCode() in 200..299)
            val bodyString = response.body()
            assertNotNull(bodyString)

            val listType = Types.newParameterizedType(List::class.java, DiscoverFamily::class.java)
            val wrapperType = Types.newParameterizedType(ApiWrapper::class.java, listType)
            val adapter = moshi.adapter<ApiWrapper<List<DiscoverFamily>>>(wrapperType)
            val parsed = adapter.fromJson(bodyString)

            assertNotNull(parsed)
            return parsed!!.data
        }
    }

    private fun getFamilyDetail(token: String, familyId: Int): FamilyDetail {
        logStep("HTTP GET /api/families/$familyId")
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/families/$familyId"))
            .header("Authorization", "Bearer $token")
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        run {
            logStep("HTTP /api/families/{familyId} status=${response.statusCode()}")
            assertTrue("GET /api/families/{familyId} failed with HTTP ${response.statusCode()}", response.statusCode() in 200..299)
            val bodyString = response.body()
            assertNotNull(bodyString)

            val type = Types.newParameterizedType(ApiWrapper::class.java, FamilyDetail::class.java)
            val adapter = moshi.adapter<ApiWrapper<FamilyDetail>>(type)
            val parsed = adapter.fromJson(bodyString)

            assertNotNull(parsed)
            return parsed!!.data
        }
    }

    private companion object {
        private const val DEFAULT_BASE_URL = "https://mad.labpro.hmif.dev"
    }

    private fun toWebSocketUrl(httpBaseUrl: String): String {
        val sanitized = httpBaseUrl.trimEnd('/')
        return when {
            sanitized.startsWith("https://") -> sanitized.replaceFirst("https://", "wss://")
            sanitized.startsWith("http://") -> sanitized.replaceFirst("http://", "ws://")
            else -> sanitized
        } + "/ws/live"
    }

    private fun logStep(message: String) {
        println("[BackendContractSmokeTest] $message")
    }
}
