package com.libredisplay.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "observed_persons")
data class ObservedPersonEntity(
    @PrimaryKey val patientId: String,
    val firstName: String?,
    val lastName: String?,
    val displayName: String,
    val connectionId: String? = null,
    val isActive: Boolean = true,
    val lastSeenAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant
)

