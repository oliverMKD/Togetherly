package com.togetherly.feature.paywall.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.PaywallDismissed
import com.togetherly.core.telemetry.PaywallPresented
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.telemetry.PurchaseOutcome
import com.togetherly.core.telemetry.PurchaseOutcomeResult
import com.togetherly.core.telemetry.PurchaseStarted
import com.togetherly.core.telemetry.RestoreOutcome
import com.togetherly.core.telemetry.RestoreOutcomeResult
import com.togetherly.core.telemetry.RestoreStarted
import com.togetherly.core.telemetry.toPurchaseOutcomeResult
import com.togetherly.core.telemetry.toRestoreOutcomeResult
import com.togetherly.core.ui.UiText
import com.togetherly.core.ui.toPurchaseAwareUiText
import com.togetherly.core.ui.toUiText
import com.togetherly.domain.purchase.ProductId
import com.togetherly.domain.purchase.PurchasePackage
import com.togetherly.domain.purchase.PurchasePackageType
import com.togetherly.domain.purchase.PurchaseResult
import com.togetherly.domain.purchase.RestoreResult
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.purchase.usecase.PurchaseFamilyPlus
import com.togetherly.domain.purchase.usecase.RestoreFamilyPlus
import com.togetherly.feature.paywall.model.FamilyPlusPaywallUiState
import com.togetherly.feature.paywall.model.PaywallContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.toPersistentList
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.paywall_intro_premium_pack
import togetherly.shared.generated.resources.paywall_intro_premium_quest
import togetherly.shared.generated.resources.paywall_intro_premium_reroll
import togetherly.shared.generated.resources.purchase_cancelled_message
import togetherly.shared.generated.resources.purchase_pending_message
import togetherly.shared.generated.resources.restore_no_purchases_found_message

/**
 * [EntitlementRepository] is injected directly (not wrapped in a pass-through observe/get-packages
 * use case) — this codebase's existing convention for pure read/observe access (see
 * [com.togetherly.feature.today.presentation.TodayViewModel]'s own direct [com.togetherly.domain.family.repository.FamilyRepository]/
 * [com.togetherly.domain.saved.repository.SavedQuestRepository] injection). [purchaseFamilyPlus]/
 * [restoreFamilyPlus] stay as use cases since they carry real behavior beyond a forward (see their
 * own KDoc).
 *
 * [access] is kept live for as long as this screen is open via [EntitlementRepository.observeAccess]
 * — if Family Plus activates from anywhere else while this paywall happens to be open (another
 * purchase flow, a push update from RevenueCat), the screen reflects it without the family needing
 * to tap Purchase again.
 *
 * [context] (Step 11.6) only ever changes [FamilyPlusPaywallUiState.introMessage] — resolved once
 * at construction via [toIntroMessage], never re-read afterward. Every context still purchases
 * against the same current offering; see [PaywallContext]'s own KDoc.
 */
