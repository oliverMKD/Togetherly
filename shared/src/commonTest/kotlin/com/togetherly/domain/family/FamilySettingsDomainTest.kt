package com.togetherly.domain.family

import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.integration.testFamilyProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FamilySettingsDomainTest {

    @Test
    fun memoryPreferencesDefaultsPreserveExistingAppBehavior() {
        val defaults = MemoryPreferences.defaults()

        // Photo/voice/note capture and the post-quest memory prompt were all already
        // unconditionally available/shown before this setting existed — an upgrading install must
        // never silently lose a capability it already had.
        assertTrue(defaults.allowPhotos)
        assertTrue(defaults.allowVoiceMemories)
        assertTrue(defaults.allowTextNotes)
        assertTrue(defaults.showMemoryPromptAfterQuests)
    }

    @Test
    fun privacyPreferencesDefaultToDiagnosticsDisabled() {
        assertFalse(PrivacyPreferences.defaults().diagnosticsEnabled)
    }

    @Test
    fun questPreferencesAreProjectedFromTheFamilyProfileWithoutDuplication() {
        val profile = testFamilyProfile()

        val questPreferences = profile.toQuestPreferences()

        assertEquals(profile.interests, questPreferences.interests)
        assertEquals(profile.preferredDurations, questPreferences.preferredDurations)
        assertEquals(profile.locationPreference, questPreferences.locationPreference)
        assertEquals(profile.preferredEnergyLevels, questPreferences.preferredEnergyLevels)
        assertEquals(profile.preparationPreference, questPreferences.preparationPreference)
    }

    @Test
    fun questPreferencesDefaultsAreMaximallyPermissive() {
        val defaults = QuestPreferences.defaults(interests = setOf(QuestCategory.TALK))

        assertEquals(setOf(QuestCategory.TALK), defaults.interests)
        assertEquals(DurationBand.entries.toSet(), defaults.preferredDurations)
        assertEquals(LocationPreference.BOTH, defaults.locationPreference)
        assertEquals(PreparationPreference.ANY, defaults.preparationPreference)
        assertEquals(EnergyLevel.entries.toSet(), defaults.preferredEnergyLevels)
    }

    @Test
    fun familySettingsReminderPreferenceReusesTheProfilesOwnNullableShape() {
        val profileWithoutReminders = testFamilyProfile()
        val settings = FamilySettings(
            profile = profileWithoutReminders,
            questPreferences = profileWithoutReminders.toQuestPreferences(),
            reminderPreference = null,
            memoryPreferences = MemoryPreferences.defaults(),
            privacyPreferences = PrivacyPreferences.defaults(),
        )

        assertEquals(null, settings.reminderPreference)
    }
}
