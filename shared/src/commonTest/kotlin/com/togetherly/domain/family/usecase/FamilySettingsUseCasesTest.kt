package com.togetherly.domain.family.usecase

import app.cash.turbine.test
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.MemoryPreferences
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.family.PrivacyPreferences
import com.togetherly.domain.family.ReminderPreference
import com.togetherly.domain.family.repository.FakeFamilySettingsRepository
import com.togetherly.domain.family.testFamilySettings
import com.togetherly.domain.family.testQuestPreferences
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.integration.testFamilyProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class FamilySettingsUseCasesTest {

    @Test
    fun observeFamilySettingsForwardsTheRepositoryFlow() = runTest {
        val repository = FakeFamilySettingsRepository()
        val settings = testFamilySettings(profile = testFamilyProfile())
        repository.setSettings(settings)
        val useCase = ObserveFamilySettings(repository)

        useCase().test {
            assertEquals(DataResult.Success(settings), awaitItem())
        }
    }

    @Test
    fun observeFamilySettingsEmitsAgainAfterAnUpdate() = runTest {
        val repository = FakeFamilySettingsRepository()
        val settings = testFamilySettings(profile = testFamilyProfile())
        repository.setSettings(settings)
        val observe = ObserveFamilySettings(repository)
        val newPreferences = testQuestPreferences(interests = setOf(QuestCategory.SILLY))

        observe().test {
            assertEquals(settings.questPreferences, (awaitItem() as DataResult.Success).value?.questPreferences)
            UpdateQuestPreferences(repository)(newPreferences)
            assertEquals(newPreferences, (awaitItem() as DataResult.Success).value?.questPreferences)
        }
    }

    @Test
    fun updateQuestPreferencesDelegatesToTheRepository() = runTest {
        val repository = FakeFamilySettingsRepository()
        repository.setSettings(testFamilySettings(profile = testFamilyProfile()))
        val useCase = UpdateQuestPreferences(repository)
        val preferences = testQuestPreferences(
            interests = setOf(QuestCategory.DISCOVER),
            preferredDurations = setOf(DurationBand.TWENTY_MINUTES),
            locationPreference = LocationPreference.OUTDOOR,
            preparationPreference = PreparationPreference.NONE,
        )

        val result = useCase(preferences)

        assertEquals(DataResult.Success(Unit), result)
        val stored = (repository.observeSettings().first() as DataResult.Success).value
        assertEquals(preferences, stored?.questPreferences)
    }

    @Test
    fun updateQuestPreferencesWithNoExistingProfileFails() = runTest {
        val repository = FakeFamilySettingsRepository()
        val useCase = UpdateQuestPreferences(repository)

        val result = useCase(testQuestPreferences())

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.MISSING_FAMILY_PROFILE)), result)
    }

    @Test
    fun updateReminderPreferenceDelegatesToTheRepository() = runTest {
        val repository = FakeFamilySettingsRepository()
        repository.setSettings(testFamilySettings(profile = testFamilyProfile()))
        val useCase = UpdateReminderPreference(repository)
        val preference = ReminderPreference(setOf(DayOfWeek.MONDAY), LocalTime(9, 0))

        val result = useCase(preference)

        assertEquals(DataResult.Success(Unit), result)
        val stored = (repository.observeSettings().first() as DataResult.Success).value
        assertEquals(preference, stored?.reminderPreference)
    }

    @Test
    fun updateReminderPreferenceCanClearReminders() = runTest {
        val repository = FakeFamilySettingsRepository()
        repository.setSettings(
            testFamilySettings(profile = testFamilyProfile(), reminderPreference = ReminderPreference(setOf(DayOfWeek.MONDAY), LocalTime(9, 0))),
        )
        val useCase = UpdateReminderPreference(repository)

        useCase(null)

        val stored = (repository.observeSettings().first() as DataResult.Success).value
        assertEquals(null, stored?.reminderPreference)
    }

    @Test
    fun updateMemoryPreferencesDelegatesToTheRepository() = runTest {
        val repository = FakeFamilySettingsRepository()
        repository.setSettings(testFamilySettings(profile = testFamilyProfile()))
        val useCase = UpdateMemoryPreferences(repository)
        val preferences = MemoryPreferences(allowPhotos = false, allowVoiceMemories = false, allowTextNotes = true, showMemoryPromptAfterQuests = false)

        val result = useCase(preferences)

        assertEquals(DataResult.Success(Unit), result)
        val stored = (repository.observeSettings().first() as DataResult.Success).value
        assertEquals(preferences, stored?.memoryPreferences)
    }

    @Test
    fun updatePrivacyPreferencesDelegatesToTheRepository() = runTest {
        val repository = FakeFamilySettingsRepository()
        repository.setSettings(testFamilySettings(profile = testFamilyProfile()))
        val useCase = UpdatePrivacyPreferences(repository)
        val preferences = PrivacyPreferences(diagnosticsEnabled = true)

        val result = useCase(preferences)

        assertEquals(DataResult.Success(Unit), result)
        val stored = (repository.observeSettings().first() as DataResult.Success).value
        assertEquals(preferences, stored?.privacyPreferences)
    }
}
