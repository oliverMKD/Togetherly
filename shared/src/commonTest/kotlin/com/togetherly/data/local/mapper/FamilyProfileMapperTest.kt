package com.togetherly.data.local.mapper

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.data.local.family.FamilyAgeBandEntity
import com.togetherly.data.local.family.FamilyDurationPreferenceEntity
import com.togetherly.data.local.family.FamilyEnergyPreferenceEntity
import com.togetherly.data.local.family.FamilyInterestEntity
import com.togetherly.data.local.family.FamilyProfileEntity
import com.togetherly.data.local.family.FamilyProfileWithDetails
import com.togetherly.data.local.family.FamilyReminderDayEntity
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyDisplayName
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.family.ReminderPreference
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestCategory
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val CREATED_AT = Instant.fromEpochMilliseconds(1_690_000_000_000L)
private val UPDATED_AT = Instant.fromEpochMilliseconds(1_690_000_500_000L)

private fun testProfile(reminderPreference: ReminderPreference?) = FamilyProfile(
    id = FamilyId("family-1"),
    displayName = FamilyDisplayName("The Smiths"),
    childAgeBands = setOf(AgeBand.AGE_6_TO_8, AgeBand.AGE_9_TO_11),
    interests = setOf(QuestCategory.TALK, QuestCategory.MOVE),
    preferredDurations = setOf(DurationBand.TEN_MINUTES),
    locationPreference = LocationPreference.BOTH,
    preparationPreference = PreparationPreference.ANY,
    reminderPreference = reminderPreference,
    createdAt = CREATED_AT,
    updatedAt = UPDATED_AT,
)

private fun testProfileEntity(
    id: String = "family-1",
    reminderLocalTime: String? = null,
    allowPhotos: Boolean = true,
    allowVoiceMemories: Boolean = true,
    allowTextNotes: Boolean = true,
    diagnosticsEnabled: Boolean = false,
    showMemoryPromptAfterQuests: Boolean = true,
) = FamilyProfileEntity(
    id = id,
    displayName = "The Smiths",
    locationPreference = "both",
    preparationPreference = "any",
    reminderLocalTime = reminderLocalTime,
    allowPhotos = allowPhotos,
    allowVoiceMemories = allowVoiceMemories,
    allowTextNotes = allowTextNotes,
    diagnosticsEnabled = diagnosticsEnabled,
    showMemoryPromptAfterQuests = showMemoryPromptAfterQuests,
    createdAtEpochMillis = CREATED_AT.toEpochMilliseconds(),
    updatedAtEpochMillis = UPDATED_AT.toEpochMilliseconds(),
)

class FamilyProfileMapperTest {

    private val mapper = FamilyProfileMapper()

    @Test
    fun familyProfileWithRemindersRoundTrips() {
        val reminderTime = LocalTime(9, 30)
        val domain = testProfile(ReminderPreference(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), reminderTime))

        val components = mapper.toComponents(domain)
        assertEquals(reminderTime.toString(), components.profile.reminderLocalTime)
        assertEquals(setOf("monday", "friday"), components.reminderDays.map { it.dayOfWeek }.toSet())

