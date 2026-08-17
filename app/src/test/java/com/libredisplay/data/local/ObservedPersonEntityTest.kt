package com.libredisplay.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * Tests for [ObservedPersonEntity] to verify that the entity can be constructed
 * with optional firstName/lastName (schema had these added as nullable columns).
 *
 * NOTE on full migration tests:
 * Room's MigrationTestHelper requires an Android instrumentation runner because
 * it spawns a real SQLite database.  Since there is no androidTest source-set
 * configured in this project, those tests would need to be added under
 * app/src/androidTest/... and run on a device or emulator with:
 *
 *   ./gradlew connectedDebugAndroidTest
 *
 * The migration SQL itself is validated via DatabaseVersionTest (pure JVM).
 */
class ObservedPersonEntityTest {

    private fun person(
        patientId: String = "p1",
        firstName: String? = null,
        lastName: String? = null,
        displayName: String = "Person One"
    ): ObservedPersonEntity {
        val now = Instant.ofEpochSecond(1_700_000_000L)
        return ObservedPersonEntity(
            patientId = patientId,
            firstName = firstName,
            lastName = lastName,
            displayName = displayName,
            isActive = true,
            lastSeenAt = now,
            createdAt = now,
            updatedAt = now
        )
    }

    @Test
    fun firstNameIsNullable() {
        val p = person(firstName = null)
        assertNull(p.firstName)
    }

    @Test
    fun lastNameIsNullable() {
        val p = person(lastName = null)
        assertNull(p.lastName)
    }

    @Test
    fun firstAndLastNamesCanBeSet() {
        val p = person(firstName = "Jan", lastName = "Kowalski")
        assertEquals("Jan", p.firstName)
        assertEquals("Kowalski", p.lastName)
    }

    @Test
    fun patientIdIsThePrimaryKey() {
        val p = person(patientId = "abc-123")
        assertEquals("abc-123", p.patientId)
    }

    @Test
    fun twoPersonsWithDifferentPatientIdsAreDifferent() {
        val a = person(patientId = "person-a", displayName = "Mom")
        val b = person(patientId = "person-b", displayName = "Dad")
        assert(a.patientId != b.patientId)
        assert(a.displayName != b.displayName)
    }

    @Test
    fun connectionIdDefaultsToNull() {
        val p = person()
        assertNull(p.connectionId)
    }
}

