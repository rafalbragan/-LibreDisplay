package com.libredisplay.data.repository

import android.content.Context
import com.libredisplay.data.local.GlucoseReadingDao
import com.libredisplay.data.local.LibreDisplayDatabase
import com.libredisplay.data.local.ObservedPersonDao
import com.libredisplay.data.local.SyncRunDao
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.storage.SecureStorage
import java.io.File
import java.time.Duration
import java.time.Instant

data class DatabaseStats(
    val databaseBytes: Long,
    val walBytes: Long,
    val shmBytes: Long,
    val totalBytes: Long,
    val readingsCount: Long,
    val peopleCount: Int,
    val oldestReadingTimestamp: Instant?,
    val newestReadingTimestamp: Instant?,
    val estimatedBytesPerReading: Long?,
    val estimatedGrowthPerWeekBytes: Long?,
    val estimatedGrowthPerMonthBytes: Long?,
    val availableRangeText: String
)

data class NetworkUsageStats(
    val totalDownloadedBytes: Long,
    val totalUploadedBytes: Long,
    val requestCount: Long,
    val successfulSyncCount: Long,
    val failedSyncCount: Long,
    val firstMeasuredAt: Instant?,
    val lastMeasuredAt: Instant?,
    val lastSyncAt: Instant?,
    val averageDownloadedPerDayBytes: Long?,
    val averageUploadedPerDayBytes: Long?,
    val averageDownloadedPerWeekBytes: Long?,
    val averageUploadedPerWeekBytes: Long?,
    val averageDownloadedPerMonthBytes: Long?,
    val averageUploadedPerMonthBytes: Long?,
    val estimatedCurrentDailyTransferBytes: Long?
)

data class PollingEstimate(
    val currentMinutes: Int,
    val selectedMinutes: Int,
    val currentDailyBytes: Long?,
    val selectedDailyBytes: Long?,
    val insufficientData: Boolean
)

data class RetentionEstimate(
    val retentionHours: Int,
    val estimatedReadings: Long?,
    val estimatedBytes: Long?,
    val insufficientData: Boolean
)

