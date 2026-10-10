package com.togetherly.feature.language.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.locale.AppLocaleController
import com.togetherly.core.locale.requiresRestartForLocaleChange
import com.togetherly.feature.language.model.LanguageAction
import com.togetherly.feature.language.model.LanguageEvent
import com.togetherly.feature.language.model.LanguageUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [showRestartPrompt] is only ever set to true on iOS ([requiresRestartForLocaleChange]) — on
 * Android the selection already takes effect by the time this screen's own next recomposition
 * happens (see [com.togetherly.app.application.App]'s `key(language)` wrapper), so there is
 * nothing to prompt for.
 */
class LanguageViewModel(private val localeController: AppLocaleController) : ViewModel() {

    private val _uiState = MutableStateFlow(LanguageUiState(selectedCode = localeController.language.value))
    val uiState: StateFlow<LanguageUiState> = _uiState.asStateFlow()

    private val _events = Channel<LanguageEvent>(Channel.BUFFERED)
    val events: Flow<LanguageEvent> = _events.receiveAsFlow()

    fun onAction(action: LanguageAction) {
        when (action) {
            is LanguageAction.LanguageSelected -> onLanguageSelected(action.code)
            LanguageAction.RestartPromptDismissed -> _uiState.update { it.copy(showRestartPrompt = false) }
            LanguageAction.BackClicked -> viewModelScope.launch { _events.send(LanguageEvent.NavigateBack) }
        }
    }

    private fun onLanguageSelected(code: String?) {
        localeController.select(code)
        _uiState.update { it.copy(selectedCode = code, showRestartPrompt = requiresRestartForLocaleChange) }
    }
}
