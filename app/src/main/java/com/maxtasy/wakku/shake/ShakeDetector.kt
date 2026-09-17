package com.maxtasy.wakku.shake

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/** Reads the accelerometer and calls [onShake] each time [ShakeCounter] counts a new shake. */
class ShakeDetector(
    private val onShake: () -> Unit,
    private val counter: ShakeCounter = ShakeCounter(),
) : SensorEventListener {

    override fun onSensorChanged(event: SensorEvent) {
        val gX = event.values[0] / SensorManager.GRAVITY_EARTH
        val gY = event.values[1] / SensorManager.GRAVITY_EARTH
        val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        if (counter.onReading(gForce, System.currentTimeMillis())) {
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
