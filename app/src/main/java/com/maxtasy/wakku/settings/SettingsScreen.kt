package com.maxtasy.wakku.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            VibrationSwitchRow(
                vibrationEnabled = settings.vibrationEnabled,
                onVibrationEnabledChange = onVibrationEnabledChange,
            )
            SoundPickerRow(soundUri = settings.soundUri, onSoundUriChange = onSoundUriChange)
        }
    }
}
