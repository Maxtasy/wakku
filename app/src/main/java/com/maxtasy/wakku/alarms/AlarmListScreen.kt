package com.maxtasy.wakku.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maxtasy.wakku.data.Alarm
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    onToggle: (Alarm, Boolean) -> Unit,
    onOpenAlarm: (Long?) -> Unit,
    onOpenSettings: () -> Unit,
    showBatteryOptimizationWarning: Boolean = false,
    onFixBatteryOptimization: () -> Unit = {},
    onDismissBatteryOptimizationWarning: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wakku") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpenAlarm(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Add alarm")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (showBatteryOptimizationWarning) {
                BatteryOptimizationBanner(
                    onFix = onFixBatteryOptimization,
                    onDismiss = onDismissBatteryOptimizationWarning,
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 0.dp),
                )
            }
            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No alarms yet.", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            onToggle = { onToggle(alarm, it) },
                            onClick = { onOpenAlarm(alarm.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryOptimizationBanner(onFix: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Alarms may be unreliable", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "This device can kill Wakku in the background. Exempt it from battery " +
                        "optimization to keep alarms reliable.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onFix,
                    modifier = Modifier.align(Alignment.Start),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                ) {
                    Text("Fix it")
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss")
            }
        }
    }
}

@Composable
private fun AlarmRow(alarm: Alarm, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "%02d:%02d".format(alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = alarm.repeatDays.toDisplayString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (alarm.label.isNotBlank()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Switch(
                checked = alarm.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.semantics {
                    contentDescription = "Alarm at %02d:%02d".format(alarm.hour, alarm.minute)
                },
            )
        }
    }
}

private val WEEKDAYS = setOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
)
private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

private fun Set<DayOfWeek>.toDisplayString(): String = when {
    isEmpty() -> "Once"
    size == 7 -> "Every day"
    this == WEEKDAYS -> "Weekdays"
    this == WEEKEND -> "Weekends"
    else -> sortedBy { it.value }.joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}
