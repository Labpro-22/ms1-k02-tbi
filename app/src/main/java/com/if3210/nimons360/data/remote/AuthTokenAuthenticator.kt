package com.if3210.nimons360.data.remote

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

@Singleton
class AuthTokenAuthenticator @Inject constructor(
    private val authTokenCache: AuthTokenCache,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_AUTH_RETRY_COUNT) {
            return null
        }

        if (response.code == AuthInterceptor.HTTP_TOKEN_EXPIRED) {
            authTokenCache.clearTokenAsync()
            AuthSessionEvents.notifySessionExpired()
            return null
        }

        val token = authTokenCache.getToken() ?: return null

        return response.request.newBuilder()
            .header(HEADER_AUTHORIZATION, "$TOKEN_PREFIX $token")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var currentResponse = response.priorResponse
        while (currentResponse != null) {
            count++
            currentResponse = currentResponse.priorResponse
        }
        return count
    }

    companion object {
        private const val MAX_AUTH_RETRY_COUNT = 2
    }
}
