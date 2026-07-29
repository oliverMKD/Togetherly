package com.togetherly.feature.family.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.result.DataResult
import com.togetherly.core.ui.UiText
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.family.FamilyDisplayName
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.repository.FamilyRepository
import com.togetherly.domain.family.usecase.UpdateFamilyProfile
import com.togetherly.domain.family.usecase.UpdateFamilyProfileCommand
import com.togetherly.feature.family.model.FamilyProfileAction
import com.togetherly.feature.family.model.FamilyProfileEditorEvent
import com.togetherly.feature.family.model.FamilyProfileField
import com.togetherly.feature.family.model.FamilyProfileUiState
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.family_profile_editor_missing_profile_error
import togetherly.shared.generated.resources.family_profile_updated_message

/**
 * Loads the existing [FamilyProfile] once ([onScreenStarted], a one-shot [FamilyRepository.getProfile]
 * read — same "load a draft once, edit locally" shape as onboarding builds its draft, not an ongoing
 * [FamilyRepository.observeProfile] subscription: an external change to the profile while this
 * editor is open should never silently overwrite the user's in-progress edits). [interests]/
 * [FamilyProfile.preparationPreference]/[FamilyProfile.reminderPreference] are captured from
 * [original] and passed through unchanged on save — this editor never touches them (see
 * [FamilyProfileUiState]'s own KDoc for why they're not editable fields here).
 *
 * Saving calls [updateFamilyProfile] directly — it already preserves [FamilyProfile.id] and
 * [FamilyProfile.createdAt] and bumps [FamilyProfile.updatedAt] on its own (see that use case's own
 * KDoc), so there is nothing extra to do here for "preserve profile ID"/"update modification
 * metadata." Today's own recommendation state reacts to the resulting [FamilyRepository.observeProfile]
 * emission on its own — see [com.togetherly.feature.today.presentation.TodayViewModel]'s existing
 * `observeProfile()` collector — without this ViewModel calling anything quest-selection-related;
 * nothing here ever regenerates a daily quest.
 */
class FamilyProfileEditorViewModel(
    private val familyRepository: FamilyRepository,
    private val updateFamilyProfile: UpdateFamilyProfile,
    private val saveMessageStore: FamilySaveMessageStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyProfileUiState())
    val uiState: StateFlow<FamilyProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<FamilyProfileEditorEvent>(Channel.BUFFERED)
    val events: Flow<FamilyProfileEditorEvent> = _events.receiveAsFlow()

    private var hasStarted = false
    private var original: FamilyProfile? = null

    fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true

        viewModelScope.launch {
            when (val result = familyRepository.getProfile()) {
                is DataResult.Success -> {
                    val profile = result.value
                    original = profile
                    _uiState.update {
                        if (profile != null) {
                            it.copy(
                                isLoading = false,
                                familyName = profile.displayName?.value.orEmpty(),
                                selectedAgeBands = profile.childAgeBands.toPersistentSet(),
                                selectedDurations = profile.preferredDurations.toPersistentSet(),
                                locationPreference = profile.locationPreference,
                            )
                        } else {
                            it.copy(isLoading = false, error = UiText.Resource(Res.string.family_profile_editor_missing_profile_error))
                        }
                    }
                }
                is DataResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.error.toUiText()) }
            }
        }
    }

    fun onAction(action: FamilyProfileAction) {
        when (action) {
            is FamilyProfileAction.FamilyNameChanged -> updateDraft(FamilyProfileField.FAMILY_NAME) {
                it.copy(familyName = action.value)
            }
            is FamilyProfileAction.AgeBandToggled -> updateDraft(FamilyProfileField.AGE_BANDS) {
                val bands = it.selectedAgeBands
                it.copy(selectedAgeBands = if (action.value in bands) bands.removing(action.value) else bands.adding(action.value))
            }
            is FamilyProfileAction.DurationToggled -> updateDraft(FamilyProfileField.DURATIONS) {
                val durations = it.selectedDurations
                it.copy(selectedDurations = if (action.value in durations) durations.removing(action.value) else durations.adding(action.value))
            }
            is FamilyProfileAction.LocationPreferenceChanged -> updateDraft { it.copy(locationPreference = action.value) }
            FamilyProfileAction.SaveClicked -> onSave()
            FamilyProfileAction.BackClicked -> onBack()
            FamilyProfileAction.DiscardConfirmed -> onDiscardConfirmed()
            FamilyProfileAction.DismissDiscardDialog -> _uiState.update { it.copy(showDiscardDialog = false) }
        }
    }

    /** Same clear-only-the-touched-field(s) pattern as [com.togetherly.feature.onboarding.presentation.OnboardingViewModel.updateDraft]. */
    private fun updateDraft(vararg fields: FamilyProfileField, transform: (FamilyProfileUiState) -> FamilyProfileUiState) {
        _uiState.update { current ->
            val next = transform(current)
            val remainingErrors = fields.fold(next.validationErrors) { errors, field -> errors.removing(field) }
            next.copy(
                validationErrors = remainingErrors,
                error = null,
                hasUnsavedChanges = next.differsFromOriginal(original),
            )
        }
    }

    private fun onBack() {
        if (_uiState.value.hasUnsavedChanges) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            viewModelScope.launch { _events.send(FamilyProfileEditorEvent.NavigatedBackWithoutSaving) }
        }
    }

    private fun onDiscardConfirmed() {
        _uiState.update { it.copy(showDiscardDialog = false) }
        viewModelScope.launch { _events.send(FamilyProfileEditorEvent.NavigatedBackWithoutSaving) }
    }

    private fun onSave() {
        val current = _uiState.value
        if (current.isSaving) return

        val errors = FamilyProfileValidator.validate(current)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }
        val baseline = original ?: return

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            when (val result = updateFamilyProfile(current.toCommand(baseline))) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
                    saveMessageStore.publish(UiText.Resource(Res.string.family_profile_updated_message))
                    _events.send(FamilyProfileEditorEvent.SaveCompleted)
                }
                is DataResult.Error -> _uiState.update { it.copy(isSaving = false, error = result.error.toUiText()) }
            }
        }
    }
}

/** [interests]/[FamilyProfile.preparationPreference]/[FamilyProfile.reminderPreference] pass through from [baseline] untouched — this editor never collects them. */
private fun FamilyProfileUiState.toCommand(baseline: FamilyProfile): UpdateFamilyProfileCommand {
    val trimmedName = familyName.trim()
    return UpdateFamilyProfileCommand(
        displayName = trimmedName.takeIf { it.isNotEmpty() }?.let { FamilyDisplayName(it) },
        childAgeBands = selectedAgeBands,
        interests = baseline.interests,
        preferredDurations = selectedDurations,
        locationPreference = locationPreference,
        preparationPreference = baseline.preparationPreference,
        preferredEnergyLevels = baseline.preferredEnergyLevels,
        reminderPreference = baseline.reminderPreference,
    )
}

private fun FamilyProfileUiState.differsFromOriginal(original: FamilyProfile?): Boolean {
    val baseline = original ?: return false
    val trimmedName = familyName.trim()
    val baselineName = baseline.displayName?.value.orEmpty()
    return trimmedName != baselineName ||
        selectedAgeBands.toSet() != baseline.childAgeBands ||
        selectedDurations.toSet() != baseline.preferredDurations ||
        locationPreference != baseline.locationPreference
}
