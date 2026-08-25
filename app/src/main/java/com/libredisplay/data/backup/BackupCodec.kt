package com.libredisplay.data.backup

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Reads and writes every LibreCare backup format.
 *
 * Pure JVM code (no Android dependency) so it can be covered by fast unit tests.
 *
 * Supported input formats:
 *  - v3  plain JSON, no password (current automatic backup / export file)
 *  - v2  AES-256-GCM encrypted envelope (`cipherText`) - requires the original password
 *  - v2b plain JSON body of the v2 format (`profiles` + `readings`)
 *  - v1  legacy plain JSON (`livePersons` / `liveReadings` / `livePatientSettings`),
 *        including the short-key variant (`h` / `i` / `j`)
 *
 * Output format is always v3 so that the file can be restored without any secret.
 */
object BackupCodec {

    private val gsonPretty = GsonBuilder().setPrettyPrinting().serializeNulls().create()

    private const val LEGACY_SECURE_SCHEMA_VERSION = 2
    private const val PBKDF2_ITERATIONS = 120_000
    private const val AES_KEY_BITS = 256

    // ------------------------------------------------------------------ encode

    fun encode(bundle: BackupBundle): String {
        val root = JsonObject()
        root.addProperty("format", BackupBundle.FORMAT_MARKER)
        root.addProperty("schemaVersion", BackupBundle.CURRENT_SCHEMA_VERSION)
        root.addProperty("createdAt", bundle.createdAtIso)
        root.addProperty("appVersion", bundle.appVersion)
        root.addProperty("deviceLabel", bundle.deviceLabel)

        val persons = JsonArray()
        bundle.persons.forEach { person ->
            persons.add(
                JsonObject().apply {
                    addProperty("patientId", person.patientId)
                    addProperty("firstName", person.firstName)
                    addProperty("lastName", person.lastName)
                    addProperty("displayName", person.displayName)
                    addProperty("connectionId", person.connectionId)
                    addProperty("isActive", person.isActive)
                    addProperty("lastSeenAtIso", person.lastSeenAtIso)
                    addProperty("createdAtIso", person.createdAtIso)
                    addProperty("updatedAtIso", person.updatedAtIso)
                }
            )
        }
        root.add("persons", persons)

        val readings = JsonArray()
        bundle.readings.forEach { reading ->
            readings.add(
                JsonObject().apply {
                    addProperty("id", reading.id)
                    addProperty("patientId", reading.patientId)
                    addProperty("timestampIso", reading.timestampIso)
                    addProperty("valueMgDl", reading.valueMgDl)
                    addProperty("trendArrow", reading.trendArrow)
                    addProperty("trendLabel", reading.trendLabel)
                    addProperty("source", reading.source)
                    addProperty("sourceAccountId", reading.sourceAccountId)
                    addProperty("receivedAtIso", reading.receivedAtIso)
                    addProperty("isValid", reading.isValid)
                    addProperty("rawTrendCode", reading.rawTrendCode)
                    addProperty("createdAtIso", reading.createdAtIso)
                }
            )
        }
        root.add("readings", readings)

        val patientSettings = JsonArray()
        bundle.patientSettings.forEach { settings ->
            patientSettings.add(
                JsonObject().apply {
                    addProperty("patientId", settings.patientId)
                    addProperty("lowCriticalMgDl", settings.lowCriticalMgDl)
                    addProperty("lowMgDl", settings.lowMgDl)
                    addProperty("targetLowMgDl", settings.targetLowMgDl)
                    addProperty("targetHighMgDl", settings.targetHighMgDl)
                    addProperty("highMgDl", settings.highMgDl)
                    addProperty("hba1cTargetPercent", settings.hba1cTargetPercent)
                    addProperty("labHba1cPercent", settings.labHba1cPercent)
                    addProperty("labHba1cDateIso", settings.labHba1cDateIso)
                    addProperty("updatedAtIso", settings.updatedAtIso)
                }
            )
        }
        root.add("patientSettings", patientSettings)

        bundle.settings?.let { settings ->
            root.add(
                "settings",
                JsonObject().apply {
                    addProperty("email", settings.email)
                    addProperty("password", settings.password)
                    addProperty("selectedPatientId", settings.selectedPatientId)
                    addProperty("region", settings.region)
                    addProperty("regionMode", settings.regionMode)
                    addProperty("customBaseUrl", settings.customBaseUrl)
                    addProperty("refreshInterval", settings.refreshInterval)
                    addProperty("targetLow", settings.targetLow)
                    addProperty("targetHigh", settings.targetHigh)
                    addProperty("trendWindowMinutes", settings.trendWindowMinutes)
                    addProperty("showStatistics", settings.showStatistics)
                    addProperty("kioskMode", settings.kioskMode)
                    addProperty("appMode", settings.appMode)
                    addProperty("useAuthV3", settings.useAuthV3)
                    addProperty("retentionHours", settings.retentionHours)
                    addProperty("backgroundPollingMinutes", settings.backgroundPollingMinutes)
                }
            )
        }

        val order = JsonArray()
        bundle.quickMetricOrder.forEach(order::add)
        root.add("quickMetricOrder", order)

        bundle.quickMetricVisibility?.let { visibility ->
            root.add(
                "quickMetricVisibility",
                JsonObject().apply { visibility.forEach { (key, value) -> addProperty(key, value) } }
            )
        }

        bundle.session?.let { session ->
            root.add(
                "session",
                JsonObject().apply {
                    addProperty("token", session.token)
                    addProperty("userId", session.userId)
                    addProperty("accountIdHash", session.accountIdHash)
                    addProperty("region", session.region)
                    addProperty("baseUrl", session.baseUrl)
                    addProperty("tokenExpiresAtEpochSeconds", session.tokenExpiresAtEpochSeconds)
                }
            )
        }

        root.addProperty("checksum", checksumOf(root))
        return gsonPretty.toJson(root)
    }

