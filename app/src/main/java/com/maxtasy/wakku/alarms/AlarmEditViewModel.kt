package com.maxtasy.wakku.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxtasy.wakku.data.Alarm
import com.maxtasy.wakku.data.AlarmDao
import com.maxtasy.wakku.scheduling.AlarmScheduler
import com.maxtasy.wakku.settings.AppSettings
import com.maxtasy.wakku.settings.SettingsRepository
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
    // Whether the four fields below override the global Settings defaults
    // for this alarm. When false, they're only pre-filled for display and
    // save() writes null for all of them.
    val useCustomSettings: Boolean = false,
    val snoozeMinutes: Int = AppSettings.DEFAULT_SNOOZE_MINUTES,
    val numberOfShakes: Int = AppSettings.DEFAULT_NUMBER_OF_SHAKES,
    val vibrationEnabled: Boolean = true,
    val soundUri: String? = null,
)

class AlarmEditViewModel(
    private val alarmDao: AlarmDao,
    private val alarmScheduler: AlarmScheduler,
    private val settingsRepository: SettingsRepository,
    private val alarmId: Long?,
) : ViewModel() {

    private val _state = MutableStateFlow(AlarmEditState())
    val state: StateFlow<AlarmEditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val defaults = settingsRepository.current()
            val alarm = alarmId?.let { alarmDao.getById(it) }
            _state.value = AlarmEditState(
                hour = alarm?.hour ?: 7,
                minute = alarm?.minute ?: 0,
                repeatDays = alarm?.repeatDays ?: emptySet(),
                label = alarm?.label ?: "",
                useCustomSettings = alarm != null && (
                    alarm.snoozeMinutes != null || alarm.numberOfShakes != null ||
                        alarm.vibrationEnabled != null || alarm.soundUri != null
                    ),
                snoozeMinutes = alarm?.snoozeMinutes ?: defaults.snoozeMinutes,
                numberOfShakes = alarm?.numberOfShakes ?: defaults.numberOfShakes,
                vibrationEnabled = alarm?.vibrationEnabled ?: defaults.vibrationEnabled,
                soundUri = alarm?.soundUri ?: defaults.soundUri,
            )
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

    fun setUseCustomSettings(useCustom: Boolean) {
        _state.value = _state.value.copy(useCustomSettings = useCustom)
    }

    fun setSnoozeMinutes(minutes: Int) {
        _state.value = _state.value.copy(snoozeMinutes = minutes)
    }

    fun setNumberOfShakes(count: Int) {
        _state.value = _state.value.copy(numberOfShakes = count)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(vibrationEnabled = enabled)
    }

    fun setSoundUri(uri: String?) {
        _state.value = _state.value.copy(soundUri = uri)
    }

    fun save() {
        val current = _state.value
        viewModelScope.launch {
            val savedId = alarmDao.upsert(
                Alarm(
                    id = alarmId ?: 0,
                    hour = current.hour,
                    minute = current.minute,
                    repeatDays = current.repeatDays,
                    label = current.label,
                    // Saving always arms the alarm, even one that was off —
                    // editing an alarm is a strong signal the user wants it.
                    enabled = true,
                    snoozeMinutes = current.snoozeMinutes.takeIf { current.useCustomSettings },
                    numberOfShakes = current.numberOfShakes.takeIf { current.useCustomSettings },
                    vibrationEnabled = current.vibrationEnabled.takeIf { current.useCustomSettings },
                    soundUri = current.soundUri.takeIf { current.useCustomSettings },
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
