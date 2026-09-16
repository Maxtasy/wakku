package com.maxtasy.wakku

import android.app.Application
import com.maxtasy.wakku.data.WakkuDatabase
import com.maxtasy.wakku.scheduling.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WakkuApplication : Application() {
    val database: WakkuDatabase by lazy { WakkuDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        // Covers app updates and force-stops, which don't go through BootReceiver.
        CoroutineScope(Dispatchers.IO).launch {
            val scheduler = AlarmScheduler(applicationContext)
            database.alarmDao().getAllEnabled().forEach { scheduler.schedule(it) }
        }
    }
}
