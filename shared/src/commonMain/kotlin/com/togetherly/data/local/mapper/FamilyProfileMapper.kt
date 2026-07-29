package com.togetherly.data.local.mapper

import com.togetherly.core.result.DataResult
import com.togetherly.core.result.getOrElse
import com.togetherly.data.local.family.FamilyAgeBandEntity
import com.togetherly.data.local.family.FamilyDurationPreferenceEntity
import com.togetherly.data.local.family.FamilyEnergyPreferenceEntity
import com.togetherly.data.local.family.FamilyInterestEntity
import com.togetherly.data.local.family.FamilyProfileEntity
import com.togetherly.data.local.family.FamilyProfileWithDetails
import com.togetherly.data.local.family.FamilyReminderDayEntity
import com.togetherly.data.local.keys.toAgeBand
import com.togetherly.data.local.keys.toDayOfWeek
import com.togetherly.data.local.keys.toDurationBand
import com.togetherly.data.local.keys.toEnergyLevel
import com.togetherly.data.local.keys.toLocationPreference
import com.togetherly.data.local.keys.toPreparationPreference
import com.togetherly.data.local.keys.toQuestCategory
import com.togetherly.data.local.keys.toStorageKey
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyDisplayName
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.FamilySettings
import com.togetherly.domain.family.MemoryPreferences
import com.togetherly.domain.family.PrivacyPreferences
import com.togetherly.domain.family.QuestPreferences
import com.togetherly.domain.family.ReminderPreference
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestCategory
import kotlinx.datetime.DayOfWeek
import kotlin.time.Instant

internal data class FamilyProfileComponents(
    val profile: FamilyProfileEntity,
    val ageBands: List<FamilyAgeBandEntity>,
    val interests: List<FamilyInterestEntity>,
    val durationPreferences: List<FamilyDurationPreferenceEntity>,
    val energyPreferences: List<FamilyEnergyPreferenceEntity>,
    val reminderDays: List<FamilyReminderDayEntity>,
)

/**
 * Not an [EntityMapper]: a family profile spans five tables, so this exposes a focused pair of
 * functions instead of forcing a one-entity-shaped interface — see [EntityMapper]'s own KDoc for
 * why [toComponents] itself cannot fail.
 *
 * A row set is corrupted (not just "unusual") when: any stored enum key is unrecognized; age
 * bands, interests, or durations are empty (domain requires at least one of each); a reminder
 * time exists without reminder days, or reminder days exist without a time — a family's
 * [ReminderPreference] is all-or-nothing, never half-configured; or the created/updated timestamp
 * relationship is invalid.
 *
 * [toComponents]'s [memoryPreferences]/[privacyPreferences] parameters (Step 13.1) exist because
 * [FamilyProfile] itself deliberately does not carry them (see [FamilySettings]'s own KDoc) — a
 * caller replacing the whole profile row (via [com.togetherly.data.family.RoomFamilyRepository.saveProfile])
 * must pass the *current* stored settings values back in, or a profile edit would silently reset
 * them to their defaults. [com.togetherly.data.family.RoomFamilyRepository] does exactly that by
 * reading the existing row first; [com.togetherly.data.family.RoomFamilySettingsRepository] never
 * calls [toComponents] at all — its own partial-update DAO methods touch only their own columns.
 */
internal class FamilyProfileMapper {

    fun toComponents(
        domain: FamilyProfile,
        memoryPreferences: MemoryPreferences = MemoryPreferences.defaults(),
        privacyPreferences: PrivacyPreferences = PrivacyPreferences.defaults(),
    ): FamilyProfileComponents {
        val familyId = domain.id.value
        return FamilyProfileComponents(
            profile = FamilyProfileEntity(
                id = familyId,
                displayName = domain.displayName?.value,
                locationPreference = domain.locationPreference.toStorageKey(),
                preparationPreference = domain.preparationPreference.toStorageKey(),
                reminderLocalTime = domain.reminderPreference?.localTime?.toString(),
                allowPhotos = memoryPreferences.allowPhotos,
                allowVoiceMemories = memoryPreferences.allowVoiceMemories,
                allowTextNotes = memoryPreferences.allowTextNotes,
                diagnosticsEnabled = privacyPreferences.diagnosticsEnabled,
                showMemoryPromptAfterQuests = memoryPreferences.showMemoryPromptAfterQuests,
                createdAtEpochMillis = domain.createdAt.toEpochMilliseconds(),
                updatedAtEpochMillis = domain.updatedAt.toEpochMilliseconds(),
            ),
            ageBands = domain.childAgeBands.map { FamilyAgeBandEntity(familyId, it.toStorageKey()) },
            interests = domain.interests.map { FamilyInterestEntity(familyId, it.toStorageKey()) },
            durationPreferences = domain.preferredDurations.map {
                FamilyDurationPreferenceEntity(familyId, it.toStorageKey())
            },
            energyPreferences = domain.preferredEnergyLevels.map {
                FamilyEnergyPreferenceEntity(familyId, it.toStorageKey())
            },
            reminderDays = domain.reminderPreference?.enabledDays.orEmpty().map {
                FamilyReminderDayEntity(familyId, it.toStorageKey())
            },
        )
    }

