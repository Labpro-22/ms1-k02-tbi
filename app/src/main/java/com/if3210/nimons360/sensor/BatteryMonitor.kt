package com.if3210.nimons360.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.if3210.nimons360.di.IoDispatcher
import com.if3210.nimons360.model.BatteryInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

@Singleton
class BatteryMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.tag(TAG).e(throwable, "BatteryMonitor scope failure")
    }
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher + coroutineExceptionHandler)

    val batteryInfo: StateFlow<BatteryInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                trySend(parseBatteryInfo(intent))
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = context.registerReceiver(receiver, filter)
        trySend(parseBatteryInfo(stickyIntent))

        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(UNSUBSCRIBE_STOP_TIMEOUT_MS),
            initialValue = readCurrentBatteryInfo(),
        )

    private fun readCurrentBatteryInfo(): BatteryInfo {
        // Passing a null receiver returns the latest sticky battery intent without registering a new receiver.
        val stickyIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return parseBatteryInfo(stickyIntent)
    }

    private fun parseBatteryInfo(intent: Intent?): BatteryInfo {
        if (intent == null) {
            return BatteryInfo(level = 0, isCharging = false)
        }

        val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val rawScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val level = if (rawLevel >= 0 && rawScale > 0) {
            ((rawLevel * 100f) / rawScale).toInt()
        } else {
            0
        }

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        return BatteryInfo(
            level = level.coerceIn(0, 100),
            isCharging = isCharging,
        )
    }

    companion object {
        private const val TAG = "BatteryMonitor"
        private const val UNSUBSCRIBE_STOP_TIMEOUT_MS = 5_000L
    }
}
