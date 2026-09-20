package com.maxtasy.wakku.ringing

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.maxtasy.wakku.WakkuApplication
import com.maxtasy.wakku.settings.AppSettings
import com.maxtasy.wakku.shake.ShakeDetector
import com.maxtasy.wakku.ui.theme.WakkuTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime

/** Shows over the lock screen when an alarm fires, via RingingService's full-screen notification intent. */
class RingingActivity : ComponentActivity() {

    private var alarmId: Long = -1L

    // Tracks the shake challenge across leaving-the-screen paths (back, home,
    // task switch) so onStop can fall back to snoozing if it wasn't finished
    // some other way (completed, or the explicit "Snooze instead" tap).
    private var shakeChallengeActive = false
    private var shakeChallengeResolved = false

    // Set by the notification's "Stop" action to jump straight into the challenge.
    private var startChallenge by mutableStateOf(false)

    // Opened from the "Next alarm" notification while an alarm is snoozed:
    // nothing is ringing, and finishing the challenge cancels the pending snooze.
    private var dismissingSnooze = false
    private var numberOfShakes by mutableIntStateOf(AppSettings.DEFAULT_NUMBER_OF_SHAKES)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        alarmId = intent.getLongExtra(RingingService.EXTRA_ALARM_ID, -1L)
        startChallenge = intent.getBooleanExtra(EXTRA_START_CHALLENGE, false)
        dismissingSnooze = intent.getBooleanExtra(EXTRA_DISMISS_SNOOZE, false)
        val hour = intent.getIntExtra(RingingService.EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(RingingService.EXTRA_MINUTE, 0)
        val label = intent.getStringExtra(RingingService.EXTRA_LABEL).orEmpty()
        numberOfShakes = intent.getIntExtra(
            RingingService.EXTRA_NUMBER_OF_SHAKES,
            AppSettings.DEFAULT_NUMBER_OF_SHAKES,
        )
        if (dismissingSnooze) loadAlarmAndStartChallenge()

        setContent {
            WakkuTheme {
                val ringingId by RingingController.ringingAlarmId.collectAsStateWithLifecycle()
                RingingScreen(
                    ringingId = ringingId,
                    alarmId = alarmId,
                    currentTime = rememberCurrentTime(),
                    startChallenge = startChallenge,
                    dismissingSnooze = dismissingSnooze,
                    hour = hour,
                    minute = minute,
                    label = label,
                    requiredShakes = numberOfShakes,
                    onFinish = ::finish,
                    onSnooze = { sendServiceAction(RingingService.ACTION_SNOOZE); finish() },
                    onStopTapped = {
                        // Nothing is ringing while dismissing a snooze, and
                        // leaving mid-challenge must keep the existing snooze.
                        if (!dismissingSnooze) {
                            sendServiceAction(RingingService.ACTION_STOP)
                            shakeChallengeActive = true
                            shakeChallengeResolved = false
                        }
                    },
                    onChallengeCompleted = {
                        shakeChallengeResolved = true
                        if (dismissingSnooze) sendServiceAction(RingingService.ACTION_DISMISS_SNOOZE)
                        finish()
                    },
                    onChallengeAbandoned = {
                        shakeChallengeResolved = true
                        if (!dismissingSnooze) sendServiceAction(RingingService.ACTION_SNOOZE)
                        finish()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_START_CHALLENGE, false)) startChallenge = true
    }

    override fun onStop() {
        super.onStop()
        // Walked away (back/home/task switch) mid-challenge without finishing
        // or explicitly bailing out: treat it the same as tapping Snooze.
        if (isChangingConfigurations) return
        if (shakeChallengeActive && !shakeChallengeResolved) {
            shakeChallengeResolved = true
            sendServiceAction(RingingService.ACTION_SNOOZE)
        }
    }

    private fun loadAlarmAndStartChallenge() {
        lifecycleScope.launch {
            val app = application as WakkuApplication
            val shakes = withContext(Dispatchers.IO) {
                val alarm = app.database.alarmDao().getById(alarmId) ?: return@withContext null
                alarm.numberOfShakes ?: app.settings.current().numberOfShakes
            }
            if (shakes == null) {
                finish()
            } else {
                numberOfShakes = shakes
                startChallenge = true
            }
        }
    }

    private fun sendServiceAction(action: String) {
        startService(
            Intent(this, RingingService::class.java)
                .setAction(action)
                .putExtra(RingingService.EXTRA_ALARM_ID, alarmId),
        )
    }

    companion object {
        const val EXTRA_START_CHALLENGE = "extra_start_challenge"
        const val EXTRA_DISMISS_SNOOZE = "extra_dismiss_snooze"

        /** Opens the shake challenge for an alarm that is snoozed (not ringing). */
        fun dismissSnoozeIntent(context: Context, alarmId: Long): Intent =
            Intent(context, RingingActivity::class.java).apply {
                putExtra(RingingService.EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_DISMISS_SNOOZE, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        fun intent(
            context: Context,
            alarmId: Long,
            hour: Int,
            minute: Int,
            label: String,
            numberOfShakes: Int,
            startChallenge: Boolean = false,
        ): Intent =
            Intent(context, RingingActivity::class.java).apply {
                putExtra(RingingService.EXTRA_ALARM_ID, alarmId)
                putExtra(RingingService.EXTRA_HOUR, hour)
                putExtra(RingingService.EXTRA_MINUTE, minute)
                putExtra(RingingService.EXTRA_LABEL, label)
                putExtra(RingingService.EXTRA_NUMBER_OF_SHAKES, numberOfShakes)
                putExtra(EXTRA_START_CHALLENGE, startChallenge)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }
}

/**
 * Stop silences the alarm immediately and starts the shake challenge; only
 * finishing that challenge actually dismisses the alarm. Not finishing it —
 * cancelling, or just walking away — falls back to snoozing instead, so
 * silence alone is never enough to make the alarm go away for good.
 */
@Composable
internal fun RingingScreen(
    ringingId: Long?,
    alarmId: Long,
    currentTime: LocalTime,
    startChallenge: Boolean,
    dismissingSnooze: Boolean,
    hour: Int,
    minute: Int,
    label: String,
    requiredShakes: Int,
    onFinish: () -> Unit,
    onSnooze: () -> Unit,
    onStopTapped: () -> Unit,
    onChallengeCompleted: () -> Unit,
    onChallengeAbandoned: () -> Unit,
) {
    var isShaking by rememberSaveable { mutableStateOf(false) }
    var shakeCount by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(startChallenge) {
        if (startChallenge && !isShaking) {
            onStopTapped()
            isShaking = true
            shakeCount = 0
        }
    }

    // Only auto-close for *external* dismissal (e.g. the notification's own
    // actions) — once we're in the challenge, we silenced things ourselves.
    LaunchedEffect(ringingId, isShaking) {
        if (!isShaking && !dismissingSnooze && ringingId != alarmId) onFinish()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (isShaking) {
            ShakeChallenge(
                shakeCount = shakeCount,
                requiredShakes = requiredShakes,
                onShake = { shakeCount++ },
                cancelLabel = if (dismissingSnooze) "Cancel" else "Snooze instead",
                onCancel = onChallengeAbandoned,
                onComplete = onChallengeCompleted,
            )
        } else if (dismissingSnooze) {
            // Blank until the alarm's shake count has loaded and the challenge starts.
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Spacer(Modifier.height(48.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%02d:%02d".format(currentTime.hour, currentTime.minute),
                        style = MaterialTheme.typography.displayLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = label.ifBlank { "Alarm" },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Set for %02d:%02d".format(hour, minute),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Text("Snooze")
                    }
                    Button(
                        onClick = {
                            onStopTapped()
                            isShaking = true
                            shakeCount = 0
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Text("Stop")
                    }
                }
            }
        }
    }
}

@Composable
private fun ShakeChallenge(
    shakeCount: Int,
    requiredShakes: Int,
    onShake: () -> Unit,
    cancelLabel: String,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val detector = ShakeDetector(onShake = onShake)
        sensorManager.registerListener(detector, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(detector) }
    }

    LaunchedEffect(shakeCount) {
        if (shakeCount >= requiredShakes) onComplete()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Shake to stop", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        Text(
            text = "$shakeCount / $requiredShakes",
            style = MaterialTheme.typography.displayMedium,
        )
        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(
            progress = { (shakeCount.toFloat() / requiredShakes).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(40.dp))
        TextButton(onClick = onCancel) {
            Text(cancelLabel)
        }
    }
}

@Composable
private fun rememberCurrentTime(): LocalTime {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    return now
}