    // ------------------------------------------------------------------ decode

    /**
     * Decodes any supported backup text.
     *
     * @param password only required for legacy v2 encrypted files.
     */
    fun decode(text: String, password: String? = null): BackupBundle {
        if (text.isBlank()) {
            throw BackupFormatException("Nie można odczytać kopii zapasowej. Plik jest pusty.")
        }
        val root = runCatching { JsonParser.parseString(text) }
            .getOrElse {
                throw BackupFormatException(
                    "Nie można odczytać kopii zapasowej. Plik ma nieprawidłowy format JSON.",
                    it
                )
            }
        if (!root.isJsonObject) {
            throw BackupFormatException("Nie można odczytać kopii zapasowej. Plik ma nieprawidłową strukturę.")
        }
        val obj = root.asJsonObject

        return when {
            obj.has("cipherText") -> decodeEncrypted(obj, password)
            obj.has("persons") || obj.optInt("schemaVersion", 0) >= 3 -> decodeV3(obj)
            obj.has("profiles") -> decodeV2Body(obj)
            obj.has("livePersons") || obj.has("h") -> decodeV1(obj)
            else -> throw BackupFormatException(
                "Nie można odczytać kopii zapasowej. Nieznany format pliku."
            )
        }
    }

    /** True when the given text requires a password before it can be decoded. */
    fun requiresPassword(text: String): Boolean = runCatching {
        JsonParser.parseString(text).asJsonObject.has("cipherText")
    }.getOrDefault(false)

    private fun decodeV3(obj: JsonObject): BackupBundle {
        val schemaVersion = obj.optInt("schemaVersion", BackupBundle.CURRENT_SCHEMA_VERSION)
        if (schemaVersion > BackupBundle.CURRENT_SCHEMA_VERSION) {
            throw BackupFormatException(
                "Nie można odczytać kopii zapasowej. Wersja schematu $schemaVersion nie jest obsługiwana przez tę wersję LibreCare."
            )
        }
        val rawPersons = obj.optArray("persons")
        val rawReadings = obj.optArray("readings")
        val rawPatientSettings = obj.optArray("patientSettings")
        val persons = rawPersons.mapNotNull { it.asObjectOrNull()?.toPerson() }
        val readings = rawReadings.mapNotNull { it.asObjectOrNull()?.toReading() }
        val patientSettings = rawPatientSettings.mapNotNull { it.asObjectOrNull()?.toPatientSettings() }
        validateDecodedCollection(section = "persons", rawSize = rawPersons.size, decodedSize = persons.size)
        validateDecodedCollection(section = "readings", rawSize = rawReadings.size, decodedSize = readings.size)
        validateDecodedCollection(section = "patientSettings", rawSize = rawPatientSettings.size, decodedSize = patientSettings.size)
        return BackupBundle(
            schemaVersion = schemaVersion,
            createdAtIso = obj.optStringOr("createdAt", ""),
            appVersion = obj.optStringOr("appVersion", "unknown"),
            deviceLabel = obj.optStringOr("deviceLabel", ""),
            persons = persons,
            readings = readings,
            patientSettings = patientSettings,
            settings = obj.optObject("settings")?.toSettings(),
            quickMetricOrder = obj.optArray("quickMetricOrder").mapNotNull { it.asStringOrNull() },
            quickMetricVisibility = obj.optObject("quickMetricVisibility")?.toBooleanMap(),
            session = obj.optObject("session")?.toSession()
        )
    }

