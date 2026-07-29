package com.togetherly.feature.family.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.app.application.LegalConfiguration
import com.togetherly.core.net.isValidExternalUrl
import com.togetherly.feature.family.model.LegalAction
import com.togetherly.feature.family.model.LegalEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Every link is validated with [isValidExternalUrl] here, in presentation, before
 * [LegalEvent.OpenExternalUrl] is ever emitted — [LegalRoute] never calls
 * [com.togetherly.core.net.ExternalUrlLauncher] with an unvalidated value, matching this project's
 * "reusable UI components never call platform URL launchers directly" rule. An invalid/blank
 * [LegalConfiguration] entry (should never happen with [LegalConfiguration.placeholder], but this
 * stays correct even if a future release wires in a genuinely empty value) simply produces no event
 * rather than a crash or a dead click.
 */
class LegalViewModel(
    private val legalConfiguration: LegalConfiguration,
) : ViewModel() {

    private val _events = Channel<LegalEvent>(Channel.BUFFERED)
    val events: Flow<LegalEvent> = _events.receiveAsFlow()

    fun onAction(action: LegalAction) {
        when (action) {
            LegalAction.BackClicked -> send(LegalEvent.NavigateBack)
            LegalAction.PrivacyPolicyClicked -> openUrl(legalConfiguration.privacyPolicyUrl)
            LegalAction.TermsOfUseClicked -> openUrl(legalConfiguration.termsOfUseUrl)
            LegalAction.SubscriptionTermsClicked -> openUrl(legalConfiguration.subscriptionTermsUrl)
            LegalAction.OpenSourceLicensesClicked -> send(LegalEvent.OpenOpenSourceLicenses)
        }
    }

    private fun openUrl(url: String) {
        if (!isValidExternalUrl(url)) return
        send(LegalEvent.OpenExternalUrl(url))
    }

    private fun send(event: LegalEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
