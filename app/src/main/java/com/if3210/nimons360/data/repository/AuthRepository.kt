package com.if3210.nimons360.data.repository

import com.if3210.nimons360.data.local.SecureTokenStore
import com.if3210.nimons360.data.remote.ApiService
import com.if3210.nimons360.model.LoginRequest
import com.if3210.nimons360.model.TokenData
import com.if3210.nimons360.util.Result
import com.if3210.nimons360.util.repositoryCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val secureTokenStore: SecureTokenStore,
) {

    suspend fun login(email: String, password: String): Result<TokenData> {
        val (validatedEmail, validatedPassword) = validateLoginInput(email, password)
        return repositoryCall {
            val response = apiService.login(
                LoginRequest(
                    email = validatedEmail,
                    password = validatedPassword,
                ),
            )
            secureTokenStore.saveToken(response.data.token)
            response.data
        }
    }

    suspend fun logout(): Result<Unit> {
        return repositoryCall {
            secureTokenStore.clearToken()
        }
    }

    fun tokenFlow(): Flow<String?> = secureTokenStore.tokenFlow()

    suspend fun getToken(): String? = secureTokenStore.getToken()

    private fun validateLoginInput(email: String, password: String): Pair<String, String> {
        val normalizedEmail = email.trim()
        require(normalizedEmail.isNotEmpty()) { "Email must not be empty" }
        require(normalizedEmail.length <= MAX_EMAIL_LENGTH) { "Email is too long" }
        require(EMAIL_PATTERN.matches(normalizedEmail)) { "Email format is invalid" }

        require(password.isNotBlank()) { "Password must not be empty" }
        require(password.length <= MAX_PASSWORD_LENGTH) { "Password is too long" }

        return normalizedEmail to password
    }

    companion object {
        private const val MAX_EMAIL_LENGTH = 254
        private const val MAX_PASSWORD_LENGTH = 128
        private val EMAIL_PATTERN =
            Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
