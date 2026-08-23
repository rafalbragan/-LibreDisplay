package com.libredisplay.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.room.withTransaction
import com.libredisplay.BuildConfig
import com.libredisplay.data.api.PersistedLibreLinkUpSession
import com.libredisplay.data.backup.BackupBundle
import com.libredisplay.data.backup.BackupCodec
import com.libredisplay.data.backup.BackupCoverageCalculator
import com.libredisplay.data.backup.BackupFormatException
import com.libredisplay.data.backup.BackupMergeEngine
import com.libredisplay.data.backup.BackupPatientSettingsDto
import com.libredisplay.data.backup.BackupPersonDto
import com.libredisplay.data.backup.BackupReadingDto
import com.libredisplay.data.backup.BackupSessionDto
import com.libredisplay.data.backup.BackupSettingsDto
import com.libredisplay.data.backup.ConflictResolution
import com.libredisplay.data.backup.LocalBackupStore
import com.libredisplay.data.backup.LocalReadingKey
import com.libredisplay.data.backup.PersonDataCoverage
import com.libredisplay.data.backup.RestorePlan
import com.libredisplay.data.local.GlucoseReadingDao
import com.libredisplay.data.local.GlucoseReadingEntity
import com.libredisplay.data.local.LibreDisplayDatabase
import com.libredisplay.data.local.ObservedPersonDao
import com.libredisplay.data.local.ObservedPersonEntity
import com.libredisplay.data.local.PatientSettingsDao
import com.libredisplay.data.local.PatientSettingsEntity
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.model.HbA1cSettings
import com.libredisplay.data.model.QuickMetricId
import com.libredisplay.diagnostics.DiagnosticLogger
import java.io.File
import java.time.Instant
import java.time.LocalDate

/**
 * Owns every backup related operation.
 *
 * Key behaviour:
 *  - exactly ONE automatic backup file lives inside the application data directory,
 *  - the user never picks a location, a name or a password,
 *  - the backup only contains the people the user currently sees after logging in (LIVE data),
 *  - the same file can be exported / shared to move data and configuration to another phone,
 *  - restore merges identical readings, reports what was added and asks about differences.
 */
