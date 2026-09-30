package com.maxtasy.wakku

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.maxtasy.wakku.data.WakkuDatabase
import com.maxtasy.wakku.ringing.RingingService
import com.maxtasy.wakku.scheduling.AlarmScheduler
import com.maxtasy.wakku.scheduling.NextAlarmNotifier
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
        NextAlarmNotifier.createChannel(this)
        // Covers app updates and force-stops, which don't go through BootReceiver.
        // restore() rather than schedule(), so a pending snooze or skip survives.
        CoroutineScope(Dispatchers.IO).launch {
            val scheduler = AlarmScheduler(applicationContext)
            database.alarmDao().getAllEnabled().forEach { scheduler.restore(it) }
        }
    }

    private fun createAlarmNotificationChannel() {
        val channel = NotificationChannel(
            RingingService.CHANNEL_ID,
            getString(R.string.channel_alarms_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.channel_alarms_description)
            // RingingService loops its own sound/vibration continuously; a
            // one-shot channel sound or vibration would just double up on it.
            setSound(null, null)
            enableVibration(false)
            setBypassDnd(true)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
