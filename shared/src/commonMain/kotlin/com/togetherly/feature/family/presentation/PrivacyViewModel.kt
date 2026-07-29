package com.togetherly.feature.family.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.feature.family.model.PrivacyAction
import com.togetherly.feature.family.model.PrivacyEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Purely informational — no [com.togetherly.domain.family.repository.FamilySettingsRepository]
 * dependency, nothing to load, nothing to save. Still follows this project's one-time-event
 * navigation convention ([PrivacyEvent.NavigateBack]) rather than the screen calling a raw
 * `onNavigateBack` lambda directly from a button's `onClick`, for consistency with every other
 * screen in this feature area.
 */
class PrivacyViewModel : ViewModel() {

    private val _events = Channel<PrivacyEvent>(Channel.BUFFERED)
    val events: Flow<PrivacyEvent> = _events.receiveAsFlow()

    fun onAction(action: PrivacyAction) {
        when (action) {
            PrivacyAction.BackClicked -> viewModelScope.launch { _events.send(PrivacyEvent.NavigateBack) }
        }
    }
}
