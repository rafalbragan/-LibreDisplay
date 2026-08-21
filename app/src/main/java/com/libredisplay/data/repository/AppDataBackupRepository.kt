package com.libredisplay.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.libredisplay.BuildConfig
import com.libredisplay.data.api.PersistedLibreLinkUpSession
import com.libredisplay.data.local.GlucoseReadingDao
import com.libredisplay.data.local.GlucoseReadingEntity
import com.libredisplay.data.local.LibreDisplayDatabase
import com.libredisplay.data.local.ObservedPersonDao
import com.libredisplay.data.local.ObservedPersonEntity
import com.libredisplay.data.local.PatientSettingsDao
import com.libredisplay.data.local.PatientSettingsEntity
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.HbA1cSettings
import com.libredisplay.data.model.QuickMetricId
import com.libredisplay.diagnostics.DiagnosticLogger
import java.time.Instant
import java.time.LocalDate
import java.io.Reader
import java.io.Writer

class AppDataBackupRepository(
    private val context: Context,
    private val database: LibreDisplayDatabase,
    private val settingsRepository: SettingsRepository,
    private val observedPersonDao: ObservedPersonDao,
    private val glucoseReadingDao: GlucoseReadingDao,
    private val patientSettingsDao: PatientSettingsDao
) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToUri(uri: Uri): BackupSummary {
        val payload = buildPayload()
        openWriter(uri).use { writer ->
            gson.toJson(payload, writer)
        }

        DiagnosticLogger.logInfo(
            "AppDataBackupRepository",
            "BACKUP EXPORT success persons=${payload.livePersons.size} readings=${payload.liveReadings.size}"
        )
        return BackupSummary(
            livePersons = payload.livePersons.size,
            liveReadings = payload.liveReadings.size,
            patientSettings = payload.livePatientSettings.size
        )
    }

    suspend fun restoreFromUri(uri: Uri): BackupSummary {
        val payload: BackupPayload? = openReader(uri).use { reader ->
            gson.fromJson(reader, BackupPayload::class.java)
        }

        require(payload != null) { "Plik kopii zapasowej jest pusty lub uszkodzony." }
        require(payload.schemaVersion == BackupPayload.SCHEMA_VERSION) {
            "Nieobslugiwany format kopii zapasowej: ${payload.schemaVersion}."
        }

        val safePersons = payload.livePersons
            .filterNot { it.patientId.startsWith(DEMO_PREFIX) }
            .map { it.toEntity() }
        val safeReadings = payload.liveReadings
            .filterNot { it.patientId.startsWith(DEMO_PREFIX) }
            .filterNot { it.source.equals(DEMO_SOURCE, ignoreCase = true) }
            .map { it.toEntity() }
        val safePatientSettings = payload.livePatientSettings
            .filterNot { it.patientId.startsWith(DEMO_PREFIX) }
            .map { it.toEntity() }

        val restoredSettings = payload.settings
            .copy(
                appMode = if (payload.settings.appMode == AppMode.DEMO) AppMode.NONE else payload.settings.appMode,
                selectedPatientId = payload.settings.selectedPatientId?.takeIf { !it.startsWith(DEMO_PREFIX) }
            )

        database.withTransaction {
            glucoseReadingDao.deleteDemoReadings()
            glucoseReadingDao.deleteAllLiveReadings()
            observedPersonDao.deleteDemoPeople()
            observedPersonDao.deleteLivePeople()
            patientSettingsDao.deleteDemoSettings()
            patientSettingsDao.deleteLiveSettings()

            if (safePersons.isNotEmpty()) observedPersonDao.upsertAll(safePersons)
            if (safeReadings.isNotEmpty()) glucoseReadingDao.insertReplace(safeReadings)
            safePatientSettings.forEach { patientSettingsDao.upsert(it) }
        }

        settingsRepository.saveSettings(restoredSettings)
        settingsRepository.saveQuickMetricsOrder(
            QuickMetricId.normalizeOrder(payload.quickMetricOrder.mapNotNull(QuickMetricId::fromStorageId))
        )
        val restoredVisibility = payload.quickMetricVisibility
            ?.mapNotNull { (storageId, visible) ->
                QuickMetricId.fromStorageId(storageId)?.let { metricId -> metricId to visible }
            }
            ?.toMap()
        if (restoredVisibility != null) {
            settingsRepository.saveQuickMetricsVisibility(restoredVisibility)
        }

        if (payload.persistedSession != null) {
            settingsRepository.savePersistedSession(payload.persistedSession)
        } else {
            settingsRepository.clearPersistedSession()
        }

        safePatientSettings.forEach {
            settingsRepository.saveHbA1cSettings(
                HbA1cSettings(
                    patientId = it.patientId,
                    labHbA1cPercent = it.labHba1cPercent,
                    labHbA1cDate = it.labHba1cDate,
                    targetHbA1cPercent = it.hba1cTargetPercent
                )
            )
        }

        DiagnosticLogger.logInfo(
            "AppDataBackupRepository",
            "BACKUP RESTORE success persons=${safePersons.size} readings=${safeReadings.size}"
        )
        return BackupSummary(
            livePersons = safePersons.size,
            liveReadings = safeReadings.size,
            patientSettings = safePatientSettings.size
        )
    }

    private suspend fun buildPayload(): BackupPayload {
        val currentSettings = settingsRepository.loadSettings()
        val settings = currentSettings.copy(
            appMode = if (currentSettings.appMode == AppMode.DEMO) AppMode.NONE else currentSettings.appMode,
            selectedPatientId = currentSettings.selectedPatientId?.takeIf { !it.startsWith(DEMO_PREFIX) }
        )
        return BackupPayload(
            schemaVersion = BackupPayload.SCHEMA_VERSION,
            generatedAtEpochMillis = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            settings = settings,
            quickMetricOrder = settingsRepository.loadQuickMetricsOrder().map { it.storageId },
            quickMetricVisibility = settingsRepository.loadQuickMetricsVisibility().mapKeys { it.key.storageId },
            persistedSession = settingsRepository.loadPersistedSession(),
            livePersons = observedPersonDao.getAllLivePersons().map { BackupPerson.fromEntity(it) },
            liveReadings = glucoseReadingDao.getAllLiveReadings().map { BackupReading.fromEntity(it) },
            livePatientSettings = patientSettingsDao.getAllLiveSettings().map { BackupPatientSettings.fromEntity(it) }
        )
    }

    data class BackupSummary(
        val livePersons: Int,
        val liveReadings: Int,
        val patientSettings: Int
    )

    private data class BackupPayload(
        val schemaVersion: Int,
        val generatedAtEpochMillis: Long,
        val appVersion: String,
        val settings: com.libredisplay.data.model.AppSettings,
        val quickMetricOrder: List<String>,
        val quickMetricVisibility: Map<String, Boolean>? = null,
        val persistedSession: PersistedLibreLinkUpSession?,
        val livePersons: List<BackupPerson>,
        val liveReadings: List<BackupReading>,
        val livePatientSettings: List<BackupPatientSettings>
    ) {
        companion object {
            const val SCHEMA_VERSION = 1
        }
    }

    private data class BackupPerson(
        val patientId: String,
        val firstName: String?,
        val lastName: String?,
        val displayName: String,
        val connectionId: String?,
        val isActive: Boolean,
        val lastSeenAtIso: String,
        val createdAtIso: String,
        val updatedAtIso: String
    ) {
        fun toEntity(): ObservedPersonEntity = ObservedPersonEntity(
            patientId = patientId,
            firstName = firstName,
            lastName = lastName,
            displayName = displayName,
            connectionId = connectionId,
            isActive = isActive,
            lastSeenAt = Instant.parse(lastSeenAtIso),
            createdAt = Instant.parse(createdAtIso),
            updatedAt = Instant.parse(updatedAtIso)
        )

        companion object {
            fun fromEntity(entity: ObservedPersonEntity): BackupPerson = BackupPerson(
                patientId = entity.patientId,
                firstName = entity.firstName,
                lastName = entity.lastName,
                displayName = entity.displayName,
                connectionId = entity.connectionId,
                isActive = entity.isActive,
                lastSeenAtIso = entity.lastSeenAt.toString(),
                createdAtIso = entity.createdAt.toString(),
                updatedAtIso = entity.updatedAt.toString()
            )
        }
    }

    private data class BackupReading(
        val id: String,
        val patientId: String,
        val timestampIso: String,
        val valueMgDl: Int,
        val trendArrow: String?,
        val trendLabel: String?,
        val source: String,
        val sourceAccountId: String?,
        val receivedAtIso: String,
        val isValid: Boolean,
        val rawTrendCode: String?,
        val createdAtIso: String
    ) {
        fun toEntity(): GlucoseReadingEntity = GlucoseReadingEntity(
            id = id,
            patientId = patientId,
            timestamp = Instant.parse(timestampIso),
            valueMgDl = valueMgDl,
            trendArrow = trendArrow,
            trendLabel = trendLabel,
            source = source,
            sourceAccountId = sourceAccountId,
            receivedAt = Instant.parse(receivedAtIso),
            isValid = isValid,
            rawTrendCode = rawTrendCode,
            createdAt = Instant.parse(createdAtIso)
        )

        companion object {
            fun fromEntity(entity: GlucoseReadingEntity): BackupReading = BackupReading(
                id = entity.id,
                patientId = entity.patientId,
                timestampIso = entity.timestamp.toString(),
                valueMgDl = entity.valueMgDl,
                trendArrow = entity.trendArrow,
                trendLabel = entity.trendLabel,
                source = entity.source,
                sourceAccountId = entity.sourceAccountId,
                receivedAtIso = entity.receivedAt.toString(),
                isValid = entity.isValid,
                rawTrendCode = entity.rawTrendCode,
                createdAtIso = entity.createdAt.toString()
            )
        }
    }

    private data class BackupPatientSettings(
        val patientId: String,
        val lowCriticalMgDl: Int,
        val lowMgDl: Int,
        val targetLowMgDl: Int,
        val targetHighMgDl: Int,
        val highMgDl: Int,
        val hba1cTargetPercent: Double,
        val labHba1cPercent: Double?,
        val labHba1cDateIso: String?,
        val updatedAtIso: String
    ) {
        fun toEntity(): PatientSettingsEntity = PatientSettingsEntity(
            patientId = patientId,
            lowCriticalMgDl = lowCriticalMgDl,
            lowMgDl = lowMgDl,
            targetLowMgDl = targetLowMgDl,
            targetHighMgDl = targetHighMgDl,
            highMgDl = highMgDl,
            hba1cTargetPercent = hba1cTargetPercent,
            labHba1cPercent = labHba1cPercent,
            labHba1cDate = labHba1cDateIso?.let(LocalDate::parse),
            updatedAt = Instant.parse(updatedAtIso)
        )

        companion object {
            fun fromEntity(entity: PatientSettingsEntity): BackupPatientSettings = BackupPatientSettings(
                patientId = entity.patientId,
                lowCriticalMgDl = entity.lowCriticalMgDl,
                lowMgDl = entity.lowMgDl,
                targetLowMgDl = entity.targetLowMgDl,
                targetHighMgDl = entity.targetHighMgDl,
                highMgDl = entity.highMgDl,
                hba1cTargetPercent = entity.hba1cTargetPercent,
                labHba1cPercent = entity.labHba1cPercent,
                labHba1cDateIso = entity.labHba1cDate?.toString(),
                updatedAtIso = entity.updatedAt.toString()
            )
        }
    }

    private companion object {
        const val DEMO_PREFIX = "demo-person-"
        const val DEMO_SOURCE = "DemoMode"
    }

    private fun openWriter(uri: Uri): Writer {
        if (uri.scheme.equals("file", ignoreCase = true)) {
            val path = requireNotNull(uri.path) { "Brak sciezki pliku kopii zapasowej." }
            return java.io.File(path).writer(Charsets.UTF_8)
        }
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: error("Nie mozna otworzyc pliku docelowego kopii zapasowej.")
        return output.writer(Charsets.UTF_8)
    }

    private fun openReader(uri: Uri): Reader {
        if (uri.scheme.equals("file", ignoreCase = true)) {
            val path = requireNotNull(uri.path) { "Brak sciezki pliku kopii zapasowej." }
            return java.io.File(path).reader(Charsets.UTF_8)
        }
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Nie mozna otworzyc pliku kopii zapasowej.")
        return input.reader(Charsets.UTF_8)
    }
}


