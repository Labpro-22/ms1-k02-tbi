package com.if3210.nimons360.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabasePassphraseProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun getOrCreatePassphrase(): ByteArray {
        val encryptedPrefs = createEncryptedPrefs()
        val existingPassphrase = encryptedPrefs.getString(KEY_DATABASE_PASSPHRASE, null)
        if (!existingPassphrase.isNullOrBlank()) {
            return Base64.getDecoder().decode(existingPassphrase)
        }

        val generatedPassphrase = ByteArray(PASSPHRASE_SIZE_BYTES).also { bytes ->
            SecureRandom().nextBytes(bytes)
        }

        encryptedPrefs.edit()
            .putString(KEY_DATABASE_PASSPHRASE, Base64.getEncoder().encodeToString(generatedPassphrase))
            .apply()

        return generatedPassphrase.copyOf()
    }

    private fun createEncryptedPrefs() = EncryptedSharedPreferences.create(
        context,
        PREFERENCES_FILE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    companion object {
        private const val PREFERENCES_FILE_NAME = "nimons360_secure_db_pref"
        private const val KEY_DATABASE_PASSPHRASE = "nimons360_database_passphrase"
        private const val PASSPHRASE_SIZE_BYTES = 32
    }
}