class AppDataBackupRepository(
    private val context: Context,
    private val database: LibreDisplayDatabase,
    private val settingsRepository: SettingsRepository,
    private val observedPersonDao: ObservedPersonDao,
    private val glucoseReadingDao: GlucoseReadingDao,
    private val patientSettingsDao: PatientSettingsDao,
    backupRootDirectory: File = context.filesDir
) {

    private val store = LocalBackupStore(backupRootDirectory)

    // ------------------------------------------------------------------ public types

    data class BackupSummary(
        val livePersons: Int,
        val liveReadings: Int,
        val patientSettings: Int
    )

    data class AutomaticBackupInfo(
        val exists: Boolean,
        val absolutePath: String,
        val sizeBytes: Long,
        val lastModifiedEpochMillis: Long?,
        val persons: Int,
        val readings: Int
    )

    data class StagedRestore(
        val bundle: BackupBundle,
        val plan: RestorePlan
    )

    data class RestoreResult(
        val summary: BackupSummary,
        val report: String
    )

    data class BackupPersonPreview(
        val patientId: String,
        val displayName: String,
        val readingsCount: Int,
        val existsLocally: Boolean,
        val defaultSelected: Boolean
    )

    data class BackupPreview(
        val schemaVersion: Int,
        val createdAt: String,
        val appVersion: String,
        val persons: List<BackupPersonPreview>,
        val settingsAvailable: Boolean
    )

    enum class RestoreMode { MERGE, REPLACE }

    data class PersonRestoreSelection(
        val patientId: String,
        val mode: RestoreMode = RestoreMode.MERGE
    )

    data class RestoreSelection(
        val people: List<PersonRestoreSelection>,
        val restoreSettings: Boolean
    ) {
        companion object {
            fun all(people: List<PersonRestoreSelection>): RestoreSelection =
                RestoreSelection(people = people, restoreSettings = true)
        }
    }

    data class ExportSelection(
        val selectedPatientIds: Set<String>,
        val includeSettings: Boolean
    ) {
        companion object {
            fun all(selectedPatientIds: Set<String>): ExportSelection =
                ExportSelection(selectedPatientIds = selectedPatientIds, includeSettings = true)
        }
    }

    /**
     * Everything the user needs to decide whether the stored database should be loaded:
     * who is inside, for which period and how complete that period is.
     */
    data class BackupOffer(
        val exists: Boolean,
        val createdAtIso: String?,
        val appVersion: String?,
        val persons: List<PersonDataCoverage>,
        val settingsAvailable: Boolean,
        val errorMessage: String? = null
    ) {
        val totalReadings: Int get() = persons.sumOf { it.readingsCount }
        val hasData: Boolean get() = exists && persons.any { it.readingsCount > 0 }

        companion object {
            val EMPTY = BackupOffer(
                exists = false,
                createdAtIso = null,
                appVersion = null,
                persons = emptyList(),
                settingsAvailable = false
            )
        }
    }

    // ------------------------------------------------------------------ status

    suspend fun isLocalLiveDataEmpty(): Boolean {
        val persons = observedPersonDao.getAllLivePersons().size
        val readings = glucoseReadingDao.countLiveReadings()
        return persons == 0 && readings == 0L
    }

    fun automaticBackupFile(): File = store.backupFile

    suspend fun automaticBackupInfo(): AutomaticBackupInfo {
        val text = store.read()
        val bundle = text?.let { runCatching { BackupCodec.decode(it) }.getOrNull() }
        return AutomaticBackupInfo(
            exists = store.exists(),
            absolutePath = store.backupFile.absolutePath,
            sizeBytes = store.sizeBytes(),
            lastModifiedEpochMillis = store.lastModifiedEpochMillis(),
            persons = bundle?.persons?.size ?: 0,
            readings = bundle?.readings?.size ?: 0
        )
    }

    /**
     * Describes the locally stored database so the user can be asked
     * "czy wczytać dane, które mam?" instead of an abstract "przywrócić kopię?".
     */
    suspend fun loadAutomaticBackupOffer(): BackupOffer {
        val content = store.read() ?: return BackupOffer.EMPTY
        val bundle = runCatching { BackupCodec.decode(content) }.getOrElse { error ->
            DiagnosticLogger.logWarning(
                "AppDataBackupRepository",
                "AUTO BACKUP OFFER decode failed reason=${error.message}"
            )
            return BackupOffer.EMPTY.copy(
                exists = true,
                errorMessage = "Zapisany plik danych jest uszkodzony i nie można go odczytać."
            )
        }
        return bundle.withoutDemoContent().toOffer()
    }

    /** Same description, but for an external file the user picked. */
    suspend fun loadOfferFromUri(uri: Uri, password: String? = null): BackupOffer {
        val bundle = BackupCodec.decode(readFromUri(uri), password)
        return bundle.withoutDemoContent().toOffer()
    }

    private fun BackupBundle.toOffer(): BackupOffer {
        val readings = readingsByPatient
        val names = persons.associate { it.patientId to it.displayName }
        val patientIds = (persons.map { it.patientId } + readings.keys).distinct()
        return BackupOffer(
            exists = true,
            createdAtIso = createdAtIso,
            appVersion = appVersion,
            persons = patientIds.map { patientId ->
                BackupCoverageCalculator.forPerson(
                    patientId = patientId,
                    displayName = names[patientId] ?: patientId,
                    timestamps = readings[patientId]
                        .orEmpty()
                        .mapNotNull { BackupMergeEngine.parseInstantOrNull(it.timestampIso) }
                )
            }.sortedByDescending { it.readingsCount },
            settingsAvailable = settings != null
        )
    }

    // ------------------------------------------------------------------ create backup

    /** Silently refreshes the single automatic backup file. Never throws. */
    suspend fun refreshAutomaticBackupQuietly() {
        runCatching { createAutomaticBackup(includeConfiguration = true) }
            .onFailure {
                DiagnosticLogger.logWarning(
                    "AppDataBackupRepository",
                    "AUTO BACKUP REFRESH FAILED reason=${it.message}"
                )
            }
    }


    /**
     * Rebuilds the single automatic backup file from the currently visible (LIVE) data.
     *
     * @param includeConfiguration when true the file also carries the app configuration and the
     * saved LibreLinkUp session so the whole setup can be moved to another phone.
     */
    suspend fun createAutomaticBackup(includeConfiguration: Boolean = true): BackupSummary {
        val bundle = buildBundle(
            selectedPatientIds = null,
            includeSettings = includeConfiguration,
            includeSession = includeConfiguration
        )
        store.write(BackupCodec.encode(bundle))
        DiagnosticLogger.logInfo(
            "AppDataBackupRepository",
            "AUTO BACKUP WRITTEN persons=${bundle.persons.size} readings=${bundle.readings.size} path=${store.backupFile.absolutePath}"
        )
        return BackupSummary(
            livePersons = bundle.persons.size,
            liveReadings = bundle.readings.size,
            patientSettings = bundle.patientSettings.size
        )
    }

    /** Copies the automatic backup (creating it when missing) to a user chosen destination. */
    suspend fun exportAutomaticBackupTo(uri: Uri): BackupSummary {
        val summary = createAutomaticBackup(includeConfiguration = true)
        val content = store.read()
            ?: throw BackupFormatException("Brak pliku kopii zapasowej do wyeksportowania.")
        writeToUri(uri, content)
        DiagnosticLogger.logInfo("AppDataBackupRepository", "BACKUP EXPORT to uri success")
        return summary
    }

    // ------------------------------------------------------------------ read backup

    suspend fun stageAutomaticBackupRestore(): StagedRestore {
        val content = store.read()
            ?: throw BackupFormatException("Nie znaleziono automatycznej kopii zapasowej na tym urządzeniu.")
        return stageRestore(BackupCodec.decode(content))
    }

    suspend fun stageRestoreFromUri(uri: Uri, password: String? = null): StagedRestore {
        val content = readFromUri(uri)
        return stageRestore(BackupCodec.decode(content, password))
    }

    private suspend fun stageRestore(bundle: BackupBundle): StagedRestore {
        val safeBundle = bundle.withoutDemoContent()
        val localPersons = observedPersonDao.getAllLivePersons()
        val localReadings = glucoseReadingDao.getAllLiveReadings().map {
            LocalReadingKey(
                patientId = it.patientId,
                epochMillis = it.timestamp.toEpochMilli(),
                valueMgDl = it.valueMgDl
            )
        }
        val plan = BackupMergeEngine.buildPlan(
            bundle = safeBundle,
            localReadings = localReadings,
            localPatientIds = localPersons.map { it.patientId }.toSet()
        )
        return StagedRestore(bundle = safeBundle, plan = plan)
    }

    // ------------------------------------------------------------------ apply restore

    suspend fun applyRestorePlan(
        staged: StagedRestore,
        selectedPatientIds: Set<String>,
        conflictResolution: ConflictResolution,
        restoreConfiguration: Boolean
    ): RestoreResult {
        val bundle = staged.bundle
        val readingsByPatient = bundle.readingsByPatient
        val selectedPlans = staged.plan.persons.filter { it.patientId in selectedPatientIds }

        val personEntities = bundle.persons
            .filter { it.patientId in selectedPatientIds }
            .mapNotNull { it.toEntityOrNull() }

        val readingEntities = selectedPlans.flatMap { plan ->
            BackupMergeEngine.readingsToWrite(
                plan = plan,
                backupReadings = readingsByPatient[plan.patientId].orEmpty(),
                resolution = conflictResolution
            )
        }.mapNotNull { it.toEntityOrNull() }

        val patientSettingsEntities = bundle.patientSettings
            .filter { it.patientId in selectedPatientIds }
            .mapNotNull { it.toEntityOrNull() }

        database.withTransaction {
            glucoseReadingDao.deleteDemoReadings()
            observedPersonDao.deleteDemoPeople()
            patientSettingsDao.deleteDemoSettings()
            if (personEntities.isNotEmpty()) observedPersonDao.upsertAll(personEntities)
            if (readingEntities.isNotEmpty()) glucoseReadingDao.insertReplace(readingEntities)
            patientSettingsEntities.forEach { patientSettingsDao.upsert(it) }
        }

        if (restoreConfiguration) {
            applyConfiguration(bundle)
        }
        patientSettingsEntities.forEach {
            settingsRepository.saveHbA1cSettings(
                HbA1cSettings(
                    patientId = it.patientId,
                    labHbA1cPercent = it.labHba1cPercent,
                    labHbA1cDate = it.labHba1cDate,
                    targetHbA1cPercent = it.hba1cTargetPercent
                )
            )
        }

        val report = buildString {
            appendLine("Przywracanie zakończone.")
            selectedPlans.forEach { appendLine(BackupMergeEngine.describePerson(it)) }
            if (staged.plan.totalConflicts > 0) {
                appendLine(
                    when (conflictResolution) {
                        ConflictResolution.KEEP_LOCAL -> "Rozbieżne odczyty: zachowano dane z aplikacji."
                        ConflictResolution.KEEP_BACKUP -> "Rozbieżne odczyty: zachowano dane z archiwum."
                    }
                )
            }
            if (restoreConfiguration) appendLine("Przywrócono konfigurację aplikacji.")
        }.trim()

        DiagnosticLogger.logInfo(
            "AppDataBackupRepository",
            "BACKUP RESTORE success persons=${personEntities.size} readings=${readingEntities.size} conflicts=${staged.plan.totalConflicts}"
        )

        // Refresh the automatic backup so the merged state is protected immediately.
        runCatching { createAutomaticBackup(includeConfiguration = true) }

        return RestoreResult(
            summary = BackupSummary(
                livePersons = personEntities.size,
                liveReadings = readingEntities.size,
                patientSettings = patientSettingsEntities.size
            ),
            report = report
        )
    }

    /** Applies everything the staged plan contains, for every person it mentions. */
    suspend fun applyWholeStagedRestore(
        staged: StagedRestore,
        conflictResolution: ConflictResolution = ConflictResolution.KEEP_LOCAL,
        restoreConfiguration: Boolean = true
    ): RestoreResult = applyRestorePlan(
        staged = staged,
        selectedPatientIds = staged.plan.persons.map { it.patientId }.toSet(),
        conflictResolution = conflictResolution,
        restoreConfiguration = restoreConfiguration
    )

    /**
     * Runs right after a verified LibreLinkUp login: the archive is merged into the live database
     * without touching the freshly verified credentials and without asking anything, because
     * identical readings simply merge. Returns null when there is nothing to merge.
     */
    suspend fun mergeAutomaticBackupAfterLogin(): RestoreResult? {
        val staged = runCatching { stageAutomaticBackupRestore() }.getOrElse { error ->
            DiagnosticLogger.logWarning(
                "AppDataBackupRepository",
                "LOGIN MERGE skipped reason=${error.message}"
            )
            return null
        }
        if (staged.plan.persons.none { it.hasAnythingToRestore }) return null
        return applyWholeStagedRestore(
            staged = staged,
            // The device already holds verified data; differences are surfaced separately.
            conflictResolution = ConflictResolution.KEEP_LOCAL,
            restoreConfiguration = false
        )
    }

    private fun applyConfiguration(bundle: BackupBundle) {        bundle.settings?.let { settings ->
            settingsRepository.saveSettings(settings.toAppSettings(settingsRepository.loadSettings()))
        }
        if (bundle.quickMetricOrder.isNotEmpty()) {
            settingsRepository.saveQuickMetricsOrder(
                QuickMetricId.normalizeOrder(bundle.quickMetricOrder.mapNotNull(QuickMetricId::fromStorageId))
            )
        }
        bundle.quickMetricVisibility
            ?.mapNotNull { (storageId, visible) ->
                QuickMetricId.fromStorageId(storageId)?.let { it to visible }
            }
            ?.toMap()
            ?.takeIf { it.isNotEmpty() }
            ?.let(settingsRepository::saveQuickMetricsVisibility)
        bundle.session?.let { session ->
            settingsRepository.savePersistedSession(
                PersistedLibreLinkUpSession(
                    token = session.token,
                    userId = session.userId,
                    accountIdHash = session.accountIdHash,
                    region = session.region,
                    baseUrl = session.baseUrl,
                    tokenExpiresAtEpochSeconds = session.tokenExpiresAtEpochSeconds
                )
            )
        }
    }

    // ------------------------------------------------------------------ legacy compatible API

    /**
     * Legacy entry point kept for compatibility. The password argument is ignored for newly
     * written files - LibreCare no longer asks the user for a backup password.
     */
    suspend fun exportToUri(uri: Uri, password: String = "", selection: ExportSelection? = null): BackupSummary {
        val bundle = buildBundle(
            selectedPatientIds = selection?.selectedPatientIds,
            includeSettings = selection?.includeSettings != false,
            includeSession = selection?.includeSettings != false
        )
        val encoded = BackupCodec.encode(bundle)
        writeToUri(uri, encoded)
        runCatching { store.write(encoded) }
        DiagnosticLogger.logInfo(
            "AppDataBackupRepository",
            "BACKUP EXPORT success persons=${bundle.persons.size} readings=${bundle.readings.size}"
        )
        return BackupSummary(
            livePersons = bundle.persons.size,
            liveReadings = bundle.readings.size,
            patientSettings = bundle.patientSettings.size
        )
    }

    suspend fun loadExportPreview(): BackupPreview {
        val bundle = buildBundle(selectedPatientIds = null, includeSettings = true, includeSession = true)
        val localIds = observedPersonDao.getAllLivePersons().map { it.patientId }.toSet()
        val readingsByPatient = bundle.readingsByPatient
        return BackupPreview(
            schemaVersion = BackupBundle.CURRENT_SCHEMA_VERSION,
            createdAt = bundle.createdAtIso,
            appVersion = bundle.appVersion,
            persons = bundle.persons.map { person ->
                BackupPersonPreview(
                    patientId = person.patientId,
                    displayName = person.displayName,
                    readingsCount = readingsByPatient[person.patientId]?.size ?: 0,
                    existsLocally = person.patientId in localIds,
                    defaultSelected = true
                )
            },
            settingsAvailable = true
        )
    }

    suspend fun loadRestorePreview(uri: Uri, password: String = ""): BackupPreview =
        stageRestoreFromUri(uri, password.takeIf { it.isNotBlank() }).toPreview()

    suspend fun restoreFromUri(
        uri: Uri,
        password: String = "",
        selection: RestoreSelection? = null
    ): BackupSummary {
        val staged = stageRestoreFromUri(uri, password.takeIf { it.isNotBlank() })
        val selectedIds = selection?.people?.map { it.patientId }?.toSet()
            ?: staged.plan.persons.map { it.patientId }.toSet()
        val replaceIds = selection?.people.orEmpty()
            .filter { it.mode == RestoreMode.REPLACE }
            .map { it.patientId }
            .toSet()

        if (replaceIds.isNotEmpty()) {
            database.withTransaction {
                replaceIds.forEach { patientId ->
                    glucoseReadingDao.deleteReadingsForPerson(patientId)
                    patientSettingsDao.deleteByPatientId(patientId)
                }
            }
        }

        val effectiveStaged = if (replaceIds.isEmpty()) staged else stageRestore(staged.bundle)
        return applyRestorePlan(
            staged = effectiveStaged,
            selectedPatientIds = selectedIds,
            conflictResolution = ConflictResolution.KEEP_BACKUP,
            restoreConfiguration = selection?.restoreSettings ?: true
        ).summary
    }

    private fun StagedRestore.toPreview(): BackupPreview = BackupPreview(
        schemaVersion = plan.schemaVersion,
        createdAt = plan.createdAtIso,
        appVersion = plan.appVersion,
        persons = plan.persons.map { person ->
            BackupPersonPreview(
                patientId = person.patientId,
                displayName = person.displayName,
                readingsCount = bundle.readingsByPatient[person.patientId]?.size ?: 0,
                existsLocally = person.existsLocally,
                defaultSelected = true
            )
        },
        settingsAvailable = plan.settingsAvailable
    )

    // ------------------------------------------------------------------ bundle building

    private suspend fun buildBundle(
        selectedPatientIds: Set<String>?,
        includeSettings: Boolean,
        includeSession: Boolean
    ): BackupBundle {
        val persons = observedPersonDao.getAllLivePersons()
            .filter { selectedPatientIds == null || it.patientId in selectedPatientIds }
        val visibleIds = persons.map { it.patientId }.toSet()
        val readings = glucoseReadingDao.getAllLiveReadings().filter { it.patientId in visibleIds }
        val patientSettings = patientSettingsDao.getAllLiveSettings().filter { it.patientId in visibleIds }
        val currentSettings = settingsRepository.loadSettings()

        return BackupBundle(
            schemaVersion = BackupBundle.CURRENT_SCHEMA_VERSION,
            createdAtIso = Instant.now().toString(),
            appVersion = BuildConfig.VERSION_NAME,
            deviceLabel = "${Build.MANUFACTURER.orEmpty()} ${Build.MODEL.orEmpty()}".trim(),
            persons = persons.map { it.toDto() },
            readings = readings.map { it.toDto() },
            patientSettings = patientSettings.map { it.toDto() },
            settings = if (includeSettings) currentSettings.toDto() else null,
            quickMetricOrder = settingsRepository.loadQuickMetricsOrder().map { it.storageId },
            quickMetricVisibility = settingsRepository.loadQuickMetricsVisibility()
                .mapKeys { it.key.storageId },
            session = if (includeSession) settingsRepository.loadPersistedSession()?.toDto() else null
        )
    }

    // ------------------------------------------------------------------ IO helpers

    private fun writeToUri(uri: Uri, content: String) {
        if (uri.scheme.equals("file", ignoreCase = true)) {
            val path = requireNotNull(uri.path) { "Brak ścieżki pliku kopii zapasowej." }
            File(path).writeText(content, Charsets.UTF_8)
            return
        }
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw BackupFormatException("Nie można otworzyć pliku docelowego kopii zapasowej.")
        output.use { it.write(content.toByteArray(Charsets.UTF_8)) }
    }

    private fun readFromUri(uri: Uri): String {
        if (uri.scheme.equals("file", ignoreCase = true)) {
            val path = requireNotNull(uri.path) { "Brak ścieżki pliku kopii zapasowej." }
            return File(path).readText(Charsets.UTF_8)
        }
        val input = context.contentResolver.openInputStream(uri)
            ?: throw BackupFormatException("Nie można otworzyć pliku kopii zapasowej.")
        return input.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    // ------------------------------------------------------------------ mapping

    private fun BackupBundle.withoutDemoContent(): BackupBundle = copy(
        persons = persons.filterNot { it.patientId.startsWith(DEMO_PREFIX) },
        readings = readings
            .filterNot { it.patientId.startsWith(DEMO_PREFIX) }
            .filterNot { it.source.equals(DEMO_SOURCE, ignoreCase = true) },
        patientSettings = patientSettings.filterNot { it.patientId.startsWith(DEMO_PREFIX) },
        settings = settings?.copy(
            appMode = if (settings.appMode == AppMode.DEMO.name) AppMode.NONE.name else settings.appMode,
            selectedPatientId = settings.selectedPatientId?.takeIf { !it.startsWith(DEMO_PREFIX) }
        )
    )

    private fun ObservedPersonEntity.toDto(): BackupPersonDto = BackupPersonDto(
        patientId = patientId,
        firstName = firstName,
        lastName = lastName,
        displayName = displayName,
        connectionId = connectionId,
        isActive = isActive,
        lastSeenAtIso = lastSeenAt.toString(),
        createdAtIso = createdAt.toString(),
        updatedAtIso = updatedAt.toString()
    )

    private fun GlucoseReadingEntity.toDto(): BackupReadingDto = BackupReadingDto(
        id = id,
        patientId = patientId,
        timestampIso = timestamp.toString(),
        valueMgDl = valueMgDl,
        trendArrow = trendArrow,
        trendLabel = trendLabel,
        source = source,
        sourceAccountId = sourceAccountId,
        receivedAtIso = receivedAt.toString(),
        isValid = isValid,
        rawTrendCode = rawTrendCode,
        createdAtIso = createdAt.toString()
    )

    private fun PatientSettingsEntity.toDto(): BackupPatientSettingsDto = BackupPatientSettingsDto(
        patientId = patientId,
        lowCriticalMgDl = lowCriticalMgDl,
        lowMgDl = lowMgDl,
        targetLowMgDl = targetLowMgDl,
        targetHighMgDl = targetHighMgDl,
        highMgDl = highMgDl,
        hba1cTargetPercent = hba1cTargetPercent,
        labHba1cPercent = labHba1cPercent,
        labHba1cDateIso = labHba1cDate?.toString(),
        updatedAtIso = updatedAt.toString()
    )

    private fun AppSettings.toDto(): BackupSettingsDto = BackupSettingsDto(
        email = email,
        password = password,
        selectedPatientId = selectedPatientId?.takeIf { !it.startsWith(DEMO_PREFIX) },
        region = region,
        regionMode = regionMode,
        customBaseUrl = customBaseUrl,
        refreshInterval = refreshInterval,
        targetLow = targetLow,
        targetHigh = targetHigh,
        trendWindowMinutes = trendWindowMinutes,
        showStatistics = showStatistics,
        kioskMode = kioskMode,
        appMode = if (appMode == AppMode.DEMO) AppMode.NONE.name else appMode.name,
        useAuthV3 = useAuthV3,
        retentionHours = retentionHours,
        backgroundPollingMinutes = backgroundPollingMinutes
    )

    private fun PersistedLibreLinkUpSession.toDto(): BackupSessionDto = BackupSessionDto(
        token = token,
        userId = userId,
        accountIdHash = accountIdHash,
        region = region,
        baseUrl = baseUrl,
        tokenExpiresAtEpochSeconds = tokenExpiresAtEpochSeconds
    )

    private fun BackupSettingsDto.toAppSettings(base: AppSettings): AppSettings {
        val mode = runCatching { AppMode.valueOf(appMode) }.getOrDefault(base.appMode)
        return base.copy(
            email = email.ifBlank { base.email },
            password = password.ifBlank { base.password },
            selectedPatientId = selectedPatientId,
            region = region,
            regionMode = regionMode,
            customBaseUrl = customBaseUrl,
            refreshInterval = refreshInterval,
            targetLow = targetLow,
            targetHigh = targetHigh,
            trendWindowMinutes = trendWindowMinutes,
            showStatistics = showStatistics,
            kioskMode = kioskMode,
            appMode = if (mode == AppMode.DEMO) AppMode.NONE else mode,
            useAuthV3 = useAuthV3,
            retentionHours = retentionHours,
            backgroundPollingMinutes = backgroundPollingMinutes
        )
    }

    private fun BackupPersonDto.toEntityOrNull(): ObservedPersonEntity? = runCatching {
        ObservedPersonEntity(
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
    }.onFailure {
        DiagnosticLogger.logWarning(
            "AppDataBackupRepository",
            "BACKUP RESTORE skipped person patientId=$patientId reason=${it.message}"
        )
    }.getOrNull()

    private fun BackupReadingDto.toEntityOrNull(): GlucoseReadingEntity? = runCatching {
        GlucoseReadingEntity(
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
    }.onFailure {
        DiagnosticLogger.logWarning(
            "AppDataBackupRepository",
            "BACKUP RESTORE skipped reading id=$id reason=${it.message}"
        )
    }.getOrNull()

    private fun BackupPatientSettingsDto.toEntityOrNull(): PatientSettingsEntity? = runCatching {
        PatientSettingsEntity(
            patientId = patientId,
            lowCriticalMgDl = lowCriticalMgDl,
            lowMgDl = lowMgDl,
            targetLowMgDl = targetLowMgDl,
            targetHighMgDl = targetHighMgDl,
            highMgDl = highMgDl,
            hba1cTargetPercent = hba1cTargetPercent,
            labHba1cPercent = labHba1cPercent,
            labHba1cDate = labHba1cDateIso?.let { text -> runCatching { LocalDate.parse(text) }.getOrNull() },
            updatedAt = Instant.parse(updatedAtIso)
        )
    }.onFailure {
        DiagnosticLogger.logWarning(
            "AppDataBackupRepository",
            "BACKUP RESTORE skipped patient settings patientId=$patientId reason=${it.message}"
        )
    }.getOrNull()

    private companion object {
        const val DEMO_PREFIX = "demo-person-"
        const val DEMO_SOURCE = "DemoMode"
    }
}

