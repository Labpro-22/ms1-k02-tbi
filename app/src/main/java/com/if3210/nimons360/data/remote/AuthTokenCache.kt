package com.if3210.nimons360.data.remote

import com.if3210.nimons360.data.local.SecureTokenStore
import com.if3210.nimons360.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class AuthTokenCache @Inject constructor(
    private val secureTokenStore: SecureTokenStore,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    @Volatile
    private var cachedToken: String? = null

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.tag(TAG).e(throwable, "AuthTokenCache scope failure")
    }
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher + coroutineExceptionHandler)

    init {
        scope.launch {
            secureTokenStore.tokenFlow().collect { token ->
                cachedToken = token
            }
        }

        scope.launch {
            secureTokenStore.tokenCorruptionEvents.collect {
                cachedToken = null
                AuthSessionEvents.notifySessionExpired()
            }
        }
    }

    fun getToken(): String? = cachedToken

    fun clearTokenAsync() {
        cachedToken = null
        scope.launch {
            secureTokenStore.clearToken()
        }
    }

    companion object {
        private const val TAG = "AuthTokenCache"
    }
}