    private fun validateDecodedCollection(section: String, rawSize: Int, decodedSize: Int) {
        if (rawSize > 0 && decodedSize != rawSize) {
            throw BackupFormatException(
                "Nie można odczytać kopii zapasowej. Sekcja \"$section\" zawiera brakujące lub nieprawidłowe pola."
            )
        }
    }

    private fun decodeV2Body(obj: JsonObject): BackupBundle {
        val persons = obj.optArray("profiles").mapNotNull { it.asObjectOrNull()?.toPerson() }
        val readings = obj.optArray("readings").mapNotNull { it.asObjectOrNull()?.toReading() }
        return BackupBundle(
            schemaVersion = obj.optInt("schemaVersion", LEGACY_SECURE_SCHEMA_VERSION),
            createdAtIso = obj.optStringOr("createdAt", ""),
            appVersion = obj.optStringOr("appVersion", "unknown"),
            persons = persons,
            readings = readings,
            patientSettings = emptyList(),
            settings = obj.optObject("settings")?.toSettings(),
            quickMetricOrder = emptyList(),
            quickMetricVisibility = null,
            session = null
        )
    }

    private fun decodeV1(obj: JsonObject): BackupBundle {
        val persons = obj.optArrayAny("livePersons", "h").mapNotNull { it.asObjectOrNull()?.toPerson() }
        val readings = obj.optArrayAny("liveReadings", "i").mapNotNull { it.asObjectOrNull()?.toReading() }
        val patientSettings = obj.optArrayAny("livePatientSettings", "j")
            .mapNotNull { it.asObjectOrNull()?.toPatientSettings() }
        val settingsObject = obj.optObject("settings") ?: obj.optObject("d")
        val generatedAt = obj.optLongAny("generatedAtEpochMillis", "b")
        return BackupBundle(
            schemaVersion = 1,
            createdAtIso = generatedAt?.let { java.time.Instant.ofEpochMilli(it).toString() } ?: "",
            appVersion = obj.optString("appVersion", null) ?: obj.optStringOr("c", "unknown"),
            persons = persons,
            readings = readings,
            patientSettings = patientSettings,
            settings = settingsObject?.toSettings(),
            quickMetricOrder = obj.optArrayAny("quickMetricOrder", "e").mapNotNull { it.asStringOrNull() },
            quickMetricVisibility = (obj.optObject("quickMetricVisibility") ?: obj.optObject("f"))?.toBooleanMap(),
            session = (obj.optObject("persistedSession") ?: obj.optObject("g"))?.toSession()
        )
    }

