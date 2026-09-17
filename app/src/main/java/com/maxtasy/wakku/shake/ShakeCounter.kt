package com.maxtasy.wakku.shake

/**
 * Pure shake-counting logic, kept separate from [ShakeDetector]'s
 * SensorEventListener glue so it can be unit tested without constructing a
 * SensorEvent (Milestone 8).
 *
 * A reading counts as a shake once its combined acceleration exceeds
 * [thresholdGravity] g — comfortably above the ~1g a phone reads at rest on
 * a table, but reachable with a firm wrist shake — and only if at least
 * [minIntervalMillis] has passed since the last counted shake, so one
 * physical shake doesn't get counted multiple times off a single motion.
 */
class ShakeCounter(
    private val thresholdGravity: Float = 2.7f,
    private val minIntervalMillis: Long = 300L,
) {
    private var lastShakeAtMillis: Long = 0L

    /** Returns true if this reading counts as a new shake. */
    fun onReading(gForce: Float, atMillis: Long): Boolean {
        if (gForce <= thresholdGravity) return false
        if (atMillis - lastShakeAtMillis < minIntervalMillis) return false
        lastShakeAtMillis = atMillis
        return true
    }
}
