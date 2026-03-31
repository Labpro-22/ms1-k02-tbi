package com.if3210.nimons360.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.if3210.nimons360.di.LocationSharingDataStore
import com.if3210.nimons360.model.LocationSharingPolicy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class LocationSharingPreferences @Inject constructor(
    @param:LocationSharingDataStore
    private val dataStore: DataStore<Preferences>,
) {

    fun locationSharingPolicyFlow(): Flow<LocationSharingPolicy> {
        return dataStore.data
            .map { preferences ->
                val hasConsent = preferences[KEY_HAS_LOCATION_SHARING_CONSENT] ?: false
                val isSharingEnabled = preferences[KEY_IS_LOCATION_SHARING_ENABLED] ?: false

                LocationSharingPolicy(
                    hasConsent = hasConsent,
                    isSharingEnabled = isSharingEnabled,
                )
            }
            .distinctUntilChanged()
    }

    suspend fun grantConsentAndEnableSharing() {
        dataStore.edit { preferences ->
            preferences[KEY_HAS_LOCATION_SHARING_CONSENT] = true
            preferences[KEY_IS_LOCATION_SHARING_ENABLED] = true
        }
    }

    suspend fun enableSharing() {
        dataStore.edit { preferences ->
            check(preferences[KEY_HAS_LOCATION_SHARING_CONSENT] == true) {
                "Location sharing cannot be enabled before explicit user consent is granted"
            }
            preferences[KEY_IS_LOCATION_SHARING_ENABLED] = true
        }
    }

    suspend fun pauseSharing() {
        dataStore.edit { preferences ->
            preferences[KEY_IS_LOCATION_SHARING_ENABLED] = false
        }
    }

    suspend fun revokeConsent() {
        dataStore.edit { preferences ->
            preferences[KEY_HAS_LOCATION_SHARING_CONSENT] = false
            preferences[KEY_IS_LOCATION_SHARING_ENABLED] = false
        }
    }

    suspend fun resetForPrivacyReview() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_HAS_LOCATION_SHARING_CONSENT)
            preferences.remove(KEY_IS_LOCATION_SHARING_ENABLED)
        }
    }

    companion object {
        const val DATASTORE_NAME = "location_sharing_preferences"
        private val KEY_HAS_LOCATION_SHARING_CONSENT =
            booleanPreferencesKey("has_location_sharing_consent")
        private val KEY_IS_LOCATION_SHARING_ENABLED =
            booleanPreferencesKey("is_location_sharing_enabled")
    }
}
