package com.maxtasy.wakku.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.maxtasy.wakku.WakkuApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** AlarmManager alarms are cleared on reboot, so every enabled alarm needs rescheduling. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = (context.applicationContext as WakkuApplication).database.alarmDao()
                val scheduler = AlarmScheduler(context.applicationContext)
                dao.getAllEnabled().forEach { scheduler.schedule(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
