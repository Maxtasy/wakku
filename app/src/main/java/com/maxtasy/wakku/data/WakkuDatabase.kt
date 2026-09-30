package com.maxtasy.wakku.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Alarm::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class WakkuDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile private var instance: WakkuDatabase? = null

        // Adds the per-alarm gradual-volume override (null = use the global setting).
        // A real migration rather than the destructive fallback, so existing
        // alarms survive the update.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN gradualVolume INTEGER")
            }
        }

        fun getInstance(context: Context): WakkuDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WakkuDatabase::class.java,
                    "wakku.db",
                )
                    .addMigrations(MIGRATION_2_3)
                    // Only reached for schema versions older than any shipped build.
                    .fallbackToDestructiveMigration(true)
                    .build().also { instance = it }
            }
    }
}
