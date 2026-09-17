package com.maxtasy.wakku.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.maxtasy.wakku.WakkuApplication
import com.maxtasy.wakku.ringing.RingingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires when a scheduled alarm's trigger time arrives. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = (context.applicationContext as WakkuApplication).database.alarmDao()
                val alarm = dao.getById(alarmId) ?: return@launch

                ContextCompat.startForegroundService(context, RingingService.intent(context, alarm))

                val scheduler = AlarmScheduler(context.applicationContext)
                if (alarm.repeatDays.isEmpty()) {
                    dao.setEnabled(alarm.id, false)
                    // WakkuApplication's cold-start reschedule can race this and
                    // schedule a stray next-day occurrence; cancel it explicitly.
                    scheduler.cancel(alarm)
                } else {
                    scheduler.schedule(alarm)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
}
