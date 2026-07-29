package com.togetherly.feature.family.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.net.isValidExternalUrl
import com.togetherly.feature.family.model.OpenSourceLicensesAction
import com.togetherly.feature.family.model.OpenSourceLicensesEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Purely informational — [com.togetherly.feature.family.model.OPEN_SOURCE_LICENSES] is a static
 * list, nothing to load. Each entry's project URL is still validated with [isValidExternalUrl]
 * before [OpenSourceLicensesEvent.OpenExternalUrl] is emitted, same rule [LegalViewModel] follows.
 */
class OpenSourceLicensesViewModel : ViewModel() {

    private val _events = Channel<OpenSourceLicensesEvent>(Channel.BUFFERED)
    val events: Flow<OpenSourceLicensesEvent> = _events.receiveAsFlow()

    fun onAction(action: OpenSourceLicensesAction) {
        when (action) {
            OpenSourceLicensesAction.BackClicked -> send(OpenSourceLicensesEvent.NavigateBack)
            is OpenSourceLicensesAction.LicenseClicked -> {
                if (isValidExternalUrl(action.license.url)) {
                    send(OpenSourceLicensesEvent.OpenExternalUrl(action.license.url))
                }
            }
        }
    }

    private fun send(event: OpenSourceLicensesEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
