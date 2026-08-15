package com.libredisplay.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ObservedPersonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(persons: List<ObservedPersonEntity>)

    @Query("SELECT * FROM observed_persons WHERE isActive = 1 ORDER BY displayName ASC")
    suspend fun getActivePersons(): List<ObservedPersonEntity>

    @Query("SELECT * FROM observed_persons WHERE patientId = :patientId LIMIT 1")
    suspend fun getByPatientId(patientId: String): ObservedPersonEntity?

    @Query("UPDATE observed_persons SET isActive = 0, updatedAt = :updatedAt WHERE patientId NOT IN (:activePatientIds)")
    suspend fun markInactiveExcept(activePatientIds: List<String>, updatedAt: java.time.Instant)
}