class FamilyPlusPaywallViewModel(
    private val context: PaywallContext,
    private val entitlementRepository: EntitlementRepository,
    private val purchaseFamilyPlus: PurchaseFamilyPlus,
    private val restoreFamilyPlus: RestoreFamilyPlus,
    private val analytics: ProductAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyPlusPaywallUiState(introMessage = context.toIntroMessage()))
    val uiState: StateFlow<FamilyPlusPaywallUiState> = _uiState.asStateFlow()

    private val _events = Channel<FamilyPlusPaywallEvent>(Channel.BUFFERED)
    val events: Flow<FamilyPlusPaywallEvent> = _events.receiveAsFlow()

    private var hasStarted = false

    fun onAction(action: FamilyPlusPaywallAction) {
        when (action) {
            is FamilyPlusPaywallAction.PackageSelected -> _uiState.update { it.copy(selectedPackageId = action.productId) }
            FamilyPlusPaywallAction.PurchaseClicked -> purchase()
            FamilyPlusPaywallAction.RestoreClicked -> restore()
            FamilyPlusPaywallAction.RetryClicked -> loadPackages()
            FamilyPlusPaywallAction.CloseClicked -> {
                analytics.capture(
                    PaywallDismissed(
                        context = context,
                        sourceScreen = AnalyticsScreen.PAYWALL,
                        offeringIdentifier = currentOfferingIdentifier(),
                        availablePackageTypes = currentAvailablePackageTypes(),
                    ),
                )
                viewModelScope.launch { _events.send(FamilyPlusPaywallEvent.Close) }
            }
            FamilyPlusPaywallAction.PrivacyPolicyClicked -> viewModelScope.launch {
                _events.send(FamilyPlusPaywallEvent.OpenExternalLink(PRIVACY_POLICY_URL))
            }
            FamilyPlusPaywallAction.TermsClicked -> viewModelScope.launch {
                _events.send(FamilyPlusPaywallEvent.OpenExternalLink(TERMS_OF_USE_URL))
            }
        }
    }

    fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true
        analytics.capture(
            PaywallPresented(
                context = context,
                sourceScreen = AnalyticsScreen.PAYWALL,
                offeringIdentifier = currentOfferingIdentifier(),
                availablePackageTypes = currentAvailablePackageTypes(),
            ),
        )

        viewModelScope.launch {
            entitlementRepository.observeAccess().collect { result ->
                if (result is DataResult.Success) {
                    _uiState.update { it.copy(access = result.value.familyAccess) }
                }
            }
        }
        loadPackages()
    }

    private fun loadPackages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = entitlementRepository.getPackages()) {
                is DataResult.Success -> _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        packages = result.value.toPersistentList(),
                        selectedPackageId = current.selectedPackageId ?: defaultSelection(result.value),
                        error = null,
                    )
                }
                is DataResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.error.toPurchaseAwareUiText()) }
            }
        }
    }

    /**
     * [FamilyPlusPaywallUiState.isPurchasing] is set synchronously here, before [viewModelScope.launch]
     * ever schedules the actual call — not inside the launched coroutine — so a second call arriving
     * before the first coroutine has even started (a recomposition re-dispatch, two rapid taps) sees
     * the flag already set and returns immediately, rather than both slipping past the guard and
     * each scheduling their own [purchaseFamilyPlus] call. [com.togetherly.data.purchase.RevenueCatEntitlementRepository.purchase]
     * still coalesces concurrent calls at the provider boundary as a second line of defense.
     */
    private fun purchase() {
        val productId = _uiState.value.selectedPackageId ?: return
        if (_uiState.value.isPurchasing) return
        val selectedPackage = _uiState.value.packages.firstOrNull { it.productId == productId }
        val packageType = selectedPackage?.type
        val offeringIdentifier = currentOfferingIdentifier()
        val availableTypes = currentAvailablePackageTypes()
        _uiState.update { it.copy(isPurchasing = true, error = null) }
        if (packageType != null && offeringIdentifier != null) {
            analytics.capture(
                PurchaseStarted(
                    context = context,
                    sourceScreen = AnalyticsScreen.PAYWALL,
                    packageType = packageType,
                    offeringIdentifier = offeringIdentifier,
                    availablePackageTypes = availableTypes,
                ),
            )
        }

        viewModelScope.launch {
            fun captureOutcome(result: PurchaseOutcomeResult) {
                analytics.capture(
                    PurchaseOutcome(
                        context = context,
                        sourceScreen = AnalyticsScreen.PAYWALL,
                        packageType = packageType,
                        result = result,
                        offeringIdentifier = offeringIdentifier,
                        availablePackageTypes = availableTypes,
                    ),
                )
            }

            when (val result = purchaseFamilyPlus(productId)) {
                is PurchaseResult.Success -> {
                    _uiState.update { it.copy(isPurchasing = false, access = result.access) }
                    captureOutcome(PurchaseOutcomeResult.SUCCESS)
                    _events.send(FamilyPlusPaywallEvent.PurchaseSucceeded)
                }
                PurchaseResult.Cancelled -> {
                    _uiState.update {
                        it.copy(isPurchasing = false, error = UiText.Resource(Res.string.purchase_cancelled_message))
                    }
                    captureOutcome(PurchaseOutcomeResult.CANCELLED)
                }
                is PurchaseResult.Pending -> {
                    _uiState.update {
                        it.copy(isPurchasing = false, error = UiText.Resource(Res.string.purchase_pending_message))
                    }
                    captureOutcome(PurchaseOutcomeResult.PENDING)
                }
                is PurchaseResult.Failure -> {
                    _uiState.update {
                        it.copy(isPurchasing = false, error = result.error.toUiText())
                    }
                    captureOutcome(result.error.toPurchaseOutcomeResult())
                }
            }
        }
    }

    private fun restore() {
        if (_uiState.value.isRestoring) return
        _uiState.update { it.copy(isRestoring = true, error = null) }
        analytics.capture(RestoreStarted(AnalyticsScreen.PAYWALL))

        viewModelScope.launch {
            when (val result = restoreFamilyPlus()) {
                is RestoreResult.Success -> {
                    _uiState.update { it.copy(isRestoring = false, access = result.access.familyAccess) }
                    if (result.access.familyAccess.isPlus) {
                        analytics.capture(RestoreOutcome(AnalyticsScreen.PAYWALL, RestoreOutcomeResult.SUCCESS))
                        _events.send(FamilyPlusPaywallEvent.RestoreSucceeded)
                    } else {
                        analytics.capture(RestoreOutcome(AnalyticsScreen.PAYWALL, RestoreOutcomeResult.NO_PURCHASES))
                        _uiState.update { it.copy(error = UiText.Resource(Res.string.restore_no_purchases_found_message)) }
                    }
                }
                is RestoreResult.Failure -> {
                    _uiState.update {
                        it.copy(isRestoring = false, error = result.error.toUiText())
                    }
                    analytics.capture(RestoreOutcome(AnalyticsScreen.PAYWALL, result.error.toRestoreOutcomeResult()))
                }
            }
        }
    }

    /** Annual is the recommended default when it's on offer — otherwise the first available package. */
    private fun defaultSelection(packages: List<PurchasePackage>): ProductId? =
        packages.firstOrNull { it.type == PurchasePackageType.ANNUAL }?.productId ?: packages.firstOrNull()?.productId

    private fun currentOfferingIdentifier(): String? = _uiState.value.packages.firstOrNull()?.offeringIdentifier

    private fun currentAvailablePackageTypes(): Set<PurchasePackageType> = _uiState.value.packages.map { it.type }.toSet()
}

// TODO(revenuecat-setup): placeholder legal URLs — replace with Togetherly's real, published
// privacy policy and terms of use pages before shipping (see docs/revenuecat-setup.md).
private const val PRIVACY_POLICY_URL = "https://togetherly.app/privacy"
private const val TERMS_OF_USE_URL = "https://togetherly.app/terms"

private fun PaywallContext.toIntroMessage(): UiText? = when (this) {
    PaywallContext.PREMIUM_REROLL -> UiText.Resource(Res.string.paywall_intro_premium_reroll)
    PaywallContext.PREMIUM_QUEST -> UiText.Resource(Res.string.paywall_intro_premium_quest)
    PaywallContext.PREMIUM_PACK -> UiText.Resource(Res.string.paywall_intro_premium_pack)
    PaywallContext.FAMILY_PLUS_MANAGEMENT -> null
}
