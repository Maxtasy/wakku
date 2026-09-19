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
    fun record(context: Context, alarmId: Long, triggerAtMillis: Long) {
        prefs(context).edit().putLong(alarmId.toString(), triggerAtMillis).apply()
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
        val next = prefs(context).all.values.filterIsInstance<Long>().filter { it > now }.minOrNull()
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
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle("Next alarm")
            .setContentText(format(next))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openApp)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
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
