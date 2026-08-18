package com.libredisplay.data.repository

import com.libredisplay.data.local.GlucoseReadingDao
import com.libredisplay.data.local.GlucoseReadingEntity
import com.libredisplay.data.local.ObservedPersonDao
import com.libredisplay.data.local.ObservedPersonEntity
import com.libredisplay.data.local.SyncRunDao
import com.libredisplay.data.local.SyncRunEntity
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.data.model.LibreConnectionPerson
import com.libredisplay.data.model.MonitoringSnapshot
import com.libredisplay.diagnostics.DiagnosticLogger
import java.time.Instant
import java.time.temporal.ChronoUnit

class LocalGlucoseHistoryRepository(
    private val observedPersonDao: ObservedPersonDao,
    private val glucoseReadingDao: GlucoseReadingDao,
    private val syncRunDao: SyncRunDao
) {

    suspend fun upsertObservedPersons(persons: List<LibreConnectionPerson>, now: Instant) {
        val entities = persons.map { person ->
            ObservedPersonEntity(
                patientId = person.patientId,
                firstName = person.firstName,
                lastName = person.lastName,
                displayName = person.displayName,
                isActive = true,
                lastSeenAt = now,
                createdAt = now,
                updatedAt = now
            )
        }
        observedPersonDao.upsertAll(entities)
        if (persons.isNotEmpty()) {
            observedPersonDao.markInactiveExcept(persons.map { it.patientId }, now)
        }
    }

    suspend fun insertReadings(
        patientId: String,
        points: List<GlucoseHistoryPoint>,
        source: String = "LibreLinkUp",
        sourceAccountId: String? = null,
        now: Instant = Instant.now()
    ): InsertSummary {
        if (points.isEmpty()) return InsertSummary(inserted = 0, duplicates = 0)
        val entities = points.map { point ->
            val epochMillis = point.timestamp.toEpochMilli()
            GlucoseReadingEntity(
                id = "$patientId:$epochMillis",
                patientId = patientId,
                timestamp = point.timestamp,
                valueMgDl = point.value,
                trendArrow = point.trend.arrow,
                trendLabel = point.trend.description,
                source = source,
                sourceAccountId = sourceAccountId,
                receivedAt = now,
                isValid = true,
                rawTrendCode = null,
                createdAt = now
            )
        }
        val insertResults = glucoseReadingDao.insertIgnore(entities)
        val inserted = insertResults.count { it != -1L }
        val duplicates = insertResults.size - inserted
        return InsertSummary(inserted = inserted, duplicates = duplicates)
    }

    suspend fun deleteReadingsOlderThan(days: Long, now: Instant = Instant.now()): Int {
        val cutoff = now.minus(days, ChronoUnit.DAYS)
        return glucoseReadingDao.deleteOlderThan(cutoff)
    }

    suspend fun deleteReadingsOlderThanHours(hours: Long, now: Instant = Instant.now()): Int {
        val cutoff = now.minus(hours, ChronoUnit.HOURS)
        return glucoseReadingDao.deleteOlderThan(cutoff)
    }

    suspend fun deleteLocalGlucoseHistory(): Int = glucoseReadingDao.deleteAllReadings()

    suspend fun deleteReadingsForPerson(patientId: String): Int = glucoseReadingDao.deleteReadingsForPerson(patientId)

    suspend fun deleteObservedPeople(): Int = observedPersonDao.deleteAllPeople()

    suspend fun deleteDemoData(): Int {
        val deletedReadings = glucoseReadingDao.deleteDemoReadings()
        val deletedPeople = observedPersonDao.deleteDemoPeople()
        return deletedReadings + deletedPeople
    }

    suspend fun deleteAllSyncRuns(): Int = syncRunDao.deleteAll()

    suspend fun saveSyncRun(syncRun: SyncRunEntity): Long {
        return syncRunDao.insert(syncRun)
    }

    suspend fun loadLatestMonitoringSnapshot(patientId: String?): MonitoringSnapshot? {
        val persons = observedPersonDao.getActivePersons().map {
            LibreConnectionPerson(
                patientId = it.patientId,
                displayName = it.displayName,
                firstName = it.firstName,
                lastName = it.lastName
            )
        }
        if (persons.isEmpty()) return null

        val selected = patientId?.let { requested ->
            persons.firstOrNull { it.patientId == requested }
        } ?: persons.first()

        val latest = glucoseReadingDao.getLatestByPatient(selected.patientId) ?: return null
        val historyStart = latest.timestamp.minus(12, ChronoUnit.HOURS)
        val historyEntities = glucoseReadingDao.getRangeByPatient(
            patientId = selected.patientId,
            fromInclusive = historyStart,
            toInclusive = latest.timestamp
        )
        val history = historyEntities.map { it.toHistoryPoint() }
        DiagnosticLogger.logInfo(
            "LocalGlucoseHistoryRepository",
            "LOCAL HISTORY loaded patientIdPrefix=${selected.patientId.take(6)} from=$historyStart to=${latest.timestamp} count=${history.size}"
        )
        val reading = GlucoseReading.of(
            value = latest.valueMgDl,
            timestamp = latest.timestamp,
            trend = trendFromLabel(arrow = latest.trendArrow, label = latest.trendLabel),
            history = history
        )

        return MonitoringSnapshot(
            persons = persons,
            selectedPerson = selected,
            reading = reading
        )
    }

    suspend fun loadHistory(patientId: String, fromInclusive: Instant, toInclusive: Instant): List<GlucoseHistoryPoint> {
        return glucoseReadingDao.getRangeByPatient(patientId, fromInclusive, toInclusive)
            .map { it.toHistoryPoint() }
    }

    private fun GlucoseReadingEntity.toHistoryPoint(): GlucoseHistoryPoint {
        return GlucoseHistoryPoint(
            value = valueMgDl,
            timestamp = timestamp,
            trend = trendFromLabel(arrow = trendArrow, label = trendLabel)
        )
    }

    private fun trendFromLabel(arrow: String?, label: String?): GlucoseTrend {
        return when {
            arrow == "↑" -> GlucoseTrend.RISING_FAST
            arrow == "↗" -> GlucoseTrend.RISING
            arrow == "→" -> GlucoseTrend.FLAT
            arrow == "↘" -> GlucoseTrend.FALLING
            arrow == "↓" -> GlucoseTrend.FALLING_FAST
            label?.contains("Szybko ro", ignoreCase = true) == true -> GlucoseTrend.RISING_FAST
            label?.contains("Rosnie", ignoreCase = true) == true -> GlucoseTrend.RISING
            label?.contains("Stabil", ignoreCase = true) == true -> GlucoseTrend.FLAT
            label?.contains("Spada", ignoreCase = true) == true -> GlucoseTrend.FALLING
            else -> GlucoseTrend.UNKNOWN
        }
    }
}

data class InsertSummary(
    val inserted: Int,
    val duplicates: Int
)

