package com.libredisplay.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncRunDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(run: SyncRunEntity): Long

    @Query("SELECT * FROM sync_runs ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLatest(): SyncRunEntity?

    @Query("DELETE FROM sync_runs")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM sync_runs")
    suspend fun countAllRuns(): Long

    @Query("SELECT COUNT(*) FROM sync_runs WHERE status = 'SUCCESS'")
    suspend fun countSuccessfulRuns(): Long

    @Query("SELECT COUNT(*) FROM sync_runs WHERE status = 'FAILED'")
    suspend fun countFailedRuns(): Long

    @Query("SELECT finishedAt FROM sync_runs WHERE status = 'SUCCESS' AND finishedAt IS NOT NULL ORDER BY finishedAt DESC LIMIT 1")
    suspend fun latestSuccessfulFinishedAt(): java.time.Instant?
}

