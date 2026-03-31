package com.if3210.nimons360.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.if3210.nimons360.data.local.LocationSharingPreferences
import com.if3210.nimons360.data.local.SecureTokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @SecureTokenDataStore
    @Singleton
    fun provideSecureTokenPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(SecureTokenStore.DATASTORE_NAME) },
        )
    }

    @Provides
    @LocationSharingDataStore
    @Singleton
    fun provideLocationSharingPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(LocationSharingPreferences.DATASTORE_NAME) },
        )
    }
}
