package com.libredisplay.data.local

import android.app.Application
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MultiPersonRoomDatabaseTest {

    @Test
    fun glucoseHistory_isSeparatedByPatientId() = runBlocking {
        val db = InMemoryRoomTestDatabase.create()
        try {
            val now = Instant.parse("2026-08-17T10:00:00Z")
            val earlier = now.minusSeconds(300)

            db.observedPersonDao().upsertAll(
                listOf(
                    ObservedPersonEntity("patient-a", "Anna", "Nowak", "Anna Nowak", null, true, now, now, now),
                    ObservedPersonEntity("patient-b", "Bartek", "Kowalski", "Bartek Kowalski", null, true, now, now, now)
                )
            )

            db.glucoseReadingDao().insertIgnore(
                listOf(
                    GlucoseReadingEntity(
                        id = "patient-a:${earlier.toEpochMilli()}",
                        patientId = "patient-a",
                        timestamp = earlier,
                        valueMgDl = 110,
                        trendArrow = "→",
                        trendLabel = "Stable",
                        source = "LibreLinkUp",
                        sourceAccountId = "acct-1",
                        receivedAt = now,
                        isValid = true,
                        rawTrendCode = null,
                        createdAt = now
                    ),
                    GlucoseReadingEntity(
                        id = "patient-b:${now.toEpochMilli()}",
                        patientId = "patient-b",
                        timestamp = now,
                        valueMgDl = 165,
                        trendArrow = "↗",
                        trendLabel = "Rising",
                        source = "LibreLinkUp",
                        sourceAccountId = "acct-1",
                        receivedAt = now,
                        isValid = true,
                        rawTrendCode = null,
                        createdAt = now
                    )
                )
            )

            val from = earlier.minusSeconds(60)
            val to = now.plusSeconds(60)
            val patientAReadings = db.glucoseReadingDao().getRangeByPatient("patient-a", from, to)
            val patientBReadings = db.glucoseReadingDao().getRangeByPatient("patient-b", from, to)

            assertEquals(1, patientAReadings.size)
            assertEquals("patient-a", patientAReadings.single().patientId)
            assertEquals(110, patientAReadings.single().valueMgDl)

            assertEquals(1, patientBReadings.size)
            assertEquals("patient-b", patientBReadings.single().patientId)
            assertEquals(165, patientBReadings.single().valueMgDl)

            val activePersons = db.observedPersonDao().getActivePersons()
            assertEquals(2, activePersons.size)
            assertTrue(activePersons.any { it.patientId == "patient-a" })
            assertTrue(activePersons.any { it.patientId == "patient-b" })
        } finally {
            db.close()
        }
    }
}



