package com.if3210.nimons360.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.if3210.nimons360.di.SecureTokenDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

@Singleton
class SecureTokenStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:SecureTokenDataStore
    private val dataStore: DataStore<Preferences>,
) {

    private val encryptedTokenKey = stringPreferencesKey(KEY_ENCRYPTED_TOKEN)
    private val corruptionRecoveryMutex = Mutex()

    private val _tokenCorruptionEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val tokenCorruptionEvents: SharedFlow<Unit> = _tokenCorruptionEvents.asSharedFlow()

    @Suppress("DEPRECATION")
    private val aead: Aead by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREF_FILE)
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        keysetHandle.getPrimitive(Aead::class.java)
    }

    suspend fun saveToken(token: String) {
        dataStore.edit { preferences ->
            preferences[encryptedTokenKey] = encrypt(token)
        }
    }

    suspend fun getToken(): String? = tokenFlow().first()

    suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(encryptedTokenKey)
        }
    }

    fun tokenFlow(): Flow<String?> {
        return dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { preferences ->
                preferences[encryptedTokenKey]?.let { encryptedToken ->
                    when (val result = decrypt(encryptedToken)) {
                        is DecryptResult.Success -> result.token
                        is DecryptResult.Failure -> {
                            handleDecryptionFailure(result.throwable)
                            null
                        }
                    }
                }
            }
    }

    private fun encrypt(plainText: String): String {
        val cipherText = aead.encrypt(plainText.toByteArray(Charsets.UTF_8), null)
        return Base64.getEncoder().encodeToString(cipherText)
    }

    private fun decrypt(encodedCipherText: String): DecryptResult {
        return runSafeCatching {
            val cipherBytes = Base64.getDecoder().decode(encodedCipherText)
            val plainBytes = aead.decrypt(cipherBytes, null)
            plainBytes.toString(Charsets.UTF_8)
        }.fold(
            onSuccess = { token -> DecryptResult.Success(token) },
            onFailure = { throwable -> DecryptResult.Failure(throwable) },
        )
    }

    private suspend fun handleDecryptionFailure(throwable: Throwable) {
        Timber.tag(TAG).e(throwable, "Token decryption failed")

        if (throwable !is GeneralSecurityException && throwable !is IllegalArgumentException) {
            return
        }

        runSuspendSafeCatching {
            corruptionRecoveryMutex.withLock {
                var wasRemoved = false
                dataStore.edit { preferences ->
                    if (preferences[encryptedTokenKey] != null) {
                        preferences.remove(encryptedTokenKey)
                        wasRemoved = true
                    }
                }

                if (wasRemoved) {
                    _tokenCorruptionEvents.tryEmit(Unit)
                }
            }
        }.onFailure { recoveryError ->
            Timber.tag(TAG)
                .e(recoveryError, "Token corruption recovery failed")
        }
    }

    private inline fun <T> runSafeCatching(block: () -> T): kotlin.Result<T> {
        return try {
            kotlin.Result.success(block())
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            kotlin.Result.failure(throwable)
        }
    }

    private suspend inline fun <T> runSuspendSafeCatching(
        crossinline block: suspend () -> T,
    ): kotlin.Result<T> {
        return try {
            kotlin.Result.success(block())
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            kotlin.Result.failure(throwable)
        }
    }

    private sealed class DecryptResult {
        data class Success(val token: String) : DecryptResult()
        data class Failure(val throwable: Throwable) : DecryptResult()
    }

    companion object {
        private const val TAG = "SecureTokenStore"
        const val DATASTORE_NAME = "secure_token_store"
        private const val KEY_ENCRYPTED_TOKEN = "jwt_token_encrypted"
        private const val KEYSET_NAME = "nimons360_tink_keyset"
        private const val KEYSET_PREF_FILE = "nimons360_tink_pref"
        private const val MASTER_KEY_URI = "android-keystore://nimons360_master_key"
    }
}
