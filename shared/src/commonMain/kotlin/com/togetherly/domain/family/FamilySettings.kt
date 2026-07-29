package com.togetherly.domain.family

/**
 * Everything a parent-facing Family settings screen needs, composed from one underlying storage
 * row — never a second source of truth for [profile] (see [com.togetherly.domain.family.repository.FamilySettingsRepository]'s
 * own KDoc). [reminderPreference] reuses [ReminderPreference]'s existing nullable-is-disabled shape
 * directly rather than introducing a parallel `enabled: Boolean` wrapper: a `ReminderPreference`
 * is already all-or-nothing by construction (see its own `init`), so an `enabled` flag alongside a
 * nullable time/day set would only ever let you represent a state ("enabled but no time chosen")
 * that already can't happen and has no real behavior behind it.
 */
data class FamilySettings(
    val profile: FamilyProfile,
    val questPreferences: QuestPreferences,
    val reminderPreference: ReminderPreference?,
    val memoryPreferences: MemoryPreferences,
    val privacyPreferences: PrivacyPreferences,
)
