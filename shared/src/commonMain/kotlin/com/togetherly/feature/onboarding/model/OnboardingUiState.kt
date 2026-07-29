package com.togetherly.feature.onboarding.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.quest.QuestCategory
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/**
 * The single, complete snapshot of onboarding's draft — one state for the whole flow, not one per
 * step, since [step] itself is just a field here rather than separate navigation state. Deliberately
 * holds nothing platform-shaped (`NavController`, `Context`, `SnackbarHostState`, `FocusRequester`,
 * a keyboard controller) and nothing mutable ([PersistentSet]/[PersistentMap], never a plain
 * `MutableSet`/`MutableMap`) — every field here must survive being diffed, compared, and replayed
 * by Compose's own recomposition machinery without surprises.
 *
 * [familyName] is the raw, untrimmed text field value (so the text field never fights the user's
 * cursor); trimming and the blank→`null` normalization happen only when building the actual save
 * command, never here — see [com.togetherly.feature.onboarding.presentation.OnboardingViewModel].
 */
@Immutable
data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val familyName: String = "",
    val selectedAgeBands: PersistentSet<AgeBand> = persistentSetOf(),
    val selectedInterests: PersistentSet<QuestCategory> = persistentSetOf(),
    val selectedDurations: PersistentSet<DurationBand> = persistentSetOf(),
    val locationPreference: LocationPreference = LocationPreference.BOTH,
    val preparationPreference: PreparationPreference = PreparationPreference.NONE,
    val reminderEnabled: Boolean = false,
    val reminderDays: PersistentSet<DayOfWeek> = persistentSetOf(),
    val reminderTime: LocalTime? = null,
    val validationErrors: PersistentMap<OnboardingField, UiText> = persistentMapOf(),
    val isSaving: Boolean = false,
    val saveError: UiText? = null,
)