    fun toDomain(relation: FamilyProfileWithDetails): DataResult<FamilyProfile> {
        val ageBands = mutableSetOf<AgeBand>()
        for (row in relation.ageBands) {
            ageBands += row.ageBand.toAgeBand().getOrElse { return DataResult.Error(it) }
        }
        if (ageBands.isEmpty()) return corruptedStorage()

        val interests = mutableSetOf<QuestCategory>()
        for (row in relation.interests) {
            interests += row.category.toQuestCategory().getOrElse { return DataResult.Error(it) }
        }
        if (interests.isEmpty()) return corruptedStorage()

        val durations = mutableSetOf<DurationBand>()
        for (row in relation.durationPreferences) {
            durations += row.duration.toDurationBand().getOrElse { return DataResult.Error(it) }
        }
        if (durations.isEmpty()) return corruptedStorage()

        val locationPreference = relation.profile.locationPreference.toLocationPreference()
            .getOrElse { return DataResult.Error(it) }
        val preparationPreference = relation.profile.preparationPreference.toPreparationPreference()
            .getOrElse { return DataResult.Error(it) }

        // Unlike age bands/interests/durations, an empty set is a valid state here — "no energy
        // preference set yet," not corrupted storage. See FamilyProfile's own KDoc.
        val energyLevels = mutableSetOf<EnergyLevel>()
        for (row in relation.energyPreferences) {
            energyLevels += row.energyLevel.toEnergyLevel().getOrElse { return DataResult.Error(it) }
        }

        val reminderPreference = reminderPreferenceOf(relation).getOrElse { return DataResult.Error(it) }

        return mapStorageCatching {
            FamilyProfile(
                id = FamilyId(relation.profile.id),
                displayName = relation.profile.displayName?.let { FamilyDisplayName(it) },
                childAgeBands = ageBands,
                interests = interests,
                preferredDurations = durations,
                locationPreference = locationPreference,
                preparationPreference = preparationPreference,
                preferredEnergyLevels = energyLevels,
                reminderPreference = reminderPreference,
                createdAt = Instant.fromEpochMilliseconds(relation.profile.createdAtEpochMillis),
                updatedAt = Instant.fromEpochMilliseconds(relation.profile.updatedAtEpochMillis),
            )
        }
    }

    /**
     * [FamilySettings.profile] is built via [toDomain] — the exact same mapping, never a second
     * independent read of the profile columns — so [FamilySettings] can never disagree with what
     * [com.togetherly.domain.family.repository.FamilyRepository] itself reports for the same row.
     */
    fun toSettingsDomain(relation: FamilyProfileWithDetails): DataResult<FamilySettings> {
        val profile = toDomain(relation).getOrElse { return DataResult.Error(it) }
        return DataResult.Success(
            FamilySettings(
                profile = profile,
                questPreferences = QuestPreferences(
                    interests = profile.interests,
                    preferredDurations = profile.preferredDurations,
                    locationPreference = profile.locationPreference,
                    preparationPreference = profile.preparationPreference,
                    preferredEnergyLevels = profile.preferredEnergyLevels,
                ),
                reminderPreference = profile.reminderPreference,
                memoryPreferences = MemoryPreferences(
                    allowPhotos = relation.profile.allowPhotos,
                    allowVoiceMemories = relation.profile.allowVoiceMemories,
                    allowTextNotes = relation.profile.allowTextNotes,
                    showMemoryPromptAfterQuests = relation.profile.showMemoryPromptAfterQuests,
                ),
                privacyPreferences = PrivacyPreferences(
                    diagnosticsEnabled = relation.profile.diagnosticsEnabled,
                ),
            ),
        )
    }

    private fun reminderPreferenceOf(relation: FamilyProfileWithDetails): DataResult<ReminderPreference?> {
        val reminderDays = mutableSetOf<DayOfWeek>()
        for (row in relation.reminderDays) {
            reminderDays += row.dayOfWeek.toDayOfWeek().getOrElse { return DataResult.Error(it) }
        }

        val reminderLocalTime = relation.profile.reminderLocalTime?.let { raw ->
            parseStoredLocalTime(raw).getOrElse { return DataResult.Error(it) }
        }

        // A ReminderPreference is all-or-nothing: a time without days, or days without a time,
        // is corrupted, not a partially-configured reminder.
        if ((reminderLocalTime == null) != reminderDays.isEmpty()) return corruptedStorage()

        return DataResult.Success(reminderLocalTime?.let { time -> ReminderPreference(enabledDays = reminderDays, localTime = time) })
    }
}
