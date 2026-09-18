package com.maxtasy.wakku.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
        val NUMBER_OF_SHAKES = intPreferencesKey("number_of_shakes")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SOUND_URI = stringPreferencesKey("sound_uri")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            snoozeMinutes = prefs[Keys.SNOOZE_MINUTES] ?: AppSettings.DEFAULT_SNOOZE_MINUTES,
            numberOfShakes = prefs[Keys.NUMBER_OF_SHAKES] ?: AppSettings.DEFAULT_NUMBER_OF_SHAKES,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            soundUri = prefs[Keys.SOUND_URI],
        )
    }

    /** One-shot read for callers that aren't already collecting [settings], e.g. RingingService. */
    suspend fun current(): AppSettings = settings.first()

    suspend fun setSnoozeMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[Keys.SNOOZE_MINUTES] = minutes }
    }

    suspend fun setNumberOfShakes(count: Int) {
        context.settingsDataStore.edit { it[Keys.NUMBER_OF_SHAKES] = count }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setSoundUri(uri: String?) {
        context.settingsDataStore.edit { prefs ->
            if (uri == null) prefs.remove(Keys.SOUND_URI) else prefs[Keys.SOUND_URI] = uri
        }
    }
}
