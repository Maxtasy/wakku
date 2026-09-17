package com.maxtasy.wakku.ringing

import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxtasy.wakku.ui.theme.WakkuTheme

/** Shows over the lock screen when an alarm fires, via RingingService's full-screen notification intent. */
class RingingActivity : ComponentActivity() {

    private var alarmId: Long = -1L

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
                LaunchedEffect(ringingId) {
                    if (ringingId != alarmId) finish()
                }
                RingingScreen(
                    hour = hour,
                    minute = minute,
                    label = label,
                    onSnooze = { sendServiceAction(RingingService.ACTION_SNOOZE); finish() },
                    onStop = { sendServiceAction(RingingService.ACTION_STOP); finish() },
                )
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

@Composable
private fun RingingScreen(
    hour: Int,
    minute: Int,
    label: String,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
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
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text("Stop")
                }
            }
        }
    }
}
