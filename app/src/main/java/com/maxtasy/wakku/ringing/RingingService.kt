package com.maxtasy.wakku.ringing

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.maxtasy.wakku.R
import com.maxtasy.wakku.WakkuApplication
import com.maxtasy.wakku.data.Alarm
import com.maxtasy.wakku.scheduling.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/** Rings an alarm: loops the current alarm sound + vibration and shows a full-screen notification. */
class RingingService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopRinging()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                snooze(intent.getLongExtra(EXTRA_ALARM_ID, -1L))
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L
        val hour = intent?.getIntExtra(EXTRA_HOUR, 0) ?: 0
        val minute = intent?.getIntExtra(EXTRA_MINUTE, 0) ?: 0
        val label = intent?.getStringExtra(EXTRA_LABEL).orEmpty()

        scope.launch {
            val settings = (application as WakkuApplication).settings.current()
            startForeground(
                NOTIFICATION_ID,
                buildNotification(alarmId, hour, minute, label, settings.numberOfShakes),
            )
            RingingController.ringingAlarmId.value = alarmId
            startSound(settings.soundUri)
            if (settings.vibrationEnabled) startVibration()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRinging()
    }

    private fun buildNotification(
        alarmId: Long,
        hour: Int,
        minute: Int,
        label: String,
        numberOfShakes: Int,
    ): Notification {
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            RingingActivity.intent(this, alarmId, hour, minute, label, numberOfShakes),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozeIntent = PendingIntent.getService(
            this,
            (alarmId + REQUEST_CODE_SNOOZE_OFFSET).toInt(),
            Intent(this, RingingService::class.java)
                .setAction(ACTION_SNOOZE)
                .putExtra(EXTRA_ALARM_ID, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle("%02d:%02d".format(hour, minute))
            .setContentText(label.ifBlank { "Alarm" })
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(fullScreenIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            // No quick "Stop" action here on purpose: stopping has to go
            // through the shake challenge in RingingActivity, not a single
            // tap from the notification shade.
            .addAction(0, "Snooze", snoozeIntent)
            .build()
    }

    private fun startSound(soundUri: String?) {
        val uri = soundUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setDataSource(this@RingingService, uri)
            isLooping = true
            prepare()
            start()
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, 0))
    }

    private fun stopRinging() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        RingingController.ringingAlarmId.value = null
    }

    private fun snooze(alarmId: Long) {
        stopRinging()
        if (alarmId == -1L) return
        scope.launch {
            val app = application as WakkuApplication
            val dao = app.database.alarmDao()
            val alarm = dao.getById(alarmId) ?: return@launch
            // A one-time alarm gets disabled the instant it first rings (see
            // AlarmReceiver); snoozing means it's still genuinely pending, so
            // the list shouldn't show it as off while that's true.
            dao.setEnabled(alarmId, true)
            val snoozeMinutes = app.settings.current().snoozeMinutes
            val triggerAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
            AlarmScheduler(applicationContext).scheduleAt(alarm, triggerAt)
            showSnoozedNotification(alarmId, triggerAt)
        }
    }

    private fun showSnoozedNotification(alarmId: Long, triggerAtMillis: Long) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val triggerTime = Instant.ofEpochMilli(triggerAtMillis).atZone(ZoneId.systemDefault()).toLocalTime()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle("Snoozed")
            .setContentText("Rings again at %02d:%02d".format(triggerTime.hour, triggerTime.minute))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(snoozedNotificationId(alarmId), notification)
    }

    private fun snoozedNotificationId(alarmId: Long): Int =
        (SNOOZED_NOTIFICATION_ID_OFFSET + alarmId).toInt()

    companion object {
        const val ACTION_STOP = "com.maxtasy.wakku.action.STOP_RINGING"
        const val ACTION_SNOOZE = "com.maxtasy.wakku.action.SNOOZE"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_HOUR = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_NUMBER_OF_SHAKES = "extra_number_of_shakes"

        const val CHANNEL_ID = "alarms"
        private const val NOTIFICATION_ID = 1
        private const val REQUEST_CODE_SNOOZE_OFFSET = 1_000_000
        private const val SNOOZED_NOTIFICATION_ID_OFFSET = 2_000_000
        private val VIBRATION_PATTERN = longArrayOf(0, 1000, 1000)

        fun intent(context: Context, alarm: Alarm): Intent =
            Intent(context, RingingService::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarm.id)
                putExtra(EXTRA_HOUR, alarm.hour)
                putExtra(EXTRA_MINUTE, alarm.minute)
                putExtra(EXTRA_LABEL, alarm.label)
            }
    }
}
