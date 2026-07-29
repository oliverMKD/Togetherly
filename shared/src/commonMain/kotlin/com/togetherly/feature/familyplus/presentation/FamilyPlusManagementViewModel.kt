package com.togetherly.feature.familyplus.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.datetime.AppTimeZoneProvider
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.telemetry.RestoreOutcome
import com.togetherly.core.telemetry.RestoreOutcomeResult
import com.togetherly.core.telemetry.RestoreStarted
import com.togetherly.core.telemetry.toRestoreOutcomeResult
import com.togetherly.core.ui.UiText
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.purchase.RestoreResult
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.purchase.usecase.RestoreFamilyPlus
import com.togetherly.feature.familyplus.mapper.toRenewalInfo
import com.togetherly.feature.familyplus.model.FamilyPlusManagementUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.familyplus_customer_center_unavailable
import togetherly.shared.generated.resources.restore_no_purchases_found_message
import togetherly.shared.generated.resources.restore_succeeded_message

/**
 * [EntitlementRepository] is injected directly (same convention as [com.togetherly.feature.paywall.presentation.FamilyPlusPaywallViewModel]/
 * [com.togetherly.feature.today.presentation.TodayViewModel] for pure read/observe access).
 * [EntitlementRepository.isReady] is re-checked live alongside [EntitlementRepository.observeAccess]
 * — a family opening this screen while RevenueCat is mid-retry (or never configured) sees
 * [FamilyPlusManagementUiState.customerCenterAvailable] flip to `true` the moment it becomes ready,
 * without needing to leave and reopen the screen.
 */
class FamilyPlusManagementViewModel(
    private val entitlementRepository: EntitlementRepository,
    private val restoreFamilyPlus: RestoreFamilyPlus,
    private val timeZoneProvider: AppTimeZoneProvider,
    private val analytics: ProductAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyPlusManagementUiState())
    val uiState: StateFlow<FamilyPlusManagementUiState> = _uiState.asStateFlow()

    private val _events = Channel<FamilyPlusManagementEvent>(Channel.BUFFERED)
    val events: Flow<FamilyPlusManagementEvent> = _events.receiveAsFlow()

    private var hasStarted = false

    fun onAction(action: FamilyPlusManagementAction) {
        when (action) {
            FamilyPlusManagementAction.ViewPlansClicked -> viewModelScope.launch { _events.send(FamilyPlusManagementEvent.OpenPaywall) }
            FamilyPlusManagementAction.RestoreClicked -> restore()
            FamilyPlusManagementAction.ManageSubscriptionClicked -> onManageSubscriptionClicked()
        }
    }

    fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true

        viewModelScope.launch {
            entitlementRepository.observeAccess().collect { result ->
                if (result is DataResult.Success) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            access = result.value.familyAccess,
                            renewalInfo = result.value.familyAccess.toRenewalInfo(timeZoneProvider.current()),
                            customerCenterAvailable = entitlementRepository.isReady(),
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, customerCenterAvailable = entitlementRepository.isReady()) }
                }
            }
        }
    }

    private fun onManageSubscriptionClicked() {
        viewModelScope.launch {
            if (_uiState.value.customerCenterAvailable) {
                _events.send(FamilyPlusManagementEvent.OpenCustomerCenter)
            } else {
                _uiState.update { it.copy(message = UiText.Resource(Res.string.familyplus_customer_center_unavailable)) }
            }
        }
    }

    private fun restore() {
        if (_uiState.value.isRestoring) return
        _uiState.update { it.copy(isRestoring = true, message = null) }
        analytics.capture(RestoreStarted(AnalyticsScreen.FAMILY_PLUS_MANAGEMENT))

        viewModelScope.launch {
            when (val result = restoreFamilyPlus()) {
                is RestoreResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            access = result.access.familyAccess,
                            renewalInfo = result.access.familyAccess.toRenewalInfo(timeZoneProvider.current()),
                            message = if (result.access.familyAccess.isPlus) {
                                UiText.Resource(Res.string.restore_succeeded_message)
                            } else {
                                UiText.Resource(Res.string.restore_no_purchases_found_message)
                            },
                        )
                    }
                    val outcome = if (result.access.familyAccess.isPlus) RestoreOutcomeResult.SUCCESS else RestoreOutcomeResult.NO_PURCHASES
                    analytics.capture(RestoreOutcome(AnalyticsScreen.FAMILY_PLUS_MANAGEMENT, outcome))
                }
                is RestoreResult.Failure -> {
                    _uiState.update {
                        it.copy(isRestoring = false, message = result.error.toUiText())
                    }
                    analytics.capture(RestoreOutcome(AnalyticsScreen.FAMILY_PLUS_MANAGEMENT, result.error.toRestoreOutcomeResult()))
                }
            }
        }
    }
}
