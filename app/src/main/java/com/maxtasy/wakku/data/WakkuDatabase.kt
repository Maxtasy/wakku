package com.maxtasy.wakku.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Alarm::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class WakkuDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile private var instance: WakkuDatabase? = null

        fun getInstance(context: Context): WakkuDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WakkuDatabase::class.java,
                    "wakku.db",
                )
                    // No real user data to preserve yet; destructive is fine.
                    .fallbackToDestructiveMigration(true)
                    .build().also { instance = it }
            }
    }
}
