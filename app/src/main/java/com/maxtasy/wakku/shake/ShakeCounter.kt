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
    private var lastShakeAtMillis: Long? = null

    /** Returns true if this reading counts as a new shake. */
    fun onReading(gForce: Float, atMillis: Long): Boolean {
        if (gForce <= thresholdGravity) return false
        val lastShake = lastShakeAtMillis
        // A null lastShake means there's no prior shake to compare against, so the
        // first-ever reading always counts (this used to be sentinel-valued 0L, which
        // silently swallowed the first shake if atMillis was itself within
        // minIntervalMillis of zero, e.g. timestamps sourced from elapsedRealtime()).
        if (lastShake != null && atMillis - lastShake < minIntervalMillis) return false
        lastShakeAtMillis = atMillis
        return true
    }
}
