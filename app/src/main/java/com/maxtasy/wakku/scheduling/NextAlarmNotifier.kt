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
 *
 * The records double as the source of truth for snoozes and skips, which
 * [AlarmScheduler.restore] reads back after a reboot or app restart.
 */
object NextAlarmNotifier {
    const val CHANNEL_ID = "next_alarm"
    private const val NOTIFICATION_ID = 2
    private const val PREFS = "next_alarm_triggers"
    private const val REQUEST_CODE_DISMISS_OFFSET = 4_000_000
    private const val REQUEST_CODE_SKIP_OFFSET = 5_000_000

    class Recorded(
        val alarmId: Long,
        val triggerAtMillis: Long,
        val kind: TriggerKind,
        val repeating: Boolean,
    )

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_next_alarm_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_next_alarm_description)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @Synchronized
    fun record(
        context: Context,
        alarmId: Long,
        triggerAtMillis: Long,
        kind: TriggerKind = TriggerKind.NORMAL,
        repeating: Boolean = false,
    ) {
        // "millis,kind,repeating" — kind is the TriggerKind ordinal; older
        // records only had "millis,snoozed(0/1)", which reads back the same way.
        val value = "$triggerAtMillis,${kind.ordinal},${if (repeating) 1 else 0}"
        prefs(context).edit().putString(alarmId.toString(), value).apply()
        refresh(context)
    }

    @Synchronized
    fun remove(context: Context, alarmId: Long) {
        prefs(context).edit().remove(alarmId.toString()).apply()
        refresh(context)
    }

    @Synchronized
    fun recorded(context: Context, alarmId: Long): Recorded? =
        all(context).firstOrNull { it.alarmId == alarmId }

    private fun refresh(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        val now = System.currentTimeMillis()
        val next = all(context).filter { it.triggerAtMillis > now }.minByOrNull { it.triggerAtMillis }
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
        val snoozed = next.kind == TriggerKind.SNOOZED
        val title = when (next.kind) {
            TriggerKind.SNOOZED -> R.string.notification_alarm_snoozed
            TriggerKind.SKIPPED -> R.string.notification_next_alarm_skipped
            TriggerKind.NORMAL -> R.string.notification_next_alarm
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(context.getString(title))
            .setContentText(
                if (snoozed) context.getString(R.string.notification_rings_again_at, timeText) else timeText,
            )
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openApp)
        if (snoozed) {
            // The alarm isn't ringing, so there's no other way into the shake
            // challenge until it rings again; finishing it cancels the snooze.
            val stop = PendingIntent.getActivity(
                context,
                (next.alarmId + REQUEST_CODE_DISMISS_OFFSET).toInt(),
                RingingActivity.dismissSnoozeIntent(context, next.alarmId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, context.getString(R.string.stop), stop)
        } else if (next.repeating && next.kind == TriggerKind.NORMAL) {
            // Only offered before anything was skipped, so a stray double tap
            // can't silently skip two days in a row.
            val skip = PendingIntent.getBroadcast(
                context,
                (next.alarmId + REQUEST_CODE_SKIP_OFFSET).toInt(),
                Intent(context, SkipNextReceiver::class.java).putExtra(AlarmReceiver.EXTRA_ALARM_ID, next.alarmId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, context.getString(R.string.notification_skip), skip)
        }
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun all(context: Context): List<Recorded> =
        prefs(context).all.mapNotNull { (key, value) ->
            val alarmId = key.toLongOrNull() ?: return@mapNotNull null
            val parts = (value as? String)?.split(',') ?: return@mapNotNull null
            val triggerAt = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val kind = parts.getOrNull(1)?.toIntOrNull()?.let { TriggerKind.entries.getOrNull(it) }
                ?: TriggerKind.NORMAL
            Recorded(alarmId, triggerAt, kind, repeating = parts.getOrNull(2) == "1")
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
