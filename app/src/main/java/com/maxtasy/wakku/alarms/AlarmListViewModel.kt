package com.maxtasy.wakku.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxtasy.wakku.data.Alarm
import com.maxtasy.wakku.data.AlarmDao
import com.maxtasy.wakku.scheduling.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmListViewModel(
    private val alarmDao: AlarmDao,
    private val alarmScheduler: AlarmScheduler,
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = alarmDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setEnabled(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch {
            alarmDao.setEnabled(alarm.id, enabled)
            val updated = alarm.copy(enabled = enabled)
            if (enabled) alarmScheduler.schedule(updated) else alarmScheduler.cancel(updated)
        }
    }
}
