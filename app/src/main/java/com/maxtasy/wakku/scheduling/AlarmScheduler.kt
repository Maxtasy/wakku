package com.maxtasy.wakku.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.maxtasy.wakku.MainActivity
import com.maxtasy.wakku.data.Alarm
import java.time.LocalDateTime

/**
 * Wraps [AlarmManager.setAlarmClock], which is exempt from Doze/App Standby
 * and puts the usual alarm icon in the status bar for free. Still needs the
 * USE_EXACT_ALARM permission (declared in the manifest), which is
 * auto-granted at install for apps whose core function is alarms.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)!!

    /**
     * Schedules the alarm's regular next occurrence, discarding any snooze or
     * skip. Returns the trigger time, or null if the alarm is disabled (and so
     * was cancelled instead).
     */
    fun schedule(alarm: Alarm): Long? {
        if (!alarm.enabled) {
            cancel(alarm)
            return null
        }
        val triggerAt = AlarmTiming.nextTriggerMillis(alarm, LocalDateTime.now())
        scheduleAt(alarm, triggerAt)
        return triggerAt
    }

    /**
     * Re-registers an alarm after AlarmManager lost it (reboot) or the app
     * process restarted. Unlike [schedule] this keeps a still-pending snooze
     * or skip instead of resetting the alarm to its regular time.
     */
    fun restore(alarm: Alarm) {
        if (!alarm.enabled) {
            cancel(alarm)
            return
        }
        val recorded = NextAlarmNotifier.recorded(context, alarm.id)
        if (recorded != null && recorded.kind != TriggerKind.NORMAL &&
            recorded.triggerAtMillis > System.currentTimeMillis()
        ) {
            scheduleAt(alarm, recorded.triggerAtMillis, recorded.kind)
        } else {
            schedule(alarm)
        }
    }

    /**
     * Skips the next occurrence of a repeating alarm and schedules the one
     * after it. Returns the new trigger time, or null if there was nothing to skip.
     */
    fun skipNext(alarm: Alarm): Long? {
        if (!alarm.enabled || alarm.repeatDays.isEmpty()) return null
        val triggerAt = AlarmTiming.triggerAfterNextMillis(alarm, LocalDateTime.now())
        scheduleAt(alarm, triggerAt, TriggerKind.SKIPPED)
        return triggerAt
    }

    /** Schedules at an explicit time, e.g. "snooze N minutes from now" rather than the next matching day. */
    fun scheduleAt(alarm: Alarm, triggerAtMillis: Long, kind: TriggerKind = TriggerKind.NORMAL) {
        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
            firingOperation(alarm.id),
        )
        NextAlarmNotifier.record(context, alarm.id, triggerAtMillis, kind, repeating = alarm.repeatDays.isNotEmpty())
    }

    fun cancel(alarm: Alarm) {
        alarmManager.cancel(firingOperation(alarm.id))
        NextAlarmNotifier.remove(context, alarm.id)
    }

    private fun firingOperation(alarmId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            Intent(context, AlarmReceiver::class.java).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
