package com.libredisplay.data.local

import android.app.Application
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DatabaseStartupTest {

    @Test
    fun roomDatabase_opensAndObservedPersonDaoQueryDoesNotCrash() = runBlocking {
        val db = InMemoryRoomTestDatabase.create()
        try {
            val now = Instant.parse("2026-08-17T09:00:00Z")
            db.observedPersonDao().upsertAll(
                listOf(
                    ObservedPersonEntity(
                        patientId = "patient-a",
                        firstName = "Anna",
                        lastName = "Nowak",
                        displayName = "Anna Nowak",
                        connectionId = null,
                        isActive = true,
                        lastSeenAt = now,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            )

            val activePersons = db.observedPersonDao().getActivePersons()

            assertTrue(activePersons.isNotEmpty())
            assertTrue(activePersons.any { it.patientId == "patient-a" && it.displayName == "Anna Nowak" })
        } finally {
            db.close()
        }
    }
}



