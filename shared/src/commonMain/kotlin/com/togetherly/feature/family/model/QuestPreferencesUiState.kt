package com.togetherly.feature.family.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.quest.EnergyLevel
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * The quest preferences editor's draft (Step 13.3) — same shape/rules as
 * [FamilyProfileUiState]/[com.togetherly.feature.onboarding.model.OnboardingUiState]. Deliberately
 * excludes [com.togetherly.domain.family.AgeBand] ("age suitability") and
 * [com.togetherly.domain.quest.QuestCategory] ("interests") — both already have a dedicated editing
 * surface elsewhere ([com.togetherly.feature.family.presentation.FamilyProfileEditorScreen] for age
 * bands, onboarding for interests), so this screen isn't a second place to edit the same field.
 * [selectedDurations]/[locationPreference] *do* overlap with [FamilyProfileEditorScreen]'s own
 * fields — a deliberate "quick edit vs. deeper settings" duplication, not a bug: both write through
 * the same [com.togetherly.domain.family.FamilyProfile] source of truth, so edits made in either
 * screen are always consistent with each other.
 */
@Immutable
data class QuestPreferencesUiState(
    val isLoading: Boolean = true,
    val selectedDurations: PersistentSet<DurationBand> = persistentSetOf(),
    val selectedEnergyLevels: PersistentSet<EnergyLevel> = persistentSetOf(),
    val locationPreference: LocationPreference = LocationPreference.BOTH,
    val preparationPreference: PreparationPreference = PreparationPreference.ANY,
    val validationErrors: PersistentMap<QuestPreferencesField, UiText> = persistentMapOf(),
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val error: UiText? = null,
)
