package com.maxtasy.wakku.shake

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeCounterTest {

    private val counter = ShakeCounter(thresholdGravity = 2.7f, minIntervalMillis = 300L)

    @Test
    fun `reading below threshold does not count`() {
        assertFalse(counter.onReading(gForce = 1.0f, atMillis = 0L))
    }

    @Test
    fun `reading exactly at threshold does not count`() {
        assertFalse(counter.onReading(gForce = 2.7f, atMillis = 0L))
    }

    @Test
    fun `reading just above threshold counts`() {
        assertTrue(counter.onReading(gForce = 2.71f, atMillis = 0L))
    }

    @Test
    fun `first reading above threshold always counts regardless of time`() {
        assertTrue(counter.onReading(gForce = 5f, atMillis = 0L))
    }

    @Test
    fun `second shake within min interval is suppressed`() {
        assertTrue(counter.onReading(gForce = 5f, atMillis = 0L))
        assertFalse(counter.onReading(gForce = 5f, atMillis = 299L))
    }

    @Test
    fun `second shake exactly at min interval counts`() {
        assertTrue(counter.onReading(gForce = 5f, atMillis = 0L))
        assertTrue(counter.onReading(gForce = 5f, atMillis = 300L))
    }

    @Test
    fun `second shake after min interval counts`() {
        assertTrue(counter.onReading(gForce = 5f, atMillis = 0L))
        assertTrue(counter.onReading(gForce = 5f, atMillis = 301L))
    }

    @Test
    fun `below-threshold readings between shakes do not reset the interval timer`() {
        assertTrue(counter.onReading(gForce = 5f, atMillis = 0L))
        assertFalse(counter.onReading(gForce = 1f, atMillis = 100L))
        assertFalse(counter.onReading(gForce = 5f, atMillis = 200L))
        assertTrue(counter.onReading(gForce = 5f, atMillis = 300L))
    }

    @Test
    fun `counting a full shake sequence yields the expected total`() {
        val timestamps = listOf(0L, 50L, 300L, 310L, 620L, 900L)
        val countedShakes = timestamps.count { counter.onReading(gForce = 5f, atMillis = it) }
        assertEquals(3, countedShakes)
    }

    @Test
    fun `custom threshold and interval are respected`() {
        val lenient = ShakeCounter(thresholdGravity = 1.5f, minIntervalMillis = 50L)
        assertTrue(lenient.onReading(gForce = 1.6f, atMillis = 0L))
        assertFalse(lenient.onReading(gForce = 1.6f, atMillis = 40L))
        assertTrue(lenient.onReading(gForce = 1.6f, atMillis = 50L))
    }
}
