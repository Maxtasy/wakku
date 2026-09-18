package com.maxtasy.wakku.scheduling

import com.maxtasy.wakku.data.Alarm
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class AlarmTimingTest {

    private val zone = ZoneOffset.UTC

    // An arbitrary fixed instant; tests derive weekdays relative to it via
    // now.dayOfWeek instead of hardcoding a calendar day, so they don't depend
    // on which actual weekday this date falls on.
    private val now = LocalDateTime.of(2026, 9, 21, 10, 30)

    private fun alarm(hour: Int, minute: Int, repeatDays: Set<DayOfWeek> = emptySet()) =
        Alarm(hour = hour, minute = minute, repeatDays = repeatDays)

    private fun expectedMillis(date: LocalDate, hour: Int, minute: Int): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `one-time alarm later today schedules today`() {
        val result = AlarmTiming.nextTriggerMillis(alarm(hour = 11, minute = 0), now, zone)
        assertEquals(expectedMillis(now.toLocalDate(), 11, 0), result)
    }

    @Test
    fun `one-time alarm earlier today schedules tomorrow`() {
        val result = AlarmTiming.nextTriggerMillis(alarm(hour = 9, minute = 0), now, zone)
        assertEquals(expectedMillis(now.toLocalDate().plusDays(1), 9, 0), result)
    }

    @Test
    fun `one-time alarm exactly at now schedules tomorrow, not today`() {
        val result = AlarmTiming.nextTriggerMillis(alarm(hour = now.hour, minute = now.minute), now, zone)
        assertEquals(expectedMillis(now.toLocalDate().plusDays(1), now.hour, now.minute), result)
    }

    @Test
    fun `repeating alarm matching today and not yet passed schedules today`() {
        val a = alarm(hour = 11, minute = 0, repeatDays = setOf(now.dayOfWeek))
        val result = AlarmTiming.nextTriggerMillis(a, now, zone)
        assertEquals(expectedMillis(now.toLocalDate(), 11, 0), result)
    }

    @Test
    fun `repeating alarm matching today but already passed skips to next week`() {
        // Only repeats on today's weekday, so the next occurrence is 7 days out.
        val a = alarm(hour = 9, minute = 0, repeatDays = setOf(now.dayOfWeek))
        val result = AlarmTiming.nextTriggerMillis(a, now, zone)
        assertEquals(expectedMillis(now.toLocalDate().plusDays(7), 9, 0), result)
    }

    @Test
    fun `repeating alarm on a future day this week schedules that day`() {
        val a = alarm(hour = 8, minute = 0, repeatDays = setOf(now.dayOfWeek.plus(3)))
        val result = AlarmTiming.nextTriggerMillis(a, now, zone)
        assertEquals(expectedMillis(now.toLocalDate().plusDays(3), 8, 0), result)
    }

    @Test
    fun `repeating alarm on every day and already passed today schedules tomorrow`() {
        val a = alarm(hour = 9, minute = 0, repeatDays = DayOfWeek.entries.toSet())
        val result = AlarmTiming.nextTriggerMillis(a, now, zone)
        assertEquals(expectedMillis(now.toLocalDate().plusDays(1), 9, 0), result)
    }

    @Test
    fun `repeating alarm picks nearest of multiple matching days`() {
        val a = alarm(
            hour = 8,
            minute = 0,
            repeatDays = setOf(now.dayOfWeek.plus(2), now.dayOfWeek.plus(5)),
        )
        val result = AlarmTiming.nextTriggerMillis(a, now, zone)
        assertEquals(expectedMillis(now.toLocalDate().plusDays(2), 8, 0), result)
    }
}
