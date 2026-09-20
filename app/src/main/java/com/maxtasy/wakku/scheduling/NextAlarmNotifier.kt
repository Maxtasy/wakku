package com.maxtasy.wakku.scheduling

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.maxtasy.wakku.MainActivity
import com.maxtasy.wakku.R
import com.maxtasy.wakku.ringing.RingingActivity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Persistent "next alarm" notification, kept in sync by [AlarmScheduler]:
 * every schedule/cancel records or removes the alarm's trigger time here and
 * re-posts (or clears) the notification. Snoozes go through the same path, so
 * the shown time is always what AlarmManager is actually going to fire.
 */
object NextAlarmNotifier {
    const val CHANNEL_ID = "next_alarm"
    private const val NOTIFICATION_ID = 2
    private const val PREFS = "next_alarm_triggers"
    private const val REQUEST_CODE_DISMISS_OFFSET = 4_000_000

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Upcoming alarm",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows when your next alarm will ring"
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @Synchronized
    fun record(context: Context, alarmId: Long, triggerAtMillis: Long, snoozed: Boolean = false) {
        prefs(context).edit().putString(alarmId.toString(), "$triggerAtMillis,${if (snoozed) 1 else 0}").apply()
        refresh(context)
    }

    @Synchronized
    fun remove(context: Context, alarmId: Long) {
        prefs(context).edit().remove(alarmId.toString()).apply()
        refresh(context)
    }

    private fun refresh(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        val now = System.currentTimeMillis()
        val next = pending(context).filter { it.triggerAtMillis > now }.minByOrNull { it.triggerAtMillis }
        if (next == null) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val timeText = format(next.triggerAtMillis)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(if (next.snoozed) "Alarm snoozed" else "Next alarm")
            .setContentText(if (next.snoozed) "Rings again at $timeText" else timeText)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openApp)
        if (next.snoozed) {
            // The alarm isn't ringing, so there's no other way into the shake
            // challenge until it rings again; finishing it cancels the snooze.
            val stop = PendingIntent.getActivity(
                context,
                (next.alarmId + REQUEST_CODE_DISMISS_OFFSET).toInt(),
                RingingActivity.dismissSnoozeIntent(context, next.alarmId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, "Stop", stop)
        }
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    private class Pending(val alarmId: Long, val triggerAtMillis: Long, val snoozed: Boolean)

    private fun pending(context: Context): List<Pending> =
        prefs(context).all.mapNotNull { (key, value) ->
            val alarmId = key.toLongOrNull() ?: return@mapNotNull null
            val parts = (value as? String)?.split(',') ?: return@mapNotNull null
            val triggerAt = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            Pending(alarmId, triggerAt, parts.getOrNull(1) == "1")
        }

    private fun format(triggerAtMillis: Long): String {
        val dateTime = Instant.ofEpochMilli(triggerAtMillis).atZone(ZoneId.systemDefault())
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(dateTime)
        return if (dateTime.toLocalDate() == LocalDate.now()) {
            time
        } else {
            "${DateTimeFormatter.ofPattern("EEE", Locale.getDefault()).format(dateTime)} $time"
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
