package com.maxtasy.wakku.ringing

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.maxtasy.wakku.R
import com.maxtasy.wakku.WakkuApplication
import com.maxtasy.wakku.data.Alarm
import com.maxtasy.wakku.scheduling.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Rings an alarm: loops the default alarm sound + vibration and shows a full-screen notification. */
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

        startForeground(NOTIFICATION_ID, buildNotification(alarmId, hour, minute, label))
        RingingController.ringingAlarmId.value = alarmId
        startSound()
        startVibration()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRinging()
    }

    private fun buildNotification(alarmId: Long, hour: Int, minute: Int, label: String): Notification {
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            RingingActivity.intent(this, alarmId, hour, minute, label),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            alarmId.toInt(),
            Intent(this, RingingService::class.java).setAction(ACTION_STOP),
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
            .addAction(0, "Snooze", snoozeIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun startSound() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
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
            val dao = (application as WakkuApplication).database.alarmDao()
            val alarm = dao.getById(alarmId) ?: return@launch
            val triggerAt = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
            AlarmScheduler(applicationContext).scheduleAt(alarm, triggerAt)
        }
    }

    companion object {
        const val ACTION_STOP = "com.maxtasy.wakku.action.STOP_RINGING"
        const val ACTION_SNOOZE = "com.maxtasy.wakku.action.SNOOZE"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_HOUR = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"
        const val EXTRA_LABEL = "extra_label"

        const val CHANNEL_ID = "alarms"
        private const val NOTIFICATION_ID = 1
        private const val REQUEST_CODE_SNOOZE_OFFSET = 1_000_000
        private val VIBRATION_PATTERN = longArrayOf(0, 1000, 1000)

        // Hardcoded until Milestone 6 makes it a setting.
        const val SNOOZE_MINUTES = 5

        fun intent(context: Context, alarm: Alarm): Intent =
            Intent(context, RingingService::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarm.id)
                putExtra(EXTRA_HOUR, alarm.hour)
                putExtra(EXTRA_MINUTE, alarm.minute)
                putExtra(EXTRA_LABEL, alarm.label)
            }
    }
}
