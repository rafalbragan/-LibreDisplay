package com.libredisplay.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {

    private fun sampleBundle(): BackupBundle = BackupBundle(
        schemaVersion = BackupBundle.CURRENT_SCHEMA_VERSION,
        createdAtIso = "2026-08-20T10:00:00Z",
        appVersion = "2.4.0",
        deviceLabel = "Samsung SM-S911B",
        persons = listOf(
            BackupPersonDto(
                patientId = "person-a",
                firstName = "Anna",
                lastName = "Kowalska",
                displayName = "Anna Kowalska",
                connectionId = "conn-1",
                isActive = true,
                lastSeenAtIso = "2026-08-20T10:00:00Z",
                createdAtIso = "2026-08-01T10:00:00Z",
                updatedAtIso = "2026-08-20T10:00:00Z"
            )
        ),
        readings = listOf(
            BackupReadingDto(
                id = "person-a:1",
                patientId = "person-a",
                timestampIso = "2026-08-20T10:00:00Z",
                valueMgDl = 118,
                trendArrow = "->",
                trendLabel = "Stable",
                source = "LibreLinkUp",
                sourceAccountId = "acc-1",
                receivedAtIso = "2026-08-20T10:00:05Z",
                isValid = true,
                rawTrendCode = null,
                createdAtIso = "2026-08-20T10:00:05Z"
            )
        ),
        patientSettings = listOf(
            BackupPatientSettingsDto(
                patientId = "person-a",
                lowCriticalMgDl = 54,
                lowMgDl = 70,
                targetLowMgDl = 80,
                targetHighMgDl = 180,
                highMgDl = 250,
                hba1cTargetPercent = 7.2,
                labHba1cPercent = 6.8,
                labHba1cDateIso = "2026-08-19",
                updatedAtIso = "2026-08-20T10:00:00Z"
            )
        ),
        settings = BackupSettingsDto(
            email = "user@example.com",
            password = "secret",
            selectedPatientId = "person-a",
            appMode = "LIVE",
            refreshInterval = 120,
            backgroundPollingMinutes = 30
        ),
        quickMetricOrder = listOf("below", "in_range", "above"),
        quickMetricVisibility = mapOf("below" to true, "gmi" to false),
        session = BackupSessionDto(
            token = "token-1",
            userId = "user-1",
            accountIdHash = "hash-1",
            region = "EU",
            baseUrl = "https://api-eu.libreview.io",
            tokenExpiresAtEpochSeconds = 1800000000L
        )
    )

    @Test
    fun encodeThenDecode_preservesEverything() {
        val original = sampleBundle()

        val decoded = BackupCodec.decode(BackupCodec.encode(original))

        assertEquals(BackupBundle.CURRENT_SCHEMA_VERSION, decoded.schemaVersion)
        assertEquals(original.createdAtIso, decoded.createdAtIso)
        assertEquals(original.appVersion, decoded.appVersion)
        assertEquals(original.deviceLabel, decoded.deviceLabel)
        assertEquals(original.persons, decoded.persons)
        assertEquals(original.readings, decoded.readings)
        assertEquals(original.patientSettings, decoded.patientSettings)
        assertEquals(original.settings, decoded.settings)
        assertEquals(original.quickMetricOrder, decoded.quickMetricOrder)
        assertEquals(original.quickMetricVisibility, decoded.quickMetricVisibility)
        assertEquals(original.session, decoded.session)
    }

    @Test
    fun encode_producesPlainReadableJsonWithoutPassword() {
        val text = BackupCodec.encode(sampleBundle())

        assertTrue(text.contains("\"format\": \"${BackupBundle.FORMAT_MARKER}\""))
        assertTrue(text.contains("\"schemaVersion\": 3"))
        assertTrue(text.contains("\"checksum\""))
        assertTrue(!BackupCodec.requiresPassword(text))
    }

    @Test
    fun decode_readsLegacyVersion1Backup() {
        val legacy = """
            {
              "schemaVersion": 1,
              "generatedAtEpochMillis": 1787000000000,
              "appVersion": "1.9.0",
              "settings": { "email": "a@b.c", "appMode": "LIVE", "targetLow": 90, "targetHigh": 170 },
              "quickMetricOrder": ["below", "in_range"],
              "livePersons": [
                {
                  "patientId": "legacy-person",
                  "displayName": "Legacy Person",
                  "isActive": true,
                  "lastSeenAtIso": "2026-08-20T10:00:00Z",
                  "createdAtIso": "2026-08-20T10:00:00Z",
                  "updatedAtIso": "2026-08-20T10:00:00Z"
                }
              ],
              "liveReadings": [
                {
                  "id": "legacy-person:1",
                  "patientId": "legacy-person",
                  "timestampIso": "2026-08-20T10:00:00Z",
                  "valueMgDl": 111,
                  "source": "LibreLinkUp",
                  "receivedAtIso": "2026-08-20T10:00:00Z",
                  "isValid": true,
                  "createdAtIso": "2026-08-20T10:00:00Z"
                }
              ],
              "livePatientSettings": []
            }
        """.trimIndent()

        val decoded = BackupCodec.decode(legacy)

        assertEquals(1, decoded.schemaVersion)
        assertEquals(1, decoded.persons.size)
        assertEquals("legacy-person", decoded.persons.first().patientId)
        assertEquals(111, decoded.readings.first().valueMgDl)
        assertEquals(90, decoded.settings?.targetLow)
        assertEquals(listOf("below", "in_range"), decoded.quickMetricOrder)
    }

    @Test
    fun decode_readsLegacyShortKeyBackup() {
        val legacyShortKeys = """
            {
              "a": 1,
              "b": 1787000000000,
              "c": "1.8.1",
              "h": [ { "a": "short-person", "d": "Short Person", "f": true,
                       "g": "2026-08-20T10:00:00Z", "h": "2026-08-20T10:00:00Z", "i": "2026-08-20T10:00:00Z" } ],
              "i": [ { "a": "short-person:1", "b": "short-person", "c": "2026-08-20T10:00:00Z",
                       "d": 123, "g": "LibreLinkUp", "i": "2026-08-20T10:00:00Z", "j": true,
                       "l": "2026-08-20T10:00:00Z" } ],
              "j": []
            }
        """.trimIndent()

        val decoded = BackupCodec.decode(legacyShortKeys)

        assertEquals("short-person", decoded.persons.single().patientId)
        assertEquals(123, decoded.readings.single().valueMgDl)
    }

    @Test
    fun decode_readsLegacyEncryptedBackupWithPassword() {
        val plainBody = """
            {
              "schemaVersion": 2,
              "createdAt": "2026-08-20T10:00:00Z",
              "appVersion": "2.2.3",
              "profiles": [ { "patientId": "enc-person", "displayName": "Enc Person",
                              "isActive": true, "lastSeenAtIso": "2026-08-20T10:00:00Z",
                              "createdAtIso": "2026-08-20T10:00:00Z", "updatedAtIso": "2026-08-20T10:00:00Z" } ],
              "readings": [ { "id": "enc-person:1", "patientId": "enc-person",
                              "timestampIso": "2026-08-20T10:00:00Z", "valueMgDl": 145,
                              "source": "LibreLinkUp", "receivedAtIso": "2026-08-20T10:00:00Z",
                              "isValid": true, "createdAtIso": "2026-08-20T10:00:00Z" } ],
              "settings": { "targetLow": 85, "targetHigh": 175, "appMode": "LIVE" }
            }
        """.trimIndent()
        val encrypted = BackupCodec.encryptForLegacyFormat(plainBody, "old-password-123")

        assertTrue(BackupCodec.requiresPassword(encrypted))

        val decoded = BackupCodec.decode(encrypted, "old-password-123")
        assertEquals("enc-person", decoded.persons.single().patientId)
        assertEquals(145, decoded.readings.single().valueMgDl)
        assertEquals(85, decoded.settings?.targetLow)
    }

    @Test
    fun decode_encryptedBackupWithoutPassword_reportsPolishMessage() {
        val encrypted = BackupCodec.encryptForLegacyFormat("""{"schemaVersion":2,"profiles":[],"readings":[]}""", "pw123456")

        try {
            BackupCodec.decode(encrypted)
            fail("Expected BackupFormatException")
        } catch (error: BackupFormatException) {
            assertTrue(error.message.orEmpty().contains("zaszyfrowany"))
        }
    }

    @Test
    fun decode_encryptedBackupWithWrongPassword_fails() {
        val encrypted = BackupCodec.encryptForLegacyFormat("""{"schemaVersion":2,"profiles":[],"readings":[]}""", "pw123456")

        try {
            BackupCodec.decode(encrypted, "wrong-password")
            fail("Expected BackupFormatException")
        } catch (error: BackupFormatException) {
            assertTrue(error.message.orEmpty().contains("hasło", ignoreCase = true))
        }
    }

    @Test
    fun decode_malformedJson_reportsPolishMessage() {
        try {
            BackupCodec.decode("{ this is not json")
            fail("Expected BackupFormatException")
        } catch (error: BackupFormatException) {
            assertTrue(error.message.orEmpty().contains("Nie można odczytać kopii zapasowej"))
        }
    }

    @Test
    fun decode_emptyFile_reportsPolishMessage() {
        try {
            BackupCodec.decode("   ")
            fail("Expected BackupFormatException")
        } catch (error: BackupFormatException) {
            assertTrue(error.message.orEmpty().contains("pusty"))
        }
    }

    @Test
    fun decode_rowsWithMissingFields_failWithReadableMessage() {
        try {
            BackupCodec.decode(
                """
                {
                  "format": "librecare-backup",
                  "schemaVersion": 3,
                  "createdAt": "2026-08-20T10:00:00Z",
                  "appVersion": "2.4.0",
                  "persons": [ { "displayName": "no id" },
                               { "patientId": "ok", "displayName": "OK",
                                 "lastSeenAtIso": "2026-08-20T10:00:00Z",
                                 "createdAtIso": "2026-08-20T10:00:00Z",
                                 "updatedAtIso": "2026-08-20T10:00:00Z" } ],
                  "readings": [ { "patientId": "ok" } ],
                  "patientSettings": []
                }
                """.trimIndent()
            )
            fail("Expected BackupFormatException")
        } catch (error: BackupFormatException) {
            assertTrue(error.message.orEmpty().contains("brakujące", ignoreCase = true) || error.message.orEmpty().contains("nieprawidłowe", ignoreCase = true))
        }
    }
}

