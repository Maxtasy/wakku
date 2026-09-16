package com.maxtasy.wakku.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxtasy.wakku.data.Alarm
import com.maxtasy.wakku.data.AlarmDao
import com.maxtasy.wakku.scheduling.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class AlarmEditState(
    val hour: Int = 7,
    val minute: Int = 0,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val label: String = "",
)

class AlarmEditViewModel(
    private val alarmDao: AlarmDao,
    private val alarmScheduler: AlarmScheduler,
    private val alarmId: Long?,
) : ViewModel() {

    private val _state = MutableStateFlow(AlarmEditState())
    val state: StateFlow<AlarmEditState> = _state.asStateFlow()

    init {
        if (alarmId != null) {
            viewModelScope.launch {
                alarmDao.getById(alarmId)?.let { alarm ->
                    _state.value = AlarmEditState(
                        hour = alarm.hour,
                        minute = alarm.minute,
                        repeatDays = alarm.repeatDays,
                        label = alarm.label,
                    )
                }
            }
        }
    }

    fun setTime(hour: Int, minute: Int) {
        _state.value = _state.value.copy(hour = hour, minute = minute)
    }

    fun setDays(days: Set<DayOfWeek>) {
        _state.value = _state.value.copy(repeatDays = days)
    }

    fun setLabel(label: String) {
        _state.value = _state.value.copy(label = label)
    }

    fun save() {
        val current = _state.value
        viewModelScope.launch {
            val existing = alarmId?.let { alarmDao.getById(it) }
            val savedId = alarmDao.upsert(
                Alarm(
                    id = alarmId ?: 0,
                    hour = current.hour,
                    minute = current.minute,
                    repeatDays = current.repeatDays,
                    label = current.label,
                    enabled = existing?.enabled ?: true,
                )
            )
            alarmDao.getById(savedId)?.let { alarmScheduler.schedule(it) }
        }
    }

    fun delete() {
        val id = alarmId ?: return
        viewModelScope.launch {
            alarmDao.getById(id)?.let { alarm ->
                alarmDao.delete(alarm)
                alarmScheduler.cancel(alarm)
            }
        }
    }
}
