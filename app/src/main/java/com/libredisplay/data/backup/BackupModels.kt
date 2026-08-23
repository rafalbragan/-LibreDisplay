package com.libredisplay.data.backup

/**
 * Normalized, version independent representation of a LibreCare backup.
 *
 * Every historic backup format (v1 plain JSON, v2 encrypted envelope, v3 automatic plain JSON)
 * is decoded into this single structure so that restore logic never has to care about the
 * on-disk format.
 */
data class BackupBundle(
    val schemaVersion: Int,
    val createdAtIso: String,
    val appVersion: String,
    val deviceLabel: String = "",
    val persons: List<BackupPersonDto> = emptyList(),
    val readings: List<BackupReadingDto> = emptyList(),
    val patientSettings: List<BackupPatientSettingsDto> = emptyList(),
    val settings: BackupSettingsDto? = null,
    val quickMetricOrder: List<String> = emptyList(),
    val quickMetricVisibility: Map<String, Boolean>? = null,
    val session: BackupSessionDto? = null
) {
    val readingsByPatient: Map<String, List<BackupReadingDto>>
        get() = readings.groupBy { it.patientId }

    companion object {
        /** Current on-disk format written by LibreCare. */
        const val CURRENT_SCHEMA_VERSION = 3

        /** Marker written into every v3 file so the format can be identified quickly. */
        const val FORMAT_MARKER = "librecare-backup"
    }
}

data class BackupPersonDto(
    val patientId: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val displayName: String,
    val connectionId: String? = null,
    val isActive: Boolean = true,
    val lastSeenAtIso: String,
    val createdAtIso: String,
    val updatedAtIso: String
)

data class BackupReadingDto(
    val id: String,
    val patientId: String,
    val timestampIso: String,
    val valueMgDl: Int,
    val trendArrow: String? = null,
    val trendLabel: String? = null,
    val source: String = "LibreLinkUp",
    val sourceAccountId: String? = null,
    val receivedAtIso: String,
    val isValid: Boolean = true,
    val rawTrendCode: String? = null,
    val createdAtIso: String
)

data class BackupPatientSettingsDto(
    val patientId: String,
    val lowCriticalMgDl: Int,
    val lowMgDl: Int,
    val targetLowMgDl: Int,
    val targetHighMgDl: Int,
    val highMgDl: Int,
    val hba1cTargetPercent: Double,
    val labHba1cPercent: Double? = null,
    val labHba1cDateIso: String? = null,
    val updatedAtIso: String
)

data class BackupSettingsDto(
    val email: String = "",
    val password: String = "",
    val selectedPatientId: String? = null,
    val region: String = "EU",
    val regionMode: String = "EU",
    val customBaseUrl: String = "",
    val refreshInterval: Int = 60,
    val targetLow: Int = 80,
    val targetHigh: Int = 180,
    val trendWindowMinutes: Int = 3,
    val showStatistics: Boolean = true,
    val kioskMode: Boolean = false,
    val appMode: String = "NONE",
    val useAuthV3: Boolean = true,
    val retentionHours: Int = 24 * 30 * 24,
    val backgroundPollingMinutes: Int = 60
)

data class BackupSessionDto(
    val token: String,
    val userId: String,
    val accountIdHash: String,
    val region: String,
    val baseUrl: String,
    val tokenExpiresAtEpochSeconds: Long? = null
)

/** Thrown for every unrecoverable backup problem, always carrying a Polish user facing message. */
class BackupFormatException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

