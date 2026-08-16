package com.togetherly.data.family

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.database.buildTogetherlyDatabase
import com.togetherly.data.local.mapper.FamilyProfileMapper
import com.togetherly.data.testFamilyProfile
import com.togetherly.domain.family.FamilyDisplayName
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.ReminderPreference
import com.togetherly.domain.family.repository.FamilyRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Uses a real, file-based database (not in-memory) — the whole point of the "survives recreation"
 * test is to simulate the app process dying and restarting, which an in-memory database cannot do
 * since it never persists past the connection that created it. Each test uses a unique database
 * file name, deleted in [tearDown], so tests never depend on execution order or on each other.
 */
@RunWith(AndroidJUnit4::class)
internal class FamilyRepositoryIntegrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "family-integration-${java.util.UUID.randomUUID()}.db"

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    private fun openDatabase(): TogetherlyDatabase =
        buildTogetherlyDatabase(Room.databaseBuilder<TogetherlyDatabase>(context = context, name = databaseName))

    private fun repositoryFor(database: TogetherlyDatabase): FamilyRepository = RoomFamilyRepository(
        familyDao = database.familyDao(),
        familyMapper = FamilyProfileMapper(),
        database = database,
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
        diagnostics = FakeOperationalDiagnostics(),
    )

    @Test
    fun createUpdateAndDeleteFamilyProfile() = runTest {
        val database = openDatabase()
        try {
            val repository = repositoryFor(database)

            val created = testFamilyProfile(displayName = FamilyDisplayName("Created"))
            repository.saveProfile(created)
            assertEquals(DataResult.Success(created), repository.getProfile())

            val updated = testFamilyProfile(displayName = FamilyDisplayName("Updated"))
            repository.saveProfile(updated)
            assertEquals(DataResult.Success(updated), repository.getProfile())

            repository.deleteProfile()
            assertEquals(DataResult.Success(null), repository.getProfile())
        } finally {
            database.close()
        }
    }

    @Test
    fun completeFamilyPreferencesSurviveProcessStyleRecreation() = runTest {
        val profile = testFamilyProfile(
            reminderPreference = ReminderPreference(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), LocalTime(9, 0)),
        )

        val firstProcessDatabase = openDatabase()
        repositoryFor(firstProcessDatabase).saveProfile(profile)
        firstProcessDatabase.close()

        val secondProcessDatabase = openDatabase()
        try {
            assertEquals(DataResult.Success(profile), repositoryFor(secondProcessDatabase).getProfile())
        } finally {
            secondProcessDatabase.close()
        }
    }

    @Test
    fun observeProfileEmitsUpdatesAsTheProfileChanges() = runTest {
        val database = openDatabase()
        try {
            val repository = repositoryFor(database)

            repository.observeProfile().test {
                assertEquals(DataResult.Success(null), awaitItem())

                val profile = testFamilyProfile()
                repository.saveProfile(profile)
                assertEquals(DataResult.Success(profile), awaitItem())

                repository.deleteProfile()
                assertEquals(DataResult.Success(null), awaitItem())
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun repositoryReturnsDomainModelsOnly() = runTest {
        val database = openDatabase()
        try {
            val repository: FamilyRepository = repositoryFor(database)
            repository.saveProfile(testFamilyProfile())

            assertIs<DataResult.Success<FamilyProfile?>>(repository.getProfile())
        } finally {
            database.close()
        }
    }
}
