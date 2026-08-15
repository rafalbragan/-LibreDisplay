package com.libredisplay.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ObservedPersonEntity::class,
        GlucoseReadingEntity::class,
        SyncRunEntity::class,
        PatientSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class LibreDisplayDatabase : RoomDatabase() {
    abstract fun observedPersonDao(): ObservedPersonDao
    abstract fun glucoseReadingDao(): GlucoseReadingDao
    abstract fun syncRunDao(): SyncRunDao
    abstract fun patientSettingsDao(): PatientSettingsDao
}

