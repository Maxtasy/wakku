package com.maxtasy.wakku.alarms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxtasy.wakku.R
import com.maxtasy.wakku.settings.GradualVolumeSwitchRow
import com.maxtasy.wakku.settings.SettingSlider
import com.maxtasy.wakku.settings.SettingSwitchRow
import com.maxtasy.wakku.settings.SoundPickerRow
import com.maxtasy.wakku.settings.VibrationSwitchRow
import java.time.DayOfWeek
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    hour: Int,
    minute: Int,
    repeatDays: Set<DayOfWeek>,
    label: String,
    isNew: Boolean,
    useCustomSettings: Boolean,
    snoozeMinutes: Int,
    numberOfShakes: Int,
    vibrationEnabled: Boolean,
    gradualVolume: Boolean,
    soundUri: String?,
    onTimeChange: (Int, Int) -> Unit,
    onDaysChange: (Set<DayOfWeek>) -> Unit,
    onLabelChange: (String) -> Unit,
    onUseCustomSettingsChange: (Boolean) -> Unit,
    onSnoozeMinutesChange: (Int) -> Unit,
    onNumberOfShakesChange: (Int) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onGradualVolumeChange: (Boolean) -> Unit,
    onSoundUriChange: (String?) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isNew) R.string.new_alarm else R.string.edit_alarm)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_alarm))
                        }
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Text(
                text = "%02d:%02d".format(hour, minute),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClickLabel = stringResource(R.string.change_time), role = Role.Button) {
                        showTimePicker = true
                    }
                    .padding(horizontal = 12.dp)
                    .testTag("alarmTimeText"),
            )
            TextButton(onClick = { showTimePicker = true }) {
                Text(stringResource(R.string.change_time))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.repeat), style = MaterialTheme.typography.labelLarge)
                DayOfWeekSelector(selected = repeatDays, onDaysChange = onDaysChange)
            }

            OutlinedTextField(
                value = label,
                onValueChange = onLabelChange,
                label = { Text(stringResource(R.string.label)) },
                placeholder = { Text(stringResource(R.string.default_alarm_label)) },
                modifier = Modifier.fillMaxWidth().testTag("alarmLabelField"),
                singleLine = true,
            )

            HorizontalDivider()

            SettingSwitchRow(
                label = stringResource(R.string.custom_settings_for_alarm),
                checked = useCustomSettings,
                onCheckedChange = onUseCustomSettingsChange,
            )
            if (useCustomSettings) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    SettingSlider(
                        label = stringResource(R.string.snooze_time),
                        valueLabel = stringResource(R.string.duration_minutes, snoozeMinutes),
                        value = snoozeMinutes,
                        onValueChange = onSnoozeMinutesChange,
                        valueRange = 1..30,
                        step = 1,
                    )
                    SettingSlider(
                        label = stringResource(R.string.shakes_to_stop),
                        valueLabel = stringResource(R.string.shakes_count, numberOfShakes),
                        value = numberOfShakes,
                        onValueChange = onNumberOfShakesChange,
                        valueRange = 5..100,
                        step = 5,
                    )
                    VibrationSwitchRow(
                        vibrationEnabled = vibrationEnabled,
                        onVibrationEnabledChange = onVibrationEnabledChange,
                    )
                    GradualVolumeSwitchRow(
                        gradualVolume = gradualVolume,
                        onGradualVolumeChange = onGradualVolumeChange,
                    )
                    SoundPickerRow(soundUri = soundUri, onSoundUriChange = onSoundUriChange)
                }
            }

            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }
        }
    }

    if (showTimePicker) {
        AlarmTimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onConfirm = { h, m ->
                onTimeChange(h, m)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        text = { TimePicker(state = state) },
    )
}

@Composable
private fun DayOfWeekSelector(
    selected: Set<DayOfWeek>,
    onDaysChange: (Set<DayOfWeek>) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val days = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        days.forEach { day ->
            val isSelected = day in selected
            FilterChip(
                selected = isSelected,
                onClick = { onDaysChange(if (isSelected) selected - day else selected + day) },
                label = { Text(day.getDisplayName(TextStyle.NARROW, locale)) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = day.getDisplayName(TextStyle.FULL, locale)
                    },
            )
        }
    }
}