    private fun decodeEncrypted(obj: JsonObject, password: String?): BackupBundle {
        if (password.isNullOrBlank()) {
            throw BackupFormatException(
                "Ten plik pochodzi ze starszej wersji LibreCare i jest zaszyfrowany. Podaj hasło użyte przy jego tworzeniu."
            )
        }
        val plain = runCatching {
            val salt = Base64.getDecoder().decode(obj.optStringOr("salt", ""))
            val iv = Base64.getDecoder().decode(obj.optStringOr("iv", ""))
            val cipherText = Base64.getDecoder().decode(obj.optStringOr("cipherText", ""))
            val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_BITS)
            val key = SecretKeySpec(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
                "AES"
            )
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }.getOrElse {
            throw BackupFormatException("Niepoprawne hasło albo uszkodzony plik kopii.", it)
        }
        return decode(plain, password = null)
    }

    // ------------------------------------------------------- legacy encryption (export only for tests)

    internal fun encryptForLegacyFormat(plainJson: String, password: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_BITS)
        val key = SecretKeySpec(
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
            "AES"
        )
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
        val envelope = JsonObject().apply {
            addProperty("schemaVersion", LEGACY_SECURE_SCHEMA_VERSION)
            addProperty("salt", Base64.getEncoder().encodeToString(salt))
            addProperty("iv", Base64.getEncoder().encodeToString(iv))
            addProperty("cipherText", Base64.getEncoder().encodeToString(encrypted))
        }
        return gsonPretty.toJson(envelope)
    }

    // ------------------------------------------------------------------ helpers

    private fun checksumOf(root: JsonObject): String {
        val copy = root.deepCopy()
        copy.remove("checksum")
        val digest = MessageDigest.getInstance("SHA-256").digest(copy.toString().toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    private fun JsonObject.toPerson(): BackupPersonDto? {
        val patientId = firstString("patientId", "a") ?: return null
        val displayName = firstString("displayName", "d") ?: patientId
        val fallbackTimestamp = firstString("lastSeenAtIso", "g")
            ?: firstString("createdAtIso", "h")
            ?: java.time.Instant.EPOCH.toString()
        return BackupPersonDto(
            patientId = patientId,
            firstName = firstString("firstName", "b"),
            lastName = firstString("lastName", "c"),
            displayName = displayName,
            connectionId = firstString("connectionId", "e"),
            isActive = firstBoolean("isActive", "f") ?: true,
            lastSeenAtIso = firstString("lastSeenAtIso", "g") ?: fallbackTimestamp,
            createdAtIso = firstString("createdAtIso", "h") ?: fallbackTimestamp,
            updatedAtIso = firstString("updatedAtIso", "i") ?: fallbackTimestamp
        )
    }

    private fun JsonObject.toReading(): BackupReadingDto? {
        val patientId = firstString("patientId", "b") ?: return null
        val timestamp = firstString("timestampIso", "c") ?: return null
        val value = firstInt("valueMgDl", "d") ?: return null
        val id = firstString("id", "a") ?: "$patientId:$timestamp"
        return BackupReadingDto(
            id = id,
            patientId = patientId,
            timestampIso = timestamp,
            valueMgDl = value,
            trendArrow = firstString("trendArrow", "e"),
            trendLabel = firstString("trendLabel", "f"),
            source = firstString("source", "g") ?: "LibreLinkUp",
            sourceAccountId = firstString("sourceAccountId", "h"),
            receivedAtIso = firstString("receivedAtIso", "i") ?: timestamp,
            isValid = firstBoolean("isValid", "j") ?: true,
            rawTrendCode = firstString("rawTrendCode", "k"),
            createdAtIso = firstString("createdAtIso", "l") ?: timestamp
        )
    }

    private fun JsonObject.toPatientSettings(): BackupPatientSettingsDto? {
        val patientId = firstString("patientId", "a") ?: return null
        return BackupPatientSettingsDto(
            patientId = patientId,
            lowCriticalMgDl = firstInt("lowCriticalMgDl", "b") ?: 54,
            lowMgDl = firstInt("lowMgDl", "c") ?: 70,
            targetLowMgDl = firstInt("targetLowMgDl", "d") ?: 80,
            targetHighMgDl = firstInt("targetHighMgDl", "e") ?: 180,
            highMgDl = firstInt("highMgDl", "f") ?: 250,
            hba1cTargetPercent = firstDouble("hba1cTargetPercent", "g") ?: 7.0,
            labHba1cPercent = firstDouble("labHba1cPercent", "h"),
            labHba1cDateIso = firstString("labHba1cDateIso", "i"),
            updatedAtIso = firstString("updatedAtIso", "j") ?: java.time.Instant.EPOCH.toString()
        )
    }

    private fun JsonObject.toSettings(): BackupSettingsDto = BackupSettingsDto(
        email = optStringOr("email", ""),
        password = optStringOr("password", ""),
        selectedPatientId = optString("selectedPatientId", null),
        region = optStringOr("region", "EU"),
        regionMode = optStringOr("regionMode", "EU"),
        customBaseUrl = optStringOr("customBaseUrl", ""),
        refreshInterval = optInt("refreshInterval", 60),
        targetLow = optInt("targetLow", 80),
        targetHigh = optInt("targetHigh", 180),
        trendWindowMinutes = optInt("trendWindowMinutes", 3),
        showStatistics = optBoolean("showStatistics", true),
        kioskMode = optBoolean("kioskMode", false),
        appMode = optStringOr("appMode", "NONE"),
        useAuthV3 = optBoolean("useAuthV3", true),
        retentionHours = optInt("retentionHours", 24 * 30 * 24),
        backgroundPollingMinutes = optInt("backgroundPollingMinutes", 60)
    )

    private fun JsonObject.toSession(): BackupSessionDto? {
        val token = optString("token", null) ?: return null
        return BackupSessionDto(
            token = token,
            userId = optStringOr("userId", ""),
            accountIdHash = optStringOr("accountIdHash", ""),
            region = optStringOr("region", "EU"),
            baseUrl = optStringOr("baseUrl", ""),
            tokenExpiresAtEpochSeconds = optLong("tokenExpiresAtEpochSeconds")
        )
    }

    private fun JsonObject.toBooleanMap(): Map<String, Boolean> =
        entrySet().mapNotNull { (key, value) ->
            runCatching { key to value.asBoolean }.getOrNull()
        }.toMap()

    private fun JsonElement.asObjectOrNull(): JsonObject? = if (isJsonObject) asJsonObject else null

    private fun JsonElement.asStringOrNull(): String? =
        runCatching { if (isJsonPrimitive) asString else null }.getOrNull()

    private fun JsonObject.optArray(name: String): List<JsonElement> {
        val element = get(name) ?: return emptyList()
        return if (element.isJsonArray) element.asJsonArray.toList() else emptyList()
    }

    private fun JsonObject.optArrayAny(vararg names: String): List<JsonElement> {
        names.forEach { name ->
            val values = optArray(name)
            if (values.isNotEmpty()) return values
        }
        return emptyList()
    }

    private fun JsonObject.optObject(name: String): JsonObject? {
        val element = get(name) ?: return null
        return if (element.isJsonObject) element.asJsonObject else null
    }

    private fun JsonObject.optString(name: String, fallback: String?): String? {
        val element = get(name) ?: return fallback
        if (element.isJsonNull) return fallback
        return runCatching { element.asString }.getOrDefault(fallback)
    }

    private fun JsonObject.optStringOr(name: String, fallback: String): String =
        optString(name, fallback) ?: fallback

    private fun JsonObject.optInt(name: String, fallback: Int): Int {
        val element = get(name) ?: return fallback
        if (element.isJsonNull) return fallback
        return runCatching { element.asInt }.getOrDefault(fallback)
    }

    private fun JsonObject.optLong(name: String): Long? {
        val element = get(name) ?: return null
        if (element.isJsonNull) return null
        return runCatching { element.asLong }.getOrNull()
    }

    private fun JsonObject.optLongAny(vararg names: String): Long? {
        names.forEach { name -> optLong(name)?.let { return it } }
        return null
    }

    private fun JsonObject.optBoolean(name: String, fallback: Boolean): Boolean {
        val element = get(name) ?: return fallback
        if (element.isJsonNull) return fallback
        return runCatching { element.asBoolean }.getOrDefault(fallback)
    }

    private fun JsonObject.firstString(vararg names: String): String? {
        names.forEach { name -> optString(name, null)?.let { return it } }
        return null
    }

    private fun JsonObject.firstInt(vararg names: String): Int? {
        names.forEach { name ->
            val element = get(name) ?: return@forEach
            if (element.isJsonNull) return@forEach
            runCatching { element.asInt }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun JsonObject.firstDouble(vararg names: String): Double? {
        names.forEach { name ->
            val element = get(name) ?: return@forEach
            if (element.isJsonNull) return@forEach
            runCatching { element.asDouble }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun JsonObject.firstBoolean(vararg names: String): Boolean? {
        names.forEach { name ->
            val element = get(name) ?: return@forEach
            if (element.isJsonNull) return@forEach
            runCatching { element.asBoolean }.getOrNull()?.let { return it }
        }
        return null
    }
}