class DiagnosticsStatsRepository(
    private val context: Context,
    private val glucoseReadingDao: GlucoseReadingDao,
    private val observedPersonDao: ObservedPersonDao,
    private val syncRunDao: SyncRunDao,
    private val settingsRepository: SettingsRepository,
    private val secureStorage: SecureStorage = SecureStorage(context)
) {

    suspend fun loadDatabaseStats(now: Instant = Instant.now()): DatabaseStats {
        val dbDir = context.getDatabasePath(LibreDisplayDatabase.DB_NAME).parentFile
        val dbFile = context.getDatabasePath(LibreDisplayDatabase.DB_NAME)
        val walFile = File(dbDir, "${LibreDisplayDatabase.DB_NAME}-wal")
        val shmFile = File(dbDir, "${LibreDisplayDatabase.DB_NAME}-shm")

        val databaseBytes = dbFile.lengthSafe()
        val walBytes = walFile.lengthSafe()
        val shmBytes = shmFile.lengthSafe()
        val totalBytes = databaseBytes + walBytes + shmBytes

        val readingsCount = glucoseReadingDao.countLiveReadings()
        val peopleCount = observedPersonDao.countActiveLivePersons()
        val oldest = glucoseReadingDao.oldestLiveReadingTimestamp()
        val newest = glucoseReadingDao.newestLiveReadingTimestamp()

        val availableRange = if (oldest != null && newest != null) {
            "${formatDateTime(oldest)} - ${formatDateTime(newest)}"
        } else {
            "Brak danych"
        }

        val estimatedBytesPerReading = if (readingsCount >= 100 && totalBytes > 0) {
            (totalBytes / readingsCount).coerceAtLeast(1)
        } else {
            null
        }

        val estimatedGrowthPerWeekBytes = estimateGrowth(
            now = now,
            oldest = oldest,
            newest = newest,
            readingsCount = readingsCount,
            bytesPerReading = estimatedBytesPerReading,
            targetDays = 7
        )

        val estimatedGrowthPerMonthBytes = estimateGrowth(
            now = now,
            oldest = oldest,
            newest = newest,
            readingsCount = readingsCount,
            bytesPerReading = estimatedBytesPerReading,
            targetDays = 30
        )

        return DatabaseStats(
            databaseBytes = databaseBytes,
            walBytes = walBytes,
            shmBytes = shmBytes,
            totalBytes = totalBytes,
            readingsCount = readingsCount,
            peopleCount = peopleCount,
            oldestReadingTimestamp = oldest,
            newestReadingTimestamp = newest,
            estimatedBytesPerReading = estimatedBytesPerReading,
            estimatedGrowthPerWeekBytes = estimatedGrowthPerWeekBytes,
            estimatedGrowthPerMonthBytes = estimatedGrowthPerMonthBytes,
            availableRangeText = availableRange
        )
    }

    suspend fun loadNetworkUsageStats(mode: AppMode): NetworkUsageStats {
        if (mode == AppMode.DEMO) {
            return NetworkUsageStats(
                totalDownloadedBytes = 0,
                totalUploadedBytes = 0,
                requestCount = 0,
                successfulSyncCount = 0,
                failedSyncCount = 0,
                firstMeasuredAt = null,
                lastMeasuredAt = null,
                lastSyncAt = null,
                averageDownloadedPerDayBytes = null,
                averageUploadedPerDayBytes = null,
                averageDownloadedPerWeekBytes = null,
                averageUploadedPerWeekBytes = null,
                averageDownloadedPerMonthBytes = null,
                averageUploadedPerMonthBytes = null,
                estimatedCurrentDailyTransferBytes = null
            )
        }

        val downloaded = secureStorage.getString(SecureStorage.KEY_NETWORK_TOTAL_DOWNLOADED_BYTES).toLongOrNull() ?: 0L
        val uploaded = secureStorage.getString(SecureStorage.KEY_NETWORK_TOTAL_UPLOADED_BYTES).toLongOrNull() ?: 0L
        val requestCount = secureStorage.getString(SecureStorage.KEY_NETWORK_REQUEST_COUNT).toLongOrNull() ?: 0L
        val successCount = secureStorage.getString(SecureStorage.KEY_NETWORK_SUCCESS_COUNT).toLongOrNull() ?: 0L
        val failedCount = secureStorage.getString(SecureStorage.KEY_NETWORK_FAILED_COUNT).toLongOrNull() ?: 0L
        val firstAt = secureStorage.getString(SecureStorage.KEY_NETWORK_FIRST_MEASURED_AT).toLongOrNull()?.let { Instant.ofEpochMilli(it) }
        val lastAt = secureStorage.getString(SecureStorage.KEY_NETWORK_LAST_MEASURED_AT).toLongOrNull()?.let { Instant.ofEpochMilli(it) }
        val lastSyncAt = syncRunDao.latestSuccessfulFinishedAt()

        val averages = calculateAverages(downloaded, uploaded, requestCount, firstAt, lastAt)

        return NetworkUsageStats(
            totalDownloadedBytes = downloaded,
            totalUploadedBytes = uploaded,
            requestCount = requestCount,
            successfulSyncCount = successCount,
            failedSyncCount = failedCount,
            firstMeasuredAt = firstAt,
            lastMeasuredAt = lastAt,
            lastSyncAt = lastSyncAt,
            averageDownloadedPerDayBytes = averages?.downloadPerDay,
            averageUploadedPerDayBytes = averages?.uploadPerDay,
            averageDownloadedPerWeekBytes = averages?.downloadPerWeek,
            averageUploadedPerWeekBytes = averages?.uploadPerWeek,
            averageDownloadedPerMonthBytes = averages?.downloadPerMonth,
            averageUploadedPerMonthBytes = averages?.uploadPerMonth,
            estimatedCurrentDailyTransferBytes = averages?.totalPerDay
        )
    }

    suspend fun estimateRetention(retentionHours: Int, now: Instant = Instant.now()): RetentionEstimate {
        val stats = loadDatabaseStats(now)
        val oldest = stats.oldestReadingTimestamp
        val newest = stats.newestReadingTimestamp

        if (oldest == null || newest == null || stats.estimatedBytesPerReading == null || stats.readingsCount < 100) {
            return RetentionEstimate(
                retentionHours = retentionHours,
                estimatedReadings = null,
                estimatedBytes = null,
                insufficientData = true
            )
        }

        val observedHours = Duration.between(oldest, newest).toHours().coerceAtLeast(1)
        val readingsPerHour = stats.readingsCount.toDouble() / observedHours.toDouble()
        val estimatedReadings = (readingsPerHour * retentionHours).toLong().coerceAtLeast(0)
        val estimatedBytes = estimatedReadings * stats.estimatedBytesPerReading

        return RetentionEstimate(
            retentionHours = retentionHours,
            estimatedReadings = estimatedReadings,
            estimatedBytes = estimatedBytes,
            insufficientData = false
        )
    }

    suspend fun estimatePolling(selectedMinutes: Int): PollingEstimate {
        val settings = settingsRepository.loadSettings()
        val network = loadNetworkUsageStats(settings.appMode)

        val totalBytes = network.totalDownloadedBytes + network.totalUploadedBytes
        val successCount = network.successfulSyncCount
        if (successCount < 5L || totalBytes <= 0L) {
            return PollingEstimate(
                currentMinutes = settings.backgroundPollingMinutes,
                selectedMinutes = selectedMinutes,
                currentDailyBytes = null,
                selectedDailyBytes = null,
                insufficientData = true
            )
        }

        val bytesPerSync = totalBytes / successCount
        val currentSyncsPerDay = (24 * 60) / settings.backgroundPollingMinutes
        val selectedSyncsPerDay = (24 * 60) / selectedMinutes

        return PollingEstimate(
            currentMinutes = settings.backgroundPollingMinutes,
            selectedMinutes = selectedMinutes,
            currentDailyBytes = bytesPerSync * currentSyncsPerDay,
            selectedDailyBytes = bytesPerSync * selectedSyncsPerDay,
            insufficientData = false
        )
    }

    fun formatBytes(bytes: Long): String {
        val b = bytes.coerceAtLeast(0)
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            b < 1024 -> "$b B"
            b < mb -> "${"%.1f".format(b / kb)} KB"
            b < gb -> "${"%.1f".format(b / mb)} MB"
            else -> "${"%.2f".format(b / gb)} GB"
        }
    }

    private fun estimateGrowth(
        now: Instant,
        oldest: Instant?,
        newest: Instant?,
        readingsCount: Long,
        bytesPerReading: Long?,
        targetDays: Int
    ): Long? {
        if (oldest == null || newest == null || bytesPerReading == null || readingsCount < 100) return null
        val spanDays = Duration.between(oldest, newest).toDays().coerceAtLeast(1)
        val readingsPerDay = readingsCount.toDouble() / spanDays.toDouble()
        val estimatedReadings = (readingsPerDay * targetDays).toLong().coerceAtLeast(0)
        return estimatedReadings * bytesPerReading
    }

    private data class AverageWindow(
        val downloadPerDay: Long,
        val uploadPerDay: Long,
        val downloadPerWeek: Long,
        val uploadPerWeek: Long,
        val downloadPerMonth: Long,
        val uploadPerMonth: Long,
        val totalPerDay: Long
    )

    private fun calculateAverages(
        downloaded: Long,
        uploaded: Long,
        requestCount: Long,
        firstAt: Instant?,
        lastAt: Instant?
    ): AverageWindow? {
        if (requestCount < 5L || firstAt == null || lastAt == null || !lastAt.isAfter(firstAt)) return null
        val days = Duration.between(firstAt, lastAt).toDays().coerceAtLeast(1)
        val downPerDay = downloaded / days
        val upPerDay = uploaded / days
        return AverageWindow(
            downloadPerDay = downPerDay,
            uploadPerDay = upPerDay,
            downloadPerWeek = downPerDay * 7,
            uploadPerWeek = upPerDay * 7,
            downloadPerMonth = downPerDay * 30,
            uploadPerMonth = upPerDay * 30,
            totalPerDay = downPerDay + upPerDay
        )
    }

    private fun File.lengthSafe(): Long = if (exists()) length().coerceAtLeast(0) else 0

    private fun formatDateTime(instant: Instant): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
        return formatter.format(instant)
    }
}

