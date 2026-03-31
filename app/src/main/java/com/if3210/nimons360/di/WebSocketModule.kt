package com.if3210.nimons360.di

import com.if3210.nimons360.data.local.LocationSharingPreferences
import com.if3210.nimons360.data.local.SecureTokenStore
import com.if3210.nimons360.data.remote.PresenceMessageSerializer
import com.if3210.nimons360.data.remote.PresenceValidator
import com.if3210.nimons360.data.remote.WebSocketManager
import com.if3210.nimons360.data.remote.WebSocketReconnectPolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object WebSocketModule {

    @Provides
    @Singleton
    fun provideWebSocketManager(
        secureTokenStore: SecureTokenStore,
        locationSharingPreferences: LocationSharingPreferences,
        presenceValidator: PresenceValidator,
        presenceMessageSerializer: PresenceMessageSerializer,
        webSocketReconnectPolicy: WebSocketReconnectPolicy,
        okHttpClient: OkHttpClient,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): WebSocketManager {
        return WebSocketManager(
            secureTokenStore = secureTokenStore,
            locationSharingPreferences = locationSharingPreferences,
            presenceValidator = presenceValidator,
            presenceMessageSerializer = presenceMessageSerializer,
            webSocketReconnectPolicy = webSocketReconnectPolicy,
            okHttpClient = okHttpClient,
            ioDispatcher = ioDispatcher,
        )
    }
}
