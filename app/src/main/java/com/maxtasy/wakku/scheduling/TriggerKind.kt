package com.maxtasy.wakku.scheduling

/**
 * Why an alarm's pending trigger time is what it is. Recorded next to the
 * time by [NextAlarmNotifier], because [SNOOZED] and [SKIPPED] times can't be
 * recomputed from the alarm itself and must survive a reboot or app restart.
 */
enum class TriggerKind {
    /** The alarm's regular next occurrence. */
    NORMAL,

    /** "Snooze N minutes from when it rang". */
    SNOOZED,

    /** The regular next occurrence was skipped; this is the one after it. */
    SKIPPED,
}
