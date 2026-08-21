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

    @Query("SELECT * FROM patient_settings WHERE patientId NOT LIKE 'demo-person-%'")
    suspend fun getAllLiveSettings(): List<PatientSettingsEntity>

    @Query("DELETE FROM patient_settings")
    suspend fun deleteAll(): Int

    @Query("DELETE FROM patient_settings WHERE patientId = :patientId")
    suspend fun deleteByPatientId(patientId: String): Int

    @Query("DELETE FROM patient_settings WHERE patientId LIKE 'demo-person-%'")
    suspend fun deleteDemoSettings(): Int

    @Query("DELETE FROM patient_settings WHERE patientId NOT LIKE 'demo-person-%'")
    suspend fun deleteLiveSettings(): Int
}

