package com.maxtasy.wakku.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSnoozeMinutesChange: (Int) -> Unit,
    onNumberOfShakesChange: (Int) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onSoundUriChange: (String?) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            SettingSlider(
                label = "Snooze time",
                valueLabel = "${settings.snoozeMinutes} min",
                value = settings.snoozeMinutes,
                onValueChange = onSnoozeMinutesChange,
                valueRange = 1..30,
                step = 1,
            )
            SettingSlider(
                label = "Shakes to stop",
                valueLabel = "${settings.numberOfShakes} shakes",
                value = settings.numberOfShakes,
                onValueChange = onNumberOfShakesChange,
                valueRange = 5..100,
                step = 5,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Vibrate", style = MaterialTheme.typography.titleMedium)
                Switch(checked = settings.vibrationEnabled, onCheckedChange = onVibrationEnabledChange)
            }
            SoundPickerRow(soundUri = settings.soundUri, onSoundUriChange = onSoundUriChange)
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    valueLabel: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    step: Int,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(valueLabel, style = MaterialTheme.typography.titleMedium)
        }
        val steps = (valueRange.last - valueRange.first) / step - 1
        Slider(
            value = value.toFloat(),
            onValueChange = { raw ->
                val snapped = (raw / step).roundToInt() * step
                onValueChange(snapped.coerceIn(valueRange.first, valueRange.last))
            },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = steps,
        )
    }
}

@Composable
private fun SoundPickerRow(soundUri: String?, onSoundUriChange: (String?) -> Unit) {
    val context = LocalContext.current
    var soundName by remember { mutableStateOf("Default alarm sound") }

    LaunchedEffect(soundUri) {
        val uri = soundUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
        soundName = uri?.let { RingtoneManager.getRingtone(context, it)?.getTitle(context) }
            ?: "Default alarm sound"
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val pickedUri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        onSoundUriChange(pickedUri?.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    putExtra(
                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        soundUri?.let { Uri.parse(it) }
                            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM),
                    )
                }
                pickerLauncher.launch(intent)
            },
    ) {
        Text("Alarm sound", style = MaterialTheme.typography.titleMedium)
        Text(soundName, style = MaterialTheme.typography.bodyMedium)
    }
}
