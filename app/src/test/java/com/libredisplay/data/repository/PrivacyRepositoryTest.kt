package com.libredisplay.data.repository

import android.app.Application
import androidx.room.Room
import com.libredisplay.data.api.AuthCapableLibreLinkUpClient
import com.libredisplay.data.api.PersistedLibreLinkUpSession
import com.libredisplay.data.local.GlucoseReadingEntity
import com.libredisplay.data.local.LibreDisplayDatabase
import com.libredisplay.data.local.ObservedPersonEntity
import com.libredisplay.data.model.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PrivacyRepositoryTest {

    private lateinit var db: LibreDisplayDatabase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var localRepo: LocalGlucoseHistoryRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var privacyRepository: PrivacyRepository
    private lateinit var fakeAuthClient: FakeAuthClient

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        settingsRepository = SettingsRepository(context)
        settingsRepository.clearAll()

        db = Room.inMemoryDatabaseBuilder(context, LibreDisplayDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        localRepo = LocalGlucoseHistoryRepository(
            observedPersonDao = db.observedPersonDao(),
            glucoseReadingDao = db.glucoseReadingDao(),
            syncRunDao = db.syncRunDao()
        )

        fakeAuthClient = FakeAuthClient()
        authRepository = AuthRepository(
            settingsProvider = { settingsRepository.loadSettings() },
            client = fakeAuthClient,
            loginStateStore = SettingsLoginStateStore(settingsRepository)
        )

        privacyRepository = PrivacyRepository(
            settingsRepository = settingsRepository,
            authRepository = authRepository,
            localHistoryRepository = localRepo,
            patientSettingsDao = db.patientSettingsDao()
        )

        seedLocalData()
    }

    @After
    fun tearDown() {
        settingsRepository.clearAll()
        db.close()
    }

    @Test
    fun deleteLocalGlucoseHistory_removesReadingsOnly() = runBlocking {
        privacyRepository.deleteLocalGlucoseHistory()

        val now = Instant.parse("2026-08-17T10:00:00Z")
        val from = now.minusSeconds(60 * 60 * 24)
        val to = now.plusSeconds(60)

        assertTrue(db.glucoseReadingDao().getRangeByPatient("real-person-a", from, to).isEmpty())
        assertTrue(db.glucoseReadingDao().getRangeByPatient("demo-person-anna", from, to).isEmpty())
        assertNotNull(db.observedPersonDao().getByPatientId("real-person-a"))
    }

    @Test
    fun deleteObservedPeople_clearsPeopleAndSelection() = runBlocking {
        privacyRepository.deleteObservedPeople()

        assertTrue(db.observedPersonDao().getActivePersons().isEmpty())
        assertNull(settingsRepository.loadSettings().selectedPatientId)
    }

    @Test
    fun clearSessionData_keepsHistory() = runBlocking {
        settingsRepository.savePersistedSession(
            PersistedLibreLinkUpSession(
                token = "token",
                userId = "u1",
                accountIdHash = "a1",
                region = "EU",
                baseUrl = "https://api-eu.libreview.io"
            )
        )

        privacyRepository.clearSessionData()

        assertNull(settingsRepository.loadPersistedSession())
        val now = Instant.parse("2026-08-17T10:00:00Z")
        assertTrue(db.glucoseReadingDao().getRangeByPatient("real-person-a", now.minusSeconds(60 * 60 * 24), now).isNotEmpty())
    }

    @Test
    fun deleteMyStoredData_clearsLocalData() = runBlocking {
        privacyRepository.deleteMyStoredData()

        assertTrue(db.observedPersonDao().getActivePersons().isEmpty())
        val settings = settingsRepository.loadSettings()
        assertEquals("", settings.email)
        assertEquals("", settings.password)
    }

    @Test
    fun resetAppData_andEmptyStateOperations_doNotCrash() = runBlocking {
        privacyRepository.resetAppData()
        privacyRepository.deleteLocalGlucoseHistory()
        privacyRepository.deleteObservedPeople()
        privacyRepository.deleteDemoData()

        assertTrue(db.observedPersonDao().getActivePersons().isEmpty())
        assertNull(settingsRepository.loadSettings().selectedPatientId)
    }

    @Test
    fun deleteDemoData_doesNotAffectRealData() = runBlocking {
        privacyRepository.deleteDemoData()

        val now = Instant.parse("2026-08-17T10:00:00Z")
        val from = now.minusSeconds(60 * 60 * 24)
        val to = now.plusSeconds(60)

        assertTrue(db.glucoseReadingDao().getRangeByPatient("demo-person-anna", from, to).isEmpty())
        assertTrue(db.observedPersonDao().getByPatientId("demo-person-anna") == null)
        assertTrue(db.glucoseReadingDao().getRangeByPatient("real-person-a", from, to).isNotEmpty())
        assertNotNull(db.observedPersonDao().getByPatientId("real-person-a"))
    }

    private fun seedLocalData() = runBlocking {
        val now = Instant.parse("2026-08-17T10:00:00Z")

        db.observedPersonDao().upsertAll(
            listOf(
                ObservedPersonEntity(
                    patientId = "real-person-a",
                    firstName = "Real",
                    lastName = "User",
                    displayName = "Real User",
                    lastSeenAt = now,
                    createdAt = now,
                    updatedAt = now
                ),
                ObservedPersonEntity(
                    patientId = "demo-person-anna",
                    firstName = "Anna",
                    lastName = "Kowalska",
                    displayName = "Anna Kowalska",
                    lastSeenAt = now,
                    createdAt = now,
                    updatedAt = now
                )
            )
        )

        db.glucoseReadingDao().insertIgnore(
            listOf(
                GlucoseReadingEntity(
                    id = "real-person-a:${now.toEpochMilli()}",
                    patientId = "real-person-a",
                    timestamp = now,
                    valueMgDl = 118,
                    trendArrow = "→",
                    trendLabel = "Stable",
                    source = "LibreLinkUp",
                    sourceAccountId = "acc-1",
                    receivedAt = now,
                    isValid = true,
                    rawTrendCode = null,
                    createdAt = now
                ),
                GlucoseReadingEntity(
                    id = "demo-person-anna:${now.toEpochMilli()}",
                    patientId = "demo-person-anna",
                    timestamp = now,
                    valueMgDl = 129,
                    trendArrow = "↗",
                    trendLabel = "Rising",
                    source = "DemoMode",
                    sourceAccountId = null,
                    receivedAt = now,
                    isValid = true,
                    rawTrendCode = null,
                    createdAt = now
                )
            )
        )

        settingsRepository.saveSettings(
            AppSettings(
                email = "user@example.com",
                password = "secret",
                selectedPatientId = "real-person-a"
            )
        )
    }

    private class FakeAuthClient : AuthCapableLibreLinkUpClient {
        private var active = false

        override suspend fun login(email: String, password: String, region: String) {
            active = true
        }

        override fun hasActiveSession(): Boolean = active

        override fun clearSession() {
            active = false
        }

        override fun exportSession(): PersistedLibreLinkUpSession? = null

        override fun importSession(session: PersistedLibreLinkUpSession) {
            active = true
        }
    }
}

