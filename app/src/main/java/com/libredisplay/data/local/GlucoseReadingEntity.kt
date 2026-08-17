package com.libredisplay.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "glucose_readings",
    indices = [
        Index(value = ["patientId", "timestamp"], unique = true),
        Index(value = ["patientId"]),
        Index(value = ["timestamp"])
    ]
)
data class GlucoseReadingEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val timestamp: Instant,
    val valueMgDl: Int,
    val trendArrow: String?,
    val trendLabel: String?,
    val source: String = "LibreLinkUp",
    val sourceAccountId: String? = null,
    val receivedAt: Instant,
    val isValid: Boolean = true,
    val rawTrendCode: String? = null,
    val createdAt: Instant
)

