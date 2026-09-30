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

    @Test
    fun `skipping the next occurrence of an every-day alarm lands on the day after`() {
        val a = alarm(hour = 9, minute = 0, repeatDays = DayOfWeek.entries.toSet())
        // Next is tomorrow 09:00, so the skip target is the day after tomorrow.
        val result = AlarmTiming.triggerAfterNextMillis(a, now, zone)
        assertEquals(expectedMillis(now.toLocalDate().plusDays(2), 9, 0), result)
    }

    @Test
    fun `skipping when the next occurrence is later today lands on the following matching day`() {
        val a = alarm(hour = 11, minute = 0, repeatDays = DayOfWeek.entries.toSet())
        val result = AlarmTiming.triggerAfterNextMillis(a, now, zone)
        assertEquals(expectedMillis(now.toLocalDate().plusDays(1), 11, 0), result)
    }

    @Test
    fun `skipping a weekly alarm lands a full week after the next occurrence`() {
        val a = alarm(hour = 8, minute = 0, repeatDays = setOf(now.dayOfWeek.plus(3)))
        val result = AlarmTiming.triggerAfterNextMillis(a, now, zone)
        assertEquals(expectedMillis(now.toLocalDate().plusDays(10), 8, 0), result)
    }

    @Test
    fun `skipping with several repeat days jumps to the second-nearest day`() {
        val a = alarm(
            hour = 8,
            minute = 0,
            repeatDays = setOf(now.dayOfWeek.plus(2), now.dayOfWeek.plus(5)),
        )
        val result = AlarmTiming.triggerAfterNextMillis(a, now, zone)
        assertEquals(expectedMillis(now.toLocalDate().plusDays(5), 8, 0), result)
    }

    private fun minutes(n: Long) = n * 60_000L

    @Test
    fun `duration splits into days hours and minutes`() {
        val parts = AlarmTiming.durationUntil(minutes(2 * 24 * 60 + 3 * 60 + 10), 0L)
        assertEquals(AlarmTiming.DurationParts(days = 2, hours = 3, minutes = 10), parts)
    }

    @Test
    fun `duration under a day has no days`() {
        val parts = AlarmTiming.durationUntil(minutes(7 * 60 + 32), 0L)
        assertEquals(AlarmTiming.DurationParts(days = 0, hours = 7, minutes = 32), parts)
    }

    @Test
    fun `a partial minute is rounded up`() {
        assertEquals(AlarmTiming.DurationParts(0, 0, 1), AlarmTiming.durationUntil(30_000L, 0L))
        assertEquals(AlarmTiming.DurationParts(0, 0, 2), AlarmTiming.durationUntil(61_000L, 0L))
    }

    @Test
    fun `an exact hour has zero minutes`() {
        assertEquals(AlarmTiming.DurationParts(0, 8, 0), AlarmTiming.durationUntil(minutes(8 * 60), 0L))
    }

    @Test
    fun `duration is at least one minute even when already due`() {
        assertEquals(AlarmTiming.DurationParts(0, 0, 1), AlarmTiming.durationUntil(1_000L, 5_000L))
    }

    @Test
    fun `duration is measured from now not from the epoch`() {
        val now = 1_000_000_000L
        val parts = AlarmTiming.durationUntil(now + minutes(90), now)
        assertEquals(AlarmTiming.DurationParts(0, 1, 30), parts)
    }
}
