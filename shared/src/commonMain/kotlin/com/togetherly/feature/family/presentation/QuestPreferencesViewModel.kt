package com.togetherly.feature.family.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.result.DataResult
import com.togetherly.core.ui.UiText
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.family.QuestPreferences
import com.togetherly.domain.family.usecase.ObserveFamilySettings
import com.togetherly.domain.family.usecase.UpdateQuestPreferences
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.feature.family.model.QuestPreferencesAction
import com.togetherly.feature.family.model.QuestPreferencesEvent
import com.togetherly.feature.family.model.QuestPreferencesField
import com.togetherly.feature.family.model.QuestPreferencesUiState
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.quest_preferences_missing_profile_error
import togetherly.shared.generated.resources.quest_preferences_updated_message

/**
 * Loads [com.togetherly.domain.family.FamilySettings.questPreferences] once ([onScreenStarted],
 * `.first()` on [observeFamilySettings]'s `Flow` — same "load a draft once, edit locally" shape as
 * [FamilyProfileEditorViewModel]: an external change to preferences while this editor is open
 * should never silently overwrite the user's in-progress edits) — [interests] is captured from the
 * loaded value and passed through unchanged on save/reset, since this screen never edits it (see
 * [QuestPreferencesUiState]'s own KDoc).
 *
 * Saving never touches [com.togetherly.domain.daily.DailyQuest] or calls anything reroll-related —
 * [UpdateQuestPreferences] only writes the preference row; Today's own daily-quest selection
 * already runs independently (once per day, on its own `loadDailyQuest()` call — see
 * [com.togetherly.feature.today.presentation.TodayViewModel]'s own KDoc), so today's already-picked
 * quest is untouched, and no reroll allowance is consumed.
 */
class QuestPreferencesViewModel(
    private val observeFamilySettings: ObserveFamilySettings,
    private val updateQuestPreferences: UpdateQuestPreferences,
    private val saveMessageStore: FamilySaveMessageStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestPreferencesUiState())
    val uiState: StateFlow<QuestPreferencesUiState> = _uiState.asStateFlow()

    private val _events = Channel<QuestPreferencesEvent>(Channel.BUFFERED)
    val events: Flow<QuestPreferencesEvent> = _events.receiveAsFlow()

    private var hasStarted = false
    private var original: QuestPreferences? = null
    private var interests: Set<QuestCategory> = emptySet()

    fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true

        viewModelScope.launch {
            when (val result = observeFamilySettings().first()) {
                is DataResult.Success -> {
                    val settings = result.value
                    if (settings != null) {
                        val preferences = settings.questPreferences
                        original = preferences
                        interests = preferences.interests
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                selectedDurations = preferences.preferredDurations.toPersistentSet(),
                                selectedEnergyLevels = preferences.preferredEnergyLevels.toPersistentSet(),
                                locationPreference = preferences.locationPreference,
                                preparationPreference = preferences.preparationPreference,
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = UiText.Resource(Res.string.quest_preferences_missing_profile_error)) }
                    }
                }
                is DataResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.error.toUiText()) }
            }
        }
    }

    fun onAction(action: QuestPreferencesAction) {
        when (action) {
            is QuestPreferencesAction.DurationToggled -> updateDraft(QuestPreferencesField.DURATIONS) {
                val durations = it.selectedDurations
                it.copy(selectedDurations = if (action.value in durations) durations.removing(action.value) else durations.adding(action.value))
            }
            is QuestPreferencesAction.EnergyToggled -> updateDraft(QuestPreferencesField.ENERGY) {
                val levels = it.selectedEnergyLevels
                it.copy(selectedEnergyLevels = if (action.value in levels) levels.removing(action.value) else levels.adding(action.value))
            }
            is QuestPreferencesAction.LocationPreferenceChanged -> updateDraft { it.copy(locationPreference = action.value) }
            is QuestPreferencesAction.PreparationPreferenceChanged -> updateDraft { it.copy(preparationPreference = action.value) }
            QuestPreferencesAction.ResetToDefaultsClicked -> onResetToDefaults()
            QuestPreferencesAction.SaveClicked -> onSave()
            QuestPreferencesAction.BackClicked -> onBack()
            QuestPreferencesAction.DiscardConfirmed -> onDiscardConfirmed()
            QuestPreferencesAction.DismissDiscardDialog -> _uiState.update { it.copy(showDiscardDialog = false) }
        }
    }

    private fun updateDraft(vararg fields: QuestPreferencesField, transform: (QuestPreferencesUiState) -> QuestPreferencesUiState) {
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

    /** [QuestPreferences.defaults] is the maximally-permissive starting point — resetting can never make every recommendation impossible. */
    private fun onResetToDefaults() {
        val defaults = QuestPreferences.defaults(interests)
        updateDraft(QuestPreferencesField.DURATIONS, QuestPreferencesField.ENERGY) {
            it.copy(
                selectedDurations = defaults.preferredDurations.toPersistentSet(),
                selectedEnergyLevels = defaults.preferredEnergyLevels.toPersistentSet(),
                locationPreference = defaults.locationPreference,
                preparationPreference = defaults.preparationPreference,
            )
        }
    }

    private fun onBack() {
        if (_uiState.value.hasUnsavedChanges) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            viewModelScope.launch { _events.send(QuestPreferencesEvent.NavigatedBackWithoutSaving) }
        }
    }

    private fun onDiscardConfirmed() {
        _uiState.update { it.copy(showDiscardDialog = false) }
        viewModelScope.launch { _events.send(QuestPreferencesEvent.NavigatedBackWithoutSaving) }
    }

    private fun onSave() {
        val current = _uiState.value
        if (current.isSaving) return

        val errors = QuestPreferencesValidator.validate(current)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        val preferences = QuestPreferences(
            interests = interests,
            preferredDurations = current.selectedDurations,
            locationPreference = current.locationPreference,
            preparationPreference = current.preparationPreference,
            preferredEnergyLevels = current.selectedEnergyLevels,
        )

        viewModelScope.launch {
            when (val result = updateQuestPreferences(preferences)) {
                is DataResult.Success -> {
                    original = preferences
                    _uiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
                    saveMessageStore.publish(UiText.Resource(Res.string.quest_preferences_updated_message))
                    _events.send(QuestPreferencesEvent.SaveCompleted)
                }
                is DataResult.Error -> _uiState.update { it.copy(isSaving = false, error = result.error.toUiText()) }
            }
        }
    }
}

private fun QuestPreferencesUiState.differsFromOriginal(original: QuestPreferences?): Boolean {
    val baseline = original ?: return false
    return selectedDurations.toSet() != baseline.preferredDurations ||
        selectedEnergyLevels.toSet() != baseline.preferredEnergyLevels ||
        locationPreference != baseline.locationPreference ||
        preparationPreference != baseline.preparationPreference
}
