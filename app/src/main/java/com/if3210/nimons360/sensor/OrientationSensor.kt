package com.if3210.nimons360.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class OrientationSensor @Inject constructor(
    @ApplicationContext context: Context,
) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(SensorManager::class.java)
    private val rotationVectorSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _azimuth = MutableStateFlow(0f)
    val azimuth: StateFlow<Float> = _azimuth.asStateFlow()

    private var hasSmoothedValue = false

    fun start() {
        rotationVectorSensor?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME,
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ROTATION_VECTOR) {
            return
        }

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)

        val rawAzimuth =
            (Math.toDegrees(orientation[0].toDouble()).toFloat() + FULL_ROTATION) % FULL_ROTATION
        val smoothedAzimuth = if (!hasSmoothedValue) {
            hasSmoothedValue = true
            rawAzimuth
        } else {
            applyLowPassFilter(previous = _azimuth.value, current = rawAzimuth)
        }

        _azimuth.value = smoothedAzimuth
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun applyLowPassFilter(previous: Float, current: Float): Float {
        val delta =
            ((current - previous + HALF_ROTATION + FULL_ROTATION) % FULL_ROTATION) - HALF_ROTATION
        return ((previous + (FILTER_ALPHA * delta)) + FULL_ROTATION) % FULL_ROTATION
    }

    companion object {
        private const val FILTER_ALPHA = 0.2f
        private const val FULL_ROTATION = 360f
        private const val HALF_ROTATION = 180f
    }
}
