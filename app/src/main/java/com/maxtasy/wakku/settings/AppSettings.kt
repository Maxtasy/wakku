package com.maxtasy.wakku.settings

data class AppSettings(
    val snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    val numberOfShakes: Int = DEFAULT_NUMBER_OF_SHAKES,
    val vibrationEnabled: Boolean = true,
    /** null means "use the device's default alarm sound." */
    val soundUri: String? = null,
) {
    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 5
        const val DEFAULT_NUMBER_OF_SHAKES = 30
    }
}
