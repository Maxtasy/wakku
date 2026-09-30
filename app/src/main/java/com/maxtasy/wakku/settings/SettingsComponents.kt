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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.maxtasy.wakku.R
import kotlin.math.roundToInt

/** Shared between the global Settings screen and per-alarm override editing. */
@Composable
fun SettingSlider(
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

/**
 * A label + switch row exposed to screen readers as one toggleable node
 * (the Switch itself has no click handler of its own).
 */
@Composable
fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            )
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = labelStyle)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun VibrationSwitchRow(vibrationEnabled: Boolean, onVibrationEnabledChange: (Boolean) -> Unit) {
    SettingSwitchRow(
        label = stringResource(R.string.vibrate),
        checked = vibrationEnabled,
        onCheckedChange = onVibrationEnabledChange,
    )
}

@Composable
fun GradualVolumeSwitchRow(gradualVolume: Boolean, onGradualVolumeChange: (Boolean) -> Unit) {
    SettingSwitchRow(
        label = stringResource(R.string.gradual_volume),
        checked = gradualVolume,
        onCheckedChange = onGradualVolumeChange,
    )
}

@Composable
fun SoundPickerRow(soundUri: String?, onSoundUriChange: (String?) -> Unit) {
    val context = LocalContext.current
    val defaultSoundName = stringResource(R.string.default_alarm_sound)
    var soundName by remember { mutableStateOf(defaultSoundName) }

    LaunchedEffect(soundUri, defaultSoundName) {
        val uri = soundUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
        soundName = uri?.let { RingtoneManager.getRingtone(context, it)?.getTitle(context) }
            ?: defaultSoundName
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
            .clickable(onClickLabel = stringResource(R.string.change_alarm_sound)) {
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
            }
            .semantics(mergeDescendants = true) { role = Role.Button },
    ) {
        Text(stringResource(R.string.alarm_sound), style = MaterialTheme.typography.titleMedium)
        Text(soundName, style = MaterialTheme.typography.bodyMedium)
    }
}
