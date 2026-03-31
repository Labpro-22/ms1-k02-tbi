package com.if3210.nimons360.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SecureTokenDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocationSharingDataStore
