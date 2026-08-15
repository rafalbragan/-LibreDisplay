package com.libredisplay.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "patient_settings")
data class PatientSettingsEntity(
    @PrimaryKey val patientId: String,
    val lowCriticalMgDl: Int = 54,
    val lowMgDl: Int = 70,
    val targetLowMgDl: Int = 70,
    val targetHighMgDl: Int = 180,
    val highMgDl: Int = 250,
    val hba1cTargetPercent: Double = 7.5,
    val labHba1cPercent: Double? = null,
    val labHba1cDate: LocalDate? = null,
    val updatedAt: Instant
)

