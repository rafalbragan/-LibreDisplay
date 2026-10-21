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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(readings: List<GlucoseReadingEntity>)

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

    @Query("DELETE FROM glucose_readings WHERE timestamp < :cutoff AND source NOT IN ('LibreViewCSV', 'LibreViewScan')")
    suspend fun deleteOlderThan(cutoff: Instant): Int

    @Query("DELETE FROM glucose_readings")
    suspend fun deleteAllReadings(): Int

    @Query("DELETE FROM glucose_readings WHERE patientId = :patientId")
    suspend fun deleteReadingsForPerson(patientId: String): Int

    @Query("DELETE FROM glucose_readings WHERE source = 'DemoMode' OR patientId LIKE 'demo-person-%'")
    suspend fun deleteDemoReadings(): Int

    @Query("SELECT * FROM glucose_readings WHERE source != 'DemoMode' AND patientId NOT LIKE 'demo-person-%' ORDER BY timestamp ASC")
    suspend fun getAllLiveReadings(): List<GlucoseReadingEntity>

    @Query("DELETE FROM glucose_readings WHERE source != 'DemoMode' AND patientId NOT LIKE 'demo-person-%'")
    suspend fun deleteAllLiveReadings(): Int

    @Query("SELECT COUNT(*) FROM glucose_readings")
    suspend fun countAllReadings(): Long

    @Query("SELECT COUNT(*) FROM glucose_readings WHERE source != 'DemoMode' AND patientId NOT LIKE 'demo-person-%'")
    suspend fun countLiveReadings(): Long

    @Query("SELECT MIN(timestamp) FROM glucose_readings WHERE source != 'DemoMode' AND patientId NOT LIKE 'demo-person-%'")
    suspend fun oldestLiveReadingTimestamp(): Instant?

    @Query("SELECT MAX(timestamp) FROM glucose_readings WHERE source != 'DemoMode' AND patientId NOT LIKE 'demo-person-%'")
    suspend fun newestLiveReadingTimestamp(): Instant?

    @Query("SELECT MIN(timestamp) FROM glucose_readings WHERE patientId = :patientId")
    suspend fun oldestReadingTimestampForPatient(patientId: String): Instant?

    @Query("SELECT MAX(timestamp) FROM glucose_readings WHERE patientId = :patientId")
    suspend fun newestReadingTimestampForPatient(patientId: String): Instant?

    @Query("SELECT COUNT(*) FROM glucose_readings WHERE source != 'DemoMode' AND patientId NOT LIKE 'demo-person-%' AND timestamp >= :fromInclusive")
    suspend fun countLiveReadingsFrom(fromInclusive: Instant): Long

    @Query("SELECT COUNT(*) FROM glucose_readings WHERE source != 'DemoMode' AND patientId NOT LIKE 'demo-person-%' AND timestamp < :cutoff")
    suspend fun countLiveReadingsOlderThan(cutoff: Instant): Long

    @Query(
        """
        SELECT
            g.patientId AS patientId,
            COALESCE(MAX(o.displayName), g.patientId) AS displayName,
            MIN(g.timestamp) AS firstTimestamp,
            MAX(g.timestamp) AS lastTimestamp,
            SUM(CASE WHEN g.timestamp >= :from14d THEN 1 ELSE 0 END) AS readings14d,
            SUM(CASE WHEN g.timestamp >= :from30d THEN 1 ELSE 0 END) AS readings30d,
            SUM(CASE WHEN g.timestamp >= :from60d THEN 1 ELSE 0 END) AS readings60d,
            SUM(CASE WHEN g.timestamp >= :from90d THEN 1 ELSE 0 END) AS readings90d,
            SUM(CASE WHEN g.timestamp >= :from360d THEN 1 ELSE 0 END) AS readings360d
        FROM glucose_readings g
        LEFT JOIN observed_persons o ON o.patientId = g.patientId
        WHERE g.source != 'DemoMode'
          AND g.patientId NOT LIKE 'demo-person-%'
        GROUP BY g.patientId
        HAVING COUNT(*) > 0
        ORDER BY displayName COLLATE NOCASE ASC
        """
    )
    suspend fun loadPersonCoverageRows(
        from14d: Instant,
        from30d: Instant,
        from60d: Instant,
        from90d: Instant,
        from360d: Instant
    ): List<PersonCoverageRow>

    @Query("SELECT * FROM glucose_readings WHERE patientId = :patientId AND timestamp IN (:timestamps)")
    suspend fun atTimestamps(patientId: String, timestamps: List<Instant>): List<GlucoseReadingEntity>

    @Query("""
        SELECT MIN(timestamp) AS firstTime, MAX(timestamp) AS lastTime,
               MIN(valueMgDl) AS minimum, MAX(valueMgDl) AS maximum, AVG(valueMgDl) AS average,
               COUNT(*) AS count, SUM(CASE WHEN source = 'LibreViewScan' THEN 1 ELSE 0 END) AS scans
        FROM glucose_readings WHERE patientId = :patientId AND isValid = 1
          AND timestamp >= :start AND timestamp <= :end
        GROUP BY (timestamp - :start) / :bucketMillis ORDER BY firstTime LIMIT 801
    """)
    suspend fun chartBuckets(patientId: String, start: Long, end: Long, bucketMillis: Long): List<HistoryBucket>

    @Query("""
        SELECT * FROM glucose_readings WHERE patientId = :patientId AND isValid = 1
        AND timestamp >= :start AND timestamp <= :end
        ORDER BY ABS(timestamp - :at) ASC, timestamp ASC LIMIT 1
    """)
    suspend fun nearestInViewport(patientId: String, start: Long, end: Long, at: Long): GlucoseReadingEntity?
}

data class HistoryBucket(val firstTime: Long, val lastTime: Long, val minimum: Int,
    val maximum: Int, val average: Double, val count: Int, val scans: Int)

