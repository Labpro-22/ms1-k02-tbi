package com.if3210.nimons360.data.remote

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AuthInterceptor @Inject constructor(
    private val authTokenCache: AuthTokenCache,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = authTokenCache.getToken()

        val authorizedRequest = if (
            !token.isNullOrBlank() && originalRequest.header(HEADER_AUTHORIZATION).isNullOrBlank()
        ) {
            originalRequest.newBuilder()
                .header(HEADER_AUTHORIZATION, "$TOKEN_PREFIX $token")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(authorizedRequest)
        if (response.code == HTTP_TOKEN_EXPIRED) {
            handleExpiredSession()
        }

        return response
    }

    private fun handleExpiredSession() {
        authTokenCache.clearTokenAsync()
        AuthSessionEvents.notifySessionExpired()
    }

    companion object {
        const val HTTP_TOKEN_EXPIRED = 409
    }
}
