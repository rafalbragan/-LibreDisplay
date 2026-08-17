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
}

