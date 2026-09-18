package com.maxtasy.wakku.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { repository.setSnoozeMinutes(minutes) }
    }

    fun setNumberOfShakes(count: Int) {
        viewModelScope.launch { repository.setNumberOfShakes(count) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setVibrationEnabled(enabled) }
    }

    fun setSoundUri(uri: String?) {
        viewModelScope.launch { repository.setSoundUri(uri) }
    }
}
