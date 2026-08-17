package com.libredisplay.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * Unit-level contract tests for [GlucoseReadingEntity].
 *
 * Verify that:
 * - sourceAccountId is nullable (schema compatibility with pre-migration rows)
 * - per-person keying works (id = "patientId:epochMillis")
 * - two readings for different persons are never equal
 */
class GlucoseReadingEntityTest {

    private fun reading(
        patientId: String = "patient-a",
        epochMillis: Long = 1_700_000_000_000L,
        value: Int = 120,
        sourceAccountId: String? = null
    ): GlucoseReadingEntity {
        val ts = Instant.ofEpochMilli(epochMillis)
        return GlucoseReadingEntity(
            id = "$patientId:$epochMillis",
            patientId = patientId,
            timestamp = ts,
            valueMgDl = value,
            trendArrow = "→",
            trendLabel = "Stable",
            source = "LibreLinkUp",
            sourceAccountId = sourceAccountId,
            receivedAt = ts,
            isValid = true,
            rawTrendCode = null,
            createdAt = ts
        )
    }

    @Test
    fun idIsCompositeOfPatientIdAndEpochMillis() {
        val r = reading(patientId = "abc", epochMillis = 12345L)
        assertEquals("abc:12345", r.id)
    }

    @Test
    fun sourceAccountIdIsNullableAndDefaultsToNull() {
        val r = reading()
        assertNull("sourceAccountId should be null when not provided", r.sourceAccountId)
    }

    @Test
    fun sourceAccountIdCanBeSet() {
        val r = reading(sourceAccountId = "hash-xyz")
        assertNotNull(r.sourceAccountId)
        assertEquals("hash-xyz", r.sourceAccountId)
    }

    @Test
    fun readingsForDifferentPersonsAreNotEqual() {
        val a = reading(patientId = "patient-a", epochMillis = 1_000L)
        val b = reading(patientId = "patient-b", epochMillis = 1_000L)
        // Different patientIds → different composite ids
        assertEquals("patient-a:1000", a.id)
        assertEquals("patient-b:1000", b.id)
        assert(a.id != b.id) { "Readings for different persons must have different ids" }
        assert(a.patientId != b.patientId) { "patientId must differ" }
    }

    @Test
    fun readingsForSamePersonAndSameTimestampAreEqual() {
        val r1 = reading(patientId = "patient-a", epochMillis = 5_000L)
        val r2 = reading(patientId = "patient-a", epochMillis = 5_000L)
        assertEquals(r1.id, r2.id)
        assertEquals(r1.patientId, r2.patientId)
        assertEquals(r1.timestamp, r2.timestamp)
    }
}

