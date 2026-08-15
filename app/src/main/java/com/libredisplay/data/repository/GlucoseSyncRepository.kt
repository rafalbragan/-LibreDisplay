package com.libredisplay.data.repository

import com.libredisplay.data.api.LibreLinkUpHttpException
import com.libredisplay.data.api.RetrofitLibreLinkUpClient
import com.libredisplay.data.local.SyncRunEntity
import com.libredisplay.data.model.AppSettings
import com.libredisplay.diagnostics.DiagnosticLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

enum class SyncReason {
    APP_START,
    PERIODIC,
    MANUAL,
    PERSON_SWITCH,
    POLLING
}

enum class SyncStatus {
    SUCCESS,
    RATE_LIMITED,
    FAILED,
    SKIPPED
}

data class SyncResult(
    val status: SyncStatus,
    val personsCount: Int,
    val inserted: Int,
    val duplicates: Int,
    val errorMessage: String? = null,
    val httpStatus: Int? = null,
    val retryAfterSeconds: Int? = null
)

class GlucoseSyncRepository(
    private val settingsProvider: () -> AppSettings,
    private val authRepository: AuthRepository,
    private val productionClient: RetrofitLibreLinkUpClient,
    private val localRepository: LocalGlucoseHistoryRepository
) {

    private val syncMutex = Mutex()

    suspend fun syncAllPersons(reason: SyncReason): SyncResult {
        if (syncMutex.isLocked) {
            DiagnosticLogger.logWarning("GlucoseSyncRepository", "SYNC SKIPPED reason=already running")
            return SyncResult(
                status = SyncStatus.SKIPPED,
                personsCount = 0,
                inserted = 0,
                duplicates = 0,
                errorMessage = "already running"
            )
        }

        return syncMutex.withLock {
            val startedAt = Instant.now()
            DiagnosticLogger.logInfo("GlucoseSyncRepository", "SYNC START reason=${reason.name}")
            var personsCount = 0
            var inserted = 0
            var duplicates = 0
            var errorMessage: String? = null
            var httpStatus: Int? = null
            var retryAfterSeconds: Int? = null
            var status = SyncStatus.SUCCESS

            try {
                authRepository.ensureAuthenticated(force = false)
                val persons = productionClient.getConnections()
                personsCount = persons.size
                localRepository.upsertObservedPersons(persons, Instant.now())

                for (person in persons) {
                    try {
                        val reading = productionClient.getLatestReading(person.patientId) ?: continue
                        val allPoints = (reading.history + listOf(
                            com.libredisplay.data.model.GlucoseHistoryPoint(
                                value = reading.value,
                                timestamp = reading.timestamp,
                                trend = reading.trend
                            )
                        )).distinctBy { it.timestamp }
                        val summary = localRepository.insertReadings(
                            patientId = person.patientId,
                            points = allPoints,
                            now = Instant.now()
                        )
                        inserted += summary.inserted
                        duplicates += summary.duplicates
                    } catch (http: LibreLinkUpHttpException) {
                        if (http.statusCode == 429) {
                            status = SyncStatus.RATE_LIMITED
                            httpStatus = 429
                            retryAfterSeconds = http.retryAfterSeconds
                            errorMessage = http.message
                            break
                        }
                        throw http
                    }
                }
            } catch (throwable: Throwable) {
                status = if (status == SyncStatus.RATE_LIMITED) SyncStatus.RATE_LIMITED else SyncStatus.FAILED
                errorMessage = throwable.message
                httpStatus = (throwable as? LibreLinkUpHttpException)?.statusCode ?: httpStatus
                retryAfterSeconds = (throwable as? LibreLinkUpHttpException)?.retryAfterSeconds ?: retryAfterSeconds
                DiagnosticLogger.logException("GlucoseSyncRepository", throwable, "syncAllPersons failed")
            } finally {
                localRepository.deleteReadingsOlderThan(days = 365, now = Instant.now())
                localRepository.saveSyncRun(
                    SyncRunEntity(
                        startedAt = startedAt,
                        finishedAt = Instant.now(),
                        status = status.name,
                        reason = reason.name,
                        personsCount = personsCount,
                        readingsInserted = inserted,
                        readingsSkippedDuplicate = duplicates,
                        errorMessage = errorMessage,
                        httpStatus = httpStatus,
                        retryAfterSeconds = retryAfterSeconds
                    )
                )
            }

            SyncResult(
                status = status,
                personsCount = personsCount,
                inserted = inserted,
                duplicates = duplicates,
                errorMessage = errorMessage,
                httpStatus = httpStatus,
                retryAfterSeconds = retryAfterSeconds
            ).also {
                DiagnosticLogger.logInfo(
                    "GlucoseSyncRepository",
                    "SYNC END inserted=${it.inserted} duplicates=${it.duplicates} status=${it.status.name}"
                )
            }
        }
    }

    suspend fun loadLatestLocalSnapshot(selectedPatientId: String?) =
        localRepository.loadLatestMonitoringSnapshot(selectedPatientId)

    suspend fun loadHistory(
        patientId: String,
        fromInclusive: Instant,
        toInclusive: Instant
    ) = localRepository.loadHistory(patientId, fromInclusive, toInclusive)
}

