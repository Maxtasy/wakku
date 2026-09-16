package com.maxtasy.wakku.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.maxtasy.wakku.WakkuApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fires when a scheduled alarm's trigger time arrives. Milestone 3 replaces
 * the toast below with the actual full-screen ringing activity + foreground
 * service; for now this just proves alarms survive an app kill or a reboot.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = (context.applicationContext as WakkuApplication).database.alarmDao()
                val alarm = dao.getById(alarmId) ?: return@launch

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Wakku: alarm %02d:%02d fired".format(alarm.hour, alarm.minute),
                        Toast.LENGTH_LONG,
                    ).show()
                }

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
