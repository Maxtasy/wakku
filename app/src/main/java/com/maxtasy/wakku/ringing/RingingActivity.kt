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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxtasy.wakku.shake.ShakeDetector
import com.maxtasy.wakku.ui.theme.WakkuTheme

/** Shows over the lock screen when an alarm fires, via RingingService's full-screen notification intent. */
class RingingActivity : ComponentActivity() {

    private var alarmId: Long = -1L

    // Tracks the shake challenge across leaving-the-screen paths (back, home,
    // task switch) so onStop can fall back to snoozing if it wasn't finished
    // some other way (completed, or the explicit "Snooze instead" tap).
    private var shakeChallengeActive = false
    private var shakeChallengeResolved = false

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
        val hour = intent.getIntExtra(RingingService.EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(RingingService.EXTRA_MINUTE, 0)
        val label = intent.getStringExtra(RingingService.EXTRA_LABEL).orEmpty()

        setContent {
            WakkuTheme {
                val ringingId by RingingController.ringingAlarmId.collectAsStateWithLifecycle()
                RingingScreen(
                    ringingId = ringingId,
                    alarmId = alarmId,
                    hour = hour,
                    minute = minute,
                    label = label,
                    onFinish = ::finish,
                    onSnooze = { sendServiceAction(RingingService.ACTION_SNOOZE); finish() },
                    onStopTapped = {
                        sendServiceAction(RingingService.ACTION_STOP)
                        shakeChallengeActive = true
                        shakeChallengeResolved = false
                    },
                    onChallengeCompleted = {
                        shakeChallengeResolved = true
                        finish()
                    },
                    onChallengeAbandoned = {
                        shakeChallengeResolved = true
                        sendServiceAction(RingingService.ACTION_SNOOZE)
                        finish()
                    },
                )
            }
        }
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

    private fun sendServiceAction(action: String) {
        startService(
            Intent(this, RingingService::class.java)
                .setAction(action)
                .putExtra(RingingService.EXTRA_ALARM_ID, alarmId),
        )
    }

    companion object {
        fun intent(context: Context, alarmId: Long, hour: Int, minute: Int, label: String): Intent =
            Intent(context, RingingActivity::class.java).apply {
                putExtra(RingingService.EXTRA_ALARM_ID, alarmId)
                putExtra(RingingService.EXTRA_HOUR, hour)
                putExtra(RingingService.EXTRA_MINUTE, minute)
                putExtra(RingingService.EXTRA_LABEL, label)
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
private fun RingingScreen(
    ringingId: Long?,
    alarmId: Long,
    hour: Int,
    minute: Int,
    label: String,
    onFinish: () -> Unit,
    onSnooze: () -> Unit,
    onStopTapped: () -> Unit,
    onChallengeCompleted: () -> Unit,
    onChallengeAbandoned: () -> Unit,
) {
    var isShaking by rememberSaveable { mutableStateOf(false) }
    var shakeCount by rememberSaveable { mutableIntStateOf(0) }

    // Only auto-close for *external* dismissal (e.g. the notification's own
    // actions) — once we're in the challenge, we silenced things ourselves.
    LaunchedEffect(ringingId, isShaking) {
        if (!isShaking && ringingId != alarmId) onFinish()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (isShaking) {
            ShakeChallenge(
                shakeCount = shakeCount,
                requiredShakes = RingingService.NUMBER_OF_SHAKES,
                onShake = { shakeCount++ },
                onCancel = onChallengeAbandoned,
                onComplete = onChallengeCompleted,
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Spacer(Modifier.height(48.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%02d:%02d".format(hour, minute),
                        style = MaterialTheme.typography.displayLarge,
                    )
                    if (label.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(text = label, style = MaterialTheme.typography.titleMedium)
                    }
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
            Text("Snooze instead")
        }
    }
}
