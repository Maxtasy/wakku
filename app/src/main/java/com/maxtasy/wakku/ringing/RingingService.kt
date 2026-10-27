package com.maxtasy.wakku.ringing

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.maxtasy.wakku.R
import com.maxtasy.wakku.WakkuApplication
import com.maxtasy.wakku.data.Alarm
import com.maxtasy.wakku.scheduling.AlarmScheduler
import com.maxtasy.wakku.scheduling.TriggerKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Rings an alarm: loops the current alarm sound + vibration and shows a full-screen notification. */
class RingingService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var volumeRamp: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopRinging()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_DISMISS_SNOOZE -> {
                dismissSnooze(intent.getLongExtra(EXTRA_ALARM_ID, -1L))
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
            val app = application as WakkuApplication
            val defaults = app.settings.current()
            val alarm = app.database.alarmDao().getById(alarmId)
            val numberOfShakes = alarm?.numberOfShakes ?: defaults.numberOfShakes
            val vibrationEnabled = alarm?.vibrationEnabled ?: defaults.vibrationEnabled
            val soundUri = alarm?.soundUri ?: defaults.soundUri
            val gradualVolume = alarm?.gradualVolume ?: defaults.gradualVolume
            // Before the notification: its full-screen intent can open the
            // activity within milliseconds, and the activity closes itself when
            // it sees no matching ringing alarm (that was the black lock screen).
            RingingController.ringingAlarmId.value = alarmId
            // With the overlay permission the screen is opened right here, so the
            // notification can stay quiet; it turns loud only if that fails.
            val openDirectly = Settings.canDrawOverlays(this@RingingService)
            startForeground(
                NOTIFICATION_ID,
                buildNotification(alarmId, hour, minute, label, numberOfShakes, loud = !openDirectly),
            )
            startSound(soundUri, gradualVolume)
            if (vibrationEnabled) startVibration()
            if (openDirectly) openRingingScreen(alarmId, hour, minute, label, numberOfShakes)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRinging()
    }

    /**
     * Opens the full-screen ringing view directly. With the phone unlocked,
     * Android only turns a notification's full-screen intent into a heads-up;
     * a background app may start an activity itself only with the "display
     * over other apps" permission (see the banner on the alarm list), and some
     * OEMs add permissions of their own. Whether it worked can only be seen by
     * the screen actually appearing, so wait for it and otherwise fall back to
     * the loud notification, remembering the failure for the banner.
     */
    private suspend fun openRingingScreen(alarmId: Long, hour: Int, minute: Int, label: String, numberOfShakes: Int) {
        RingingController.screenVisible.value = false
        try {
            startActivity(RingingActivity.intent(this, alarmId, hour, minute, label, numberOfShakes))
        } catch (_: RuntimeException) {
            // Handled like any other failure to appear, below.
        }
        val shown = withTimeoutOrNull(SCREEN_TIMEOUT_MILLIS) {
            RingingController.screenVisible.first { it }
        } != null
        FullScreenStatus.setBlocked(this, !shown)
        // Only while still ringing: the user may have acted on the quiet notification meanwhile.
        if (!shown && RingingController.ringingAlarmId.value == alarmId) {
            getSystemService(NotificationManager::class.java).notify(
                NOTIFICATION_ID,
                buildNotification(alarmId, hour, minute, label, numberOfShakes, loud = true),
            )
        }
    }

    private fun buildNotification(
        alarmId: Long,
        hour: Int,
        minute: Int,
        label: String,
        numberOfShakes: Int,
        loud: Boolean,
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

        // Opens the ringing screen already in the shake challenge; the screen
        // itself silences the alarm on entering it, same as tapping its Stop button.
        val stopIntent = PendingIntent.getActivity(
            this,
            (alarmId + REQUEST_CODE_STOP_OFFSET).toInt(),
            RingingActivity.intent(this, alarmId, hour, minute, label, numberOfShakes, startChallenge = true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, if (loud) CHANNEL_ID else QUIET_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle("%02d:%02d".format(hour, minute))
            .setContentText(label.ifBlank { getString(R.string.default_alarm_label) })
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(if (loud) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(fullScreenIntent)
            .apply { if (loud) setFullScreenIntent(fullScreenIntent, true) }
            .addAction(0, getString(R.string.snooze), snoozeIntent)
            .addAction(0, getString(R.string.stop), stopIntent)
            .build()
    }

    private fun startSound(soundUri: String?, gradualVolume: Boolean) {
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
            if (gradualVolume) setVolume(VOLUME_RAMP_START, VOLUME_RAMP_START)
            prepare()
            start()
        }
        if (gradualVolume) startVolumeRamp()
    }

    /** Raises the player's volume from [VOLUME_RAMP_START] to full over [VOLUME_RAMP_MILLIS]. */
    private fun startVolumeRamp() {
        volumeRamp = scope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            while (true) {
                val progress = ((SystemClock.elapsedRealtime() - startedAt).toFloat() / VOLUME_RAMP_MILLIS)
                    .coerceIn(0f, 1f)
                // Squared so the first seconds stay quiet; loudness is perceived
                // roughly logarithmically, so a linear ramp sounds like it jumps early.
                val volume = VOLUME_RAMP_START + (1f - VOLUME_RAMP_START) * progress * progress
                mediaPlayer?.setVolume(volume, volume)
                if (progress >= 1f) break
                delay(VOLUME_RAMP_STEP_MILLIS)
            }
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
        volumeRamp?.cancel()
        volumeRamp = null
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
            val snoozeMinutes = alarm.snoozeMinutes ?: app.settings.current().snoozeMinutes
            val triggerAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
            // scheduleAt also updates the persistent "next alarm" notification.
            AlarmScheduler(applicationContext).scheduleAt(alarm, triggerAt, TriggerKind.SNOOZED)
        }
    }

    /** The shake challenge was completed: drop any snooze and fall back to the alarm's normal schedule (one-time alarms end up off). */
    private fun dismissSnooze(alarmId: Long) {
        if (alarmId == -1L) return
        scope.launch {
            val app = application as WakkuApplication
            val dao = app.database.alarmDao()
            val alarm = dao.getById(alarmId) ?: return@launch
            val scheduler = AlarmScheduler(applicationContext)
            if (alarm.repeatDays.isEmpty()) {
                dao.setEnabled(alarmId, false)
                scheduler.cancel(alarm)
            } else {
                scheduler.schedule(alarm)
            }
        }
    }

    companion object {
        const val ACTION_STOP = "com.maxtasy.wakku.action.STOP_RINGING"
        const val ACTION_SNOOZE = "com.maxtasy.wakku.action.SNOOZE"
        const val ACTION_DISMISS_SNOOZE = "com.maxtasy.wakku.action.DISMISS_SNOOZE"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_HOUR = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_NUMBER_OF_SHAKES = "extra_number_of_shakes"

        const val CHANNEL_ID = "alarms"

        /** Same notification without the heads-up, for when the ringing screen opens by itself. */
        const val QUIET_CHANNEL_ID = "alarms_quiet"
        private const val SCREEN_TIMEOUT_MILLIS = 2_000L
        private const val NOTIFICATION_ID = 1
        private const val REQUEST_CODE_SNOOZE_OFFSET = 1_000_000
        private const val REQUEST_CODE_STOP_OFFSET = 3_000_000
        private val VIBRATION_PATTERN = longArrayOf(0, 1000, 1000)
        private const val VOLUME_RAMP_MILLIS = 45_000L
        private const val VOLUME_RAMP_STEP_MILLIS = 250L
        private const val VOLUME_RAMP_START = 0.05f

        fun intent(context: Context, alarm: Alarm): Intent =
            Intent(context, RingingService::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarm.id)
                putExtra(EXTRA_HOUR, alarm.hour)
                putExtra(EXTRA_MINUTE, alarm.minute)
                putExtra(EXTRA_LABEL, alarm.label)
            }
    }
}
