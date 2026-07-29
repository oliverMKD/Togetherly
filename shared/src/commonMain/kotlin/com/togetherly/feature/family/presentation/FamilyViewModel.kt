package com.togetherly.feature.family.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.result.DataResult
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.family.repository.FamilyRepository
import com.togetherly.feature.family.model.FamilyAction
import com.togetherly.feature.family.model.FamilyEvent
import com.togetherly.feature.family.model.FamilyUiState
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The Family tab root — reads [FamilyRepository.observeProfile] directly, same convention
 * [com.togetherly.feature.today.presentation.TodayViewModel] already established for pure
 * read/observe access to the family profile. Stays subscribed for the tab's whole lifetime (this
 * ViewModel is paused, not destroyed, while [com.togetherly.feature.family.model.FamilyProfileEditorEvent]
 * pushes [com.togetherly.navigation.destination.RootDestination.FamilyProfileEditor] on top of it —
 * see [com.togetherly.navigation.host.TogetherlyNavHost]'s own KDoc on that back-stack shape), so an
 * edit made there is reflected here the moment the user navigates back, with no manual refresh.
 *
 * [FamilySaveMessageStore.message] is collected the same way, for the whole lifetime — see that
 * store's own KDoc on why the publish→collect race that would matter for a cold flow doesn't apply.
 */
class FamilyViewModel(
    private val familyRepository: FamilyRepository,
    private val saveMessageStore: FamilySaveMessageStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    private val _events = Channel<FamilyEvent>(Channel.BUFFERED)
    val events: Flow<FamilyEvent> = _events.receiveAsFlow()

    private var hasStarted = false

    fun onAction(action: FamilyAction) {
        when (action) {
            FamilyAction.EditProfileClicked -> viewModelScope.launch { _events.send(FamilyEvent.OpenProfileEditor) }
            FamilyAction.QuestPreferencesClicked -> viewModelScope.launch { _events.send(FamilyEvent.OpenQuestPreferences) }
            FamilyAction.ReminderClicked -> viewModelScope.launch { _events.send(FamilyEvent.OpenReminder) }
            FamilyAction.MemorySettingsClicked -> viewModelScope.launch { _events.send(FamilyEvent.OpenMemorySettings) }
            FamilyAction.PrivacyClicked -> viewModelScope.launch { _events.send(FamilyEvent.OpenPrivacy) }
            FamilyAction.LegalClicked -> viewModelScope.launch { _events.send(FamilyEvent.OpenLegal) }
            FamilyAction.AboutClicked -> viewModelScope.launch { _events.send(FamilyEvent.OpenAbout) }
            FamilyAction.DataManagementClicked -> viewModelScope.launch { _events.send(FamilyEvent.OpenDataManagement) }
            FamilyAction.MessageDismissed -> _uiState.update { it.copy(message = null) }
        }
    }

    fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true

        viewModelScope.launch {
            familyRepository.observeProfile().collect { result ->
                when (result) {
                    is DataResult.Success -> {
                        val profile = result.value
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                familyName = profile?.displayName?.value,
                                ageBands = profile?.childAgeBands?.toPersistentSet() ?: it.ageBands,
                                preferredDurations = profile?.preferredDurations?.toPersistentSet() ?: it.preferredDurations,
                                locationPreference = profile?.locationPreference ?: it.locationPreference,
                            )
                        }
                    }
                    is DataResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                }
            }
        }

        viewModelScope.launch {
            saveMessageStore.message.collect { message ->
                if (message != null) {
                    _uiState.update { it.copy(message = message) }
                    saveMessageStore.consume()
                }
            }
        }
    }
}
