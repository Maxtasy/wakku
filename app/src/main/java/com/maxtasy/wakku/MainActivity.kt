package com.maxtasy.wakku

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

@Composable
fun WakkuApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val alarmDao = context.alarmDao
    val alarmScheduler = remember(context) { AlarmScheduler(context.applicationContext) }

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
            )
        }
        composable(
            route = "alarm/{alarmId}",
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: NEW_ALARM_ID
            val editingId = alarmId.takeIf { it != NEW_ALARM_ID }
            val viewModel: AlarmEditViewModel = viewModel(
                factory = viewModelFactory { initializer { AlarmEditViewModel(alarmDao, alarmScheduler, editingId) } },
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            AlarmEditScreen(
                hour = state.hour,
                minute = state.minute,
                repeatDays = state.repeatDays,
                label = state.label,
                isNew = editingId == null,
                onTimeChange = viewModel::setTime,
                onDaysChange = viewModel::setDays,
                onLabelChange = viewModel::setLabel,
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
