package com.togetherly.feature.family.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * The Family Profile editor's draft — same shape/rules as
 * [com.togetherly.feature.onboarding.model.OnboardingUiState] (nothing platform-shaped, only
 * immutable collections, raw untrimmed [familyName] text).
 *
 * Deliberately smaller than the step spec's own example: there is no `participantCount` field —
 * [com.togetherly.domain.family.FamilyProfile] represents a household only by which [AgeBand]s are
 * present in it (see that enum's own KDoc: "never a child identity"), not a headcount, so adding one
 * here would be inventing a field with nowhere real to persist it. There is likewise no "energy
 * level" field — the closest existing concepts ([com.togetherly.domain.family.PreparationPreference],
 * [com.togetherly.domain.quest.QuestCategory]) describe preparation effort and activity type, not
 * energy level, so neither is a real equivalent. [selectedAgeBands]/[selectedDurations]/
 * [locationPreference] are the editable fields that do have exact existing equivalents (childAgeBands/
 * preferredDurations/locationPreference on [com.togetherly.domain.family.FamilyProfile], "Broad child
 * age ranges" / "Preferred activity duration" / "Indoor/outdoor preferences" in the step spec).
 * [interests]/[preparationPreference]/[reminderPreference] are intentionally not editable here —
 * they pass through unchanged on save (see [com.togetherly.feature.family.presentation.FamilyProfileEditorViewModel]).
 */
@Immutable
data class FamilyProfileUiState(
    val isLoading: Boolean = true,
    val familyName: String = "",
    val selectedAgeBands: PersistentSet<AgeBand> = persistentSetOf(),
    val selectedDurations: PersistentSet<DurationBand> = persistentSetOf(),
    val locationPreference: LocationPreference = LocationPreference.BOTH,
    val validationErrors: PersistentMap<FamilyProfileField, UiText> = persistentMapOf(),
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val error: UiText? = null,
)
