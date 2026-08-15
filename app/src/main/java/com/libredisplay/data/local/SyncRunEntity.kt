package com.libredisplay.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "sync_runs")
data class SyncRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val status: String,
    val reason: String,
    val personsCount: Int,
    val readingsInserted: Int,
    val readingsSkippedDuplicate: Int,
    val errorMessage: String?,
    val httpStatus: Int?,
    val retryAfterSeconds: Int?
)

