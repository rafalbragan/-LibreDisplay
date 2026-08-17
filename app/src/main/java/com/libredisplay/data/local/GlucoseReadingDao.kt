package com.libredisplay.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.Instant

@Dao
interface GlucoseReadingDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(readings: List<GlucoseReadingEntity>): List<Long>

    @Query("SELECT * FROM glucose_readings WHERE patientId = :patientId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestByPatient(patientId: String): GlucoseReadingEntity?

    @Query(
        """
        SELECT * FROM glucose_readings
        WHERE patientId = :patientId
          AND timestamp >= :fromInclusive
          AND timestamp <= :toInclusive
        ORDER BY timestamp ASC
        """
    )
    suspend fun getRangeByPatient(
        patientId: String,
        fromInclusive: Instant,
        toInclusive: Instant
    ): List<GlucoseReadingEntity>

    @Query("DELETE FROM glucose_readings WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Instant): Int

    @Query("DELETE FROM glucose_readings")
    suspend fun deleteAllReadings(): Int

    @Query("DELETE FROM glucose_readings WHERE patientId = :patientId")
    suspend fun deleteReadingsForPerson(patientId: String): Int

    @Query("DELETE FROM glucose_readings WHERE source = 'DemoMode' OR patientId LIKE 'demo-person-%'")
    suspend fun deleteDemoReadings(): Int
}