        val relation = FamilyProfileWithDetails(
            profile = components.profile,
            ageBands = components.ageBands,
            interests = components.interests,
            durationPreferences = components.durationPreferences,
            energyPreferences = components.energyPreferences,
            reminderDays = components.reminderDays,
        )
        assertEquals(domain, (mapper.toDomain(relation) as DataResult.Success).value)
    }

    @Test
    fun familyProfileWithoutRemindersRoundTrips() {
        val domain = testProfile(reminderPreference = null)

        val components = mapper.toComponents(domain)
        assertEquals(null, components.profile.reminderLocalTime)
        assertEquals(emptyList(), components.reminderDays)

        val relation = FamilyProfileWithDetails(
            profile = components.profile,
            ageBands = components.ageBands,
            interests = components.interests,
            durationPreferences = components.durationPreferences,
            energyPreferences = components.energyPreferences,
            reminderDays = components.reminderDays,
        )
        assertEquals(domain, (mapper.toDomain(relation) as DataResult.Success).value)
    }

    @Test
    fun missingAgeBandsIsCorruptedStorage() {
        val relation = FamilyProfileWithDetails(
            profile = testProfileEntity(),
            ageBands = emptyList(),
            interests = listOf(FamilyInterestEntity("family-1", "talk")),
            durationPreferences = listOf(FamilyDurationPreferenceEntity("family-1", "ten_minutes")),
            energyPreferences = emptyList(),
            reminderDays = emptyList(),
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(relation))
    }

    @Test
    fun missingInterestsIsCorruptedStorage() {
        val relation = FamilyProfileWithDetails(
            profile = testProfileEntity(),
            ageBands = listOf(FamilyAgeBandEntity("family-1", "age_6_8")),
            interests = emptyList(),
            durationPreferences = listOf(FamilyDurationPreferenceEntity("family-1", "ten_minutes")),
            energyPreferences = emptyList(),
            reminderDays = emptyList(),
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(relation))
    }

    @Test
    fun missingDurationPreferencesIsCorruptedStorage() {
        val relation = FamilyProfileWithDetails(
            profile = testProfileEntity(),
            ageBands = listOf(FamilyAgeBandEntity("family-1", "age_6_8")),
            interests = listOf(FamilyInterestEntity("family-1", "talk")),
            durationPreferences = emptyList(),
            energyPreferences = emptyList(),
            reminderDays = emptyList(),
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(relation))
    }

    @Test
    fun reminderTimeWithoutDaysIsCorruptedStorage() {
        val relation = FamilyProfileWithDetails(
            profile = testProfileEntity(reminderLocalTime = "09:30:00"),
            ageBands = listOf(FamilyAgeBandEntity("family-1", "age_6_8")),
            interests = listOf(FamilyInterestEntity("family-1", "talk")),
            durationPreferences = listOf(FamilyDurationPreferenceEntity("family-1", "ten_minutes")),
            energyPreferences = emptyList(),
            reminderDays = emptyList(),
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(relation))
    }

    @Test
    fun reminderDaysWithoutTimeIsCorruptedStorage() {
        val relation = FamilyProfileWithDetails(
            profile = testProfileEntity(reminderLocalTime = null),
            ageBands = listOf(FamilyAgeBandEntity("family-1", "age_6_8")),
            interests = listOf(FamilyInterestEntity("family-1", "talk")),
            durationPreferences = listOf(FamilyDurationPreferenceEntity("family-1", "ten_minutes")),
            energyPreferences = emptyList(),
            reminderDays = listOf(FamilyReminderDayEntity("family-1", "monday")),
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(relation))
    }

    @Test
    fun invalidLocalTimeIsCorruptedStorage() {
        val relation = FamilyProfileWithDetails(
            profile = testProfileEntity(reminderLocalTime = "not-a-time"),
            ageBands = listOf(FamilyAgeBandEntity("family-1", "age_6_8")),
            interests = listOf(FamilyInterestEntity("family-1", "talk")),
            durationPreferences = listOf(FamilyDurationPreferenceEntity("family-1", "ten_minutes")),
            energyPreferences = emptyList(),
            reminderDays = listOf(FamilyReminderDayEntity("family-1", "monday")),
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(relation))
    }

    @Test
    fun invalidTimestampRelationshipIsCorruptedStorage() {
        val relation = FamilyProfileWithDetails(
            profile = testProfileEntity().copy(
                createdAtEpochMillis = UPDATED_AT.toEpochMilliseconds(),
                updatedAtEpochMillis = CREATED_AT.toEpochMilliseconds(),
            ),
            ageBands = listOf(FamilyAgeBandEntity("family-1", "age_6_8")),
            interests = listOf(FamilyInterestEntity("family-1", "talk")),
            durationPreferences = listOf(FamilyDurationPreferenceEntity("family-1", "ten_minutes")),
            energyPreferences = emptyList(),
            reminderDays = emptyList(),
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(relation))
    }

    @Test
    fun unknownAgeBandKeyIsCorruptedStorage() {
        val relation = FamilyProfileWithDetails(
            profile = testProfileEntity(),
            ageBands = listOf(FamilyAgeBandEntity("family-1", "not-a-band")),
            interests = listOf(FamilyInterestEntity("family-1", "talk")),
            durationPreferences = listOf(FamilyDurationPreferenceEntity("family-1", "ten_minutes")),
            energyPreferences = emptyList(),
            reminderDays = emptyList(),
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(relation))
    }

    @Test
    fun toComponentsPreservesTheMemoryAndPrivacyPreferencesItsGiven() {
        val domain = testProfile(reminderPreference = null)

        val components = mapper.toComponents(
            domain = domain,
            memoryPreferences = com.togetherly.domain.family.MemoryPreferences(
                allowPhotos = false,
                allowVoiceMemories = false,
                allowTextNotes = false,
                showMemoryPromptAfterQuests = false,
            ),
            privacyPreferences = com.togetherly.domain.family.PrivacyPreferences(diagnosticsEnabled = true),
        )

        assertEquals(false, components.profile.allowPhotos)
        assertEquals(false, components.profile.allowVoiceMemories)
        assertEquals(false, components.profile.allowTextNotes)
        assertEquals(false, components.profile.showMemoryPromptAfterQuests)
        assertEquals(true, components.profile.diagnosticsEnabled)
    }

    @Test
    fun toComponentsDefaultsToSafeMemoryAndPrivacyPreferencesWhenNotGiven() {
        val components = mapper.toComponents(testProfile(reminderPreference = null))

        assertEquals(true, components.profile.allowPhotos)
        assertEquals(true, components.profile.allowVoiceMemories)
        assertEquals(true, components.profile.allowTextNotes)
        assertEquals(true, components.profile.showMemoryPromptAfterQuests)
        assertEquals(false, components.profile.diagnosticsEnabled)
    }

    @Test
    fun toSettingsDomainUsesTheSameProfileMappingAsToDomain() {
        val relation = FamilyProfileWithDetails(
            profile = testProfileEntity(),
            ageBands = listOf(FamilyAgeBandEntity("family-1", "age_6_8")),
            interests = listOf(FamilyInterestEntity("family-1", "talk")),
            durationPreferences = listOf(FamilyDurationPreferenceEntity("family-1", "ten_minutes")),
            energyPreferences = emptyList(),
            reminderDays = emptyList(),
        )

        val profileResult = (mapper.toDomain(relation) as DataResult.Success).value
        val settingsResult = (mapper.toSettingsDomain(relation) as DataResult.Success).value

        assertEquals(profileResult, settingsResult.profile)
    }

    @Test
    fun toSettingsDomainMapsQuestPreferencesFromTheSameProfileFields() {
        val relation = FamilyProfileWithDetails(
            profile = testProfileEntity(),
            ageBands = listOf(FamilyAgeBandEntity("family-1", "age_6_8")),
            interests = listOf(FamilyInterestEntity("family-1", "talk")),
            durationPreferences = listOf(FamilyDurationPreferenceEntity("family-1", "ten_minutes")),
            energyPreferences = listOf(FamilyEnergyPreferenceEntity("family-1", "calm")),
            reminderDays = emptyList(),
        )

        val settings = (mapper.toSettingsDomain(relation) as DataResult.Success).value

        assertEquals(setOf(QuestCategory.TALK), settings.questPreferences.interests)
        assertEquals(setOf(DurationBand.TEN_MINUTES), settings.questPreferences.preferredDurations)
        assertEquals(LocationPreference.BOTH, settings.questPreferences.locationPreference)
        assertEquals(PreparationPreference.ANY, settings.questPreferences.preparationPreference)
        assertEquals(setOf(EnergyLevel.CALM), settings.questPreferences.preferredEnergyLevels)
    }

    @Test
    fun toSettingsDomainMapsMemoryAndPrivacyPreferencesFromTheEntityColumns() {
        val relation = FamilyProfileWithDetails(
            profile = testProfileEntity(
                allowPhotos = false,
                allowVoiceMemories = true,
                allowTextNotes = true,
                diagnosticsEnabled = true,
                showMemoryPromptAfterQuests = false,
            ),
            ageBands = listOf(FamilyAgeBandEntity("family-1", "age_6_8")),
            interests = listOf(FamilyInterestEntity("family-1", "talk")),
            durationPreferences = listOf(FamilyDurationPreferenceEntity("family-1", "ten_minutes")),
            energyPreferences = emptyList(),
            reminderDays = emptyList(),
        )

        val settings = (mapper.toSettingsDomain(relation) as DataResult.Success).value

        assertEquals(false, settings.memoryPreferences.allowPhotos)
        assertEquals(true, settings.memoryPreferences.allowVoiceMemories)
        assertEquals(true, settings.memoryPreferences.allowTextNotes)
        assertEquals(false, settings.memoryPreferences.showMemoryPromptAfterQuests)
        assertEquals(true, settings.privacyPreferences.diagnosticsEnabled)
    }

    @Test
    fun toSettingsDomainPropagatesCorruptedStorageTheSameWayToDomainDoes() {
        val relation = FamilyProfileWithDetails(
            profile = testProfileEntity(),
            ageBands = emptyList(),
            interests = listOf(FamilyInterestEntity("family-1", "talk")),
            durationPreferences = listOf(FamilyDurationPreferenceEntity("family-1", "ten_minutes")),
            energyPreferences = emptyList(),
            reminderDays = emptyList(),
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toSettingsDomain(relation))
    }
}
