package com.maxtasy.wakku

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.maxtasy.wakku.data.WakkuDatabase
import com.maxtasy.wakku.ringing.RingingService
import com.maxtasy.wakku.scheduling.AlarmScheduler
import com.maxtasy.wakku.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WakkuApplication : Application() {
    val database: WakkuDatabase by lazy { WakkuDatabase.getInstance(this) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        createAlarmNotificationChannel()
        // Covers app updates and force-stops, which don't go through BootReceiver.
        CoroutineScope(Dispatchers.IO).launch {
            val scheduler = AlarmScheduler(applicationContext)
            database.alarmDao().getAllEnabled().forEach { scheduler.schedule(it) }
        }
    }

    private fun createAlarmNotificationChannel() {
        val channel = NotificationChannel(
            RingingService.CHANNEL_ID,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Shown while an alarm is ringing"
            // RingingService loops its own sound/vibration continuously; a
            // one-shot channel sound or vibration would just double up on it.
            setSound(null, null)
            enableVibration(false)
            setBypassDnd(true)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
