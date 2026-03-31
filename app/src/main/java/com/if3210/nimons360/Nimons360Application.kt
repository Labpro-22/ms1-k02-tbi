package com.if3210.nimons360

import android.app.Application
import android.content.pm.ApplicationInfo
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class Nimons360Application : Application() {

	override fun onCreate() {
		super.onCreate()
		val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
		if (isDebuggable) {
			Timber.plant(Timber.DebugTree())
		}
	}
}
