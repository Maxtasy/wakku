package com.maxtasy.wakku.ringing

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The id of the alarm currently ringing, or null. RingingActivity watches this
 * so it can close itself if the notification's Stop/Snooze action (rather than
 * the on-screen buttons) is what ended the ringing.
 */
object RingingController {
    val ringingAlarmId = MutableStateFlow<Long?>(null)
}
