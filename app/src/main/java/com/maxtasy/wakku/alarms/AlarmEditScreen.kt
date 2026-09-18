package com.maxtasy.wakku.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxtasy.wakku.settings.SettingSlider
import com.maxtasy.wakku.settings.SoundPickerRow
import com.maxtasy.wakku.settings.VibrationSwitchRow
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

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
    soundUri: String?,
    onTimeChange: (Int, Int) -> Unit,
    onDaysChange: (Set<DayOfWeek>) -> Unit,
    onLabelChange: (String) -> Unit,
    onUseCustomSettingsChange: (Boolean) -> Unit,
    onSnoozeMinutesChange: (Int) -> Unit,
    onNumberOfShakesChange: (Int) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onSoundUriChange: (String?) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New alarm" else "Edit alarm") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete alarm")
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
            )
            TextButton(onClick = { showTimePicker = true }) {
                Text("Change time")
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Repeat", style = MaterialTheme.typography.labelLarge)
                DayOfWeekSelector(selected = repeatDays, onDaysChange = onDaysChange)
            }

            OutlinedTextField(
                value = label,
                onValueChange = onLabelChange,
                label = { Text("Label") },
                placeholder = { Text("Alarm") },
                modifier = Modifier.fillMaxWidth().testTag("alarmLabelField"),
                singleLine = true,
            )

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = useCustomSettings,
                        onValueChange = onUseCustomSettingsChange,
                        role = Role.Switch,
                    )
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Custom settings for this alarm", style = MaterialTheme.typography.titleMedium)
                Switch(checked = useCustomSettings, onCheckedChange = null)
            }
            if (useCustomSettings) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    SettingSlider(
                        label = "Snooze time",
                        valueLabel = "$snoozeMinutes min",
                        value = snoozeMinutes,
                        onValueChange = onSnoozeMinutesChange,
                        valueRange = 1..30,
                        step = 1,
                    )
                    SettingSlider(
                        label = "Shakes to stop",
                        valueLabel = "$numberOfShakes shakes",
                        value = numberOfShakes,
                        onValueChange = onNumberOfShakesChange,
                        valueRange = 5..100,
                        step = 5,
                    )
                    VibrationSwitchRow(
                        vibrationEnabled = vibrationEnabled,
                        onVibrationEnabledChange = onVibrationEnabledChange,
                    )
                    SoundPickerRow(soundUri = soundUri, onSoundUriChange = onSoundUriChange)
                }
            }

            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
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
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = { TimePicker(state = state) },
    )
}

@Composable
private fun DayOfWeekSelector(
    selected: Set<DayOfWeek>,
    onDaysChange: (Set<DayOfWeek>) -> Unit,
) {
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
                label = { Text(day.getDisplayName(TextStyle.NARROW, Locale.getDefault())) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    },
            )
        }
    }
}
