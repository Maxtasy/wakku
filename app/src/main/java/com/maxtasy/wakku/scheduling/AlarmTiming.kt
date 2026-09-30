package com.maxtasy.wakku.scheduling

import com.maxtasy.wakku.data.Alarm
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Pure next-trigger-time math, kept separate from [AlarmScheduler]'s AlarmManager/
 * PendingIntent glue so it can be unit tested without Android framework classes
 * (Milestone 8) — mirrors the [com.maxtasy.wakku.shake.ShakeCounter] split.
 */
object AlarmTiming {
    fun nextTriggerMillis(alarm: Alarm, now: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Long {
        val today = now.toLocalDate()
        val timeToday = today.atTime(alarm.hour, alarm.minute)

        val triggerDate = if (alarm.repeatDays.isEmpty()) {
            if (timeToday.isAfter(now)) today else today.plusDays(1)
        } else {
            (0..7)
                .map { offset -> today.plusDays(offset.toLong()) }
                .first { date ->
                    date.dayOfWeek in alarm.repeatDays && (date != today || timeToday.isAfter(now))
                }
        }
        return triggerDate.atTime(alarm.hour, alarm.minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    /** The occurrence after the next one, for "skip next" on a repeating alarm. */
    fun triggerAfterNextMillis(alarm: Alarm, now: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Long {
        val next = nextTriggerMillis(alarm, now, zone)
        val nextDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(next), zone)
        return nextTriggerMillis(alarm, nextDateTime, zone)
    }

    /** A time span split into the parts shown in the "Rings in …" message. */
    data class DurationParts(val days: Int, val hours: Int, val minutes: Int)

    /**
     * Time from [nowMillis] until [triggerAtMillis], with a partial minute
     * rounded up (an alarm 30 s away is "1 min", never "0 min").
     */
    fun durationUntil(triggerAtMillis: Long, nowMillis: Long): DurationParts {
        val totalMinutes = ((triggerAtMillis - nowMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
        return DurationParts(
            days = (totalMinutes / (24 * 60)).toInt(),
            hours = (totalMinutes / 60 % 24).toInt(),
            minutes = (totalMinutes % 60).toInt(),
        )
    }
}
