package com.togetherly.data.family

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.database.buildTogetherlyDatabase
import com.togetherly.data.local.mapper.FamilyProfileMapper
import com.togetherly.data.testFamilyProfile
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.MemoryPreferences
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.family.PrivacyPreferences
import com.togetherly.domain.family.QuestPreferences
import com.togetherly.domain.family.ReminderPreference
import com.togetherly.domain.quest.QuestCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")

@RunWith(AndroidJUnit4::class)
internal class RoomFamilySettingsRepositoryTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val databaseFile = instrumentation.targetContext.getDatabasePath("family-settings-test.db")

    private lateinit var database: TogetherlyDatabase

    @Before
    fun setUpDatabase() {
        databaseFile.delete()
        database = openDatabase()
    }

    @After
    fun tearDownDatabase() {
        database.close()
        databaseFile.delete()
    }

    private fun openDatabase(): TogetherlyDatabase =
        buildTogetherlyDatabase(Room.databaseBuilder<TogetherlyDatabase>(context = instrumentation.targetContext, name = databaseFile.path))

    private fun familyRepository(db: TogetherlyDatabase = database) = RoomFamilyRepository(
        familyDao = db.familyDao(),
        familyMapper = FamilyProfileMapper(),
        database = db,
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
    )

    private fun settingsRepository(db: TogetherlyDatabase = database) = RoomFamilySettingsRepository(
        familyDao = db.familyDao(),
        familyMapper = FamilyProfileMapper(),
        clock = TestAppClock(NOW),
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
    )

    @Test
    fun defaultSettingsForANewlySavedProfileAreSafe() = runTest {
        familyRepository().saveProfile(testFamilyProfile())

        val settings = (settingsRepository().observeSettings().first() as DataResult.Success).value
        requireNotNull(settings)
        assertEquals(MemoryPreferences.defaults(), settings.memoryPreferences)
        assertEquals(PrivacyPreferences.defaults(), settings.privacyPreferences)
    }

    @Test
    fun existingOnboardingProfileRemainsTheSourceOfTruthForFamilySettings() = runTest {
        val profile = testFamilyProfile()
        familyRepository().saveProfile(profile)

        val settings = (settingsRepository().observeSettings().first() as DataResult.Success).value
        requireNotNull(settings)
        assertEquals(profile, settings.profile)
        assertEquals(profile.interests, settings.questPreferences.interests)
        assertEquals(profile.preferredDurations, settings.questPreferences.preferredDurations)
    }

    @Test
    fun updatingQuestPreferencesIsReflectedReactively() = runTest {
        familyRepository().saveProfile(testFamilyProfile())
        val repository = settingsRepository()
        val newPreferences = QuestPreferences(
            interests = setOf(QuestCategory.SILLY),
            preferredDurations = setOf(DurationBand.THIRTY_PLUS_MINUTES),
            locationPreference = LocationPreference.INDOOR,
            preparationPreference = PreparationPreference.NONE,
        )

        repository.updateQuestPreferences(newPreferences)

        val settings = (repository.observeSettings().first() as DataResult.Success).value
        assertEquals(newPreferences, settings?.questPreferences)
    }

    @Test
    fun updatingMemoryPreferencesIsReflectedReactively() = runTest {
        familyRepository().saveProfile(testFamilyProfile())
        val repository = settingsRepository()
        val preferences = MemoryPreferences(allowPhotos = false, allowVoiceMemories = true, allowTextNotes = true, showMemoryPromptAfterQuests = false)

        repository.updateMemoryPreferences(preferences)

        val settings = (repository.observeSettings().first() as DataResult.Success).value
        assertEquals(preferences, settings?.memoryPreferences)
    }

    @Test
    fun updatingPrivacyPreferencesIsReflectedReactively() = runTest {
        familyRepository().saveProfile(testFamilyProfile())
        val repository = settingsRepository()

        repository.updatePrivacyPreferences(PrivacyPreferences(diagnosticsEnabled = true))

        val settings = (repository.observeSettings().first() as DataResult.Success).value
        assertTrue(settings?.privacyPreferences?.diagnosticsEnabled == true)
    }

    @Test
    fun updatingReminderPreferenceHandlesMissingOptionalValues() = runTest {
        familyRepository().saveProfile(testFamilyProfile())
        val repository = settingsRepository()
        repository.updateReminderPreference(ReminderPreference(setOf(DayOfWeek.MONDAY), LocalTime(9, 0)))

        repository.updateReminderPreference(null)

        val settings = (repository.observeSettings().first() as DataResult.Success).value
        assertNull(settings?.reminderPreference)
    }

    @Test
    fun updatingSettingsWithNoExistingProfileFails() = runTest {
        val repository = settingsRepository()

        val result = repository.updateMemoryPreferences(MemoryPreferences.defaults())

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.MISSING_FAMILY_PROFILE)), result)
    }

    @Test
    fun updatingTheFamilyProfileNeverResetsPreviouslySetMemoryOrPrivacyPreferences() = runTest {
        val profile = testFamilyProfile()
        val familyRepo = familyRepository()
        familyRepo.saveProfile(profile)
        val settingsRepo = settingsRepository()
        settingsRepo.updateMemoryPreferences(MemoryPreferences(allowPhotos = false, allowVoiceMemories = false, allowTextNotes = false, showMemoryPromptAfterQuests = false))
        settingsRepo.updatePrivacyPreferences(PrivacyPreferences(diagnosticsEnabled = true))

        // A whole-profile replace — e.g. UpdateFamilyProfile editing the display name — must not
        // silently reset the settings just configured above.
        familyRepo.saveProfile(profile.copy(displayName = com.togetherly.domain.family.FamilyDisplayName("New Name")))

        val settings = (settingsRepo.observeSettings().first() as DataResult.Success).value
        requireNotNull(settings)
        assertEquals(false, settings.memoryPreferences.allowPhotos)
        assertEquals(false, settings.memoryPreferences.allowVoiceMemories)
        assertEquals(false, settings.memoryPreferences.allowTextNotes)
        assertEquals(false, settings.memoryPreferences.showMemoryPromptAfterQuests)
        assertEquals(true, settings.privacyPreferences.diagnosticsEnabled)
        assertEquals("New Name", settings.profile.displayName?.value)
    }

    @Test
    fun settingsSurvivePersistenceAcrossRepositoryRecreation() = runTest {
        familyRepository().saveProfile(testFamilyProfile())
        settingsRepository().updateMemoryPreferences(MemoryPreferences(allowPhotos = false, allowVoiceMemories = true, allowTextNotes = true, showMemoryPromptAfterQuests = false))
        settingsRepository().updatePrivacyPreferences(PrivacyPreferences(diagnosticsEnabled = true))
        settingsRepository().updateReminderPreference(ReminderPreference(setOf(DayOfWeek.TUESDAY), LocalTime(18, 30)))

        // Close the database and reopen a brand-new instance against the same file — simulating a
        // fresh process/repository instance reading whatever was actually persisted to disk.
        database.close()
        database = openDatabase()

        val settings = (settingsRepository().observeSettings().first() as DataResult.Success).value
        requireNotNull(settings)
        assertEquals(false, settings.memoryPreferences.allowPhotos)
        assertEquals(true, settings.memoryPreferences.allowVoiceMemories)
        assertEquals(true, settings.memoryPreferences.allowTextNotes)
        assertEquals(false, settings.memoryPreferences.showMemoryPromptAfterQuests)
        assertEquals(true, settings.privacyPreferences.diagnosticsEnabled)
        assertEquals(ReminderPreference(setOf(DayOfWeek.TUESDAY), LocalTime(18, 30)), settings.reminderPreference)
    }
}
