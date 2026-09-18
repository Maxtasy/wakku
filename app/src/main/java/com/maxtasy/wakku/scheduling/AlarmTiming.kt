package com.maxtasy.wakku.scheduling

import com.maxtasy.wakku.data.Alarm
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
}
