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

    fun schedule(alarm: Alarm) {
        if (!alarm.enabled) {
            cancel(alarm)
            return
        }
        scheduleAt(alarm, AlarmTiming.nextTriggerMillis(alarm, LocalDateTime.now()))
    }

    /** Schedules at an explicit time, e.g. "snooze N minutes from now" rather than the next matching day. */
    fun scheduleAt(alarm: Alarm, triggerAtMillis: Long) {
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
    }

    fun cancel(alarm: Alarm) {
        alarmManager.cancel(firingOperation(alarm.id))
    }

    private fun firingOperation(alarmId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            Intent(context, AlarmReceiver::class.java).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
