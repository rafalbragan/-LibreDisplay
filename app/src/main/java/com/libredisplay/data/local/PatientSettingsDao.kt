package com.libredisplay.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PatientSettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: PatientSettingsEntity)

    @Query("SELECT * FROM patient_settings WHERE patientId = :patientId LIMIT 1")
    suspend fun getByPatientId(patientId: String): PatientSettingsEntity?
}

