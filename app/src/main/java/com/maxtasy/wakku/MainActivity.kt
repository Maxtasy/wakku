package com.maxtasy.wakku

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.maxtasy.wakku.alarms.AlarmEditScreen
import com.maxtasy.wakku.alarms.AlarmEditViewModel
import com.maxtasy.wakku.alarms.AlarmListScreen
import com.maxtasy.wakku.alarms.AlarmListViewModel
import com.maxtasy.wakku.data.AlarmDao
import com.maxtasy.wakku.scheduling.AlarmScheduler
import com.maxtasy.wakku.settings.SettingsRepository
import com.maxtasy.wakku.settings.SettingsScreen
import com.maxtasy.wakku.settings.SettingsViewModel
import com.maxtasy.wakku.ui.theme.WakkuTheme

private const val NEW_ALARM_ID = -1L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WakkuTheme {
                WakkuApp(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private val Context.alarmDao: AlarmDao
    get() = (applicationContext as WakkuApplication).database.alarmDao()

private val Context.settingsRepository: SettingsRepository
    get() = (applicationContext as WakkuApplication).settings

private fun Context.isIgnoringBatteryOptimizations(): Boolean =
    getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

@Composable
fun WakkuApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val alarmDao = context.alarmDao
    val alarmScheduler = remember(context) { AlarmScheduler(context.applicationContext) }
    val settingsRepository = context.settingsRepository

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Ringing still shows full-screen either way; only the notification itself is affected. */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // MIUI/Xiaomi/Samsung-style OEMs aggressively kill background apps; without this
    // exemption the ringing foreground service can be stopped before an alarm fires.
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(context.isIgnoringBatteryOptimizations()) }
    var batteryWarningDismissed by remember { mutableStateOf(false) }
    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations() }

    NavHost(navController = navController, startDestination = "alarms", modifier = modifier) {
        composable("alarms") {
            val viewModel: AlarmListViewModel = viewModel(
                factory = viewModelFactory { initializer { AlarmListViewModel(alarmDao, alarmScheduler) } },
            )
            val alarms by viewModel.alarms.collectAsStateWithLifecycle()
            AlarmListScreen(
                alarms = alarms,
                onToggle = viewModel::setEnabled,
                onOpenAlarm = { id -> navController.navigate("alarm/${id ?: NEW_ALARM_ID}") },
                onOpenSettings = { navController.navigate("settings") },
                showBatteryOptimizationWarning = !isIgnoringBatteryOptimizations && !batteryWarningDismissed,
                onFixBatteryOptimization = {
                    batteryOptimizationLauncher.launch(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                },
                onDismissBatteryOptimizationWarning = { batteryWarningDismissed = true },
            )
        }
        composable("settings") {
            val viewModel: SettingsViewModel = viewModel(
                factory = viewModelFactory { initializer { SettingsViewModel(settingsRepository) } },
            )
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            SettingsScreen(
                settings = settings,
                onSnoozeMinutesChange = viewModel::setSnoozeMinutes,
                onNumberOfShakesChange = viewModel::setNumberOfShakes,
                onVibrationEnabledChange = viewModel::setVibrationEnabled,
                onSoundUriChange = viewModel::setSoundUri,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "alarm/{alarmId}",
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: NEW_ALARM_ID
            val editingId = alarmId.takeIf { it != NEW_ALARM_ID }
            val viewModel: AlarmEditViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { AlarmEditViewModel(alarmDao, alarmScheduler, settingsRepository, editingId) }
                },
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            AlarmEditScreen(
                hour = state.hour,
                minute = state.minute,
                repeatDays = state.repeatDays,
                label = state.label,
                isNew = editingId == null,
                useCustomSettings = state.useCustomSettings,
                snoozeMinutes = state.snoozeMinutes,
                numberOfShakes = state.numberOfShakes,
                vibrationEnabled = state.vibrationEnabled,
                soundUri = state.soundUri,
                onTimeChange = viewModel::setTime,
                onDaysChange = viewModel::setDays,
                onLabelChange = viewModel::setLabel,
                onUseCustomSettingsChange = viewModel::setUseCustomSettings,
                onSnoozeMinutesChange = viewModel::setSnoozeMinutes,
                onNumberOfShakesChange = viewModel::setNumberOfShakes,
                onVibrationEnabledChange = viewModel::setVibrationEnabled,
                onSoundUriChange = viewModel::setSoundUri,
                onSave = {
                    viewModel.save()
                    navController.popBackStack()
                },
                onDelete = {
                    viewModel.delete()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
