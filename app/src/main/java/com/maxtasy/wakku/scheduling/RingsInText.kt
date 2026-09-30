package com.maxtasy.wakku.scheduling

import android.content.Context
import com.maxtasy.wakku.R

/** "Rings in 7 h 32 min" — shown after saving or switching on an alarm. Zero-valued parts are left out. */
fun Context.ringsInText(triggerAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val (days, hours, minutes) = AlarmTiming.durationUntil(triggerAtMillis, nowMillis)
    val parts = buildList {
        if (days > 0) add(getString(R.string.duration_days, days))
        if (hours > 0) add(getString(R.string.duration_hours, hours))
        if (minutes > 0) add(getString(R.string.duration_minutes, minutes))
    }
    return getString(R.string.rings_in, parts.joinToString(" "))
}
