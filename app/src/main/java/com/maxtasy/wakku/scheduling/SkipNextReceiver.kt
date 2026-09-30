package com.maxtasy.wakku.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.maxtasy.wakku.WakkuApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles the "Skip next" action on the "Next alarm" notification. */
class SkipNextReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = (context.applicationContext as WakkuApplication).database.alarmDao()
                val alarm = dao.getById(alarmId) ?: return@launch
                // scheduleAt inside skipNext refreshes the notification too.
                AlarmScheduler(context.applicationContext).skipNext(alarm)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
