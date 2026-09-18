package com.maxtasy.wakku.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val label: String = "",
    val enabled: Boolean = true,
    // null means "use the global default from Settings" for all four.
    val snoozeMinutes: Int? = null,
    val numberOfShakes: Int? = null,
    val vibrationEnabled: Boolean? = null,
    val soundUri: String? = null,
)
