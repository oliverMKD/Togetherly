package com.togetherly.core.ui

import com.togetherly.core.error.AppError
import com.togetherly.domain.purchase.PurchaseError
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.purchase_error_already_owned
import togetherly.shared.generated.resources.purchase_error_configuration_problem
import togetherly.shared.generated.resources.purchase_error_network_problem
import togetherly.shared.generated.resources.purchase_error_not_allowed
import togetherly.shared.generated.resources.purchase_error_product_unavailable
import togetherly.shared.generated.resources.purchase_error_store_unavailable
import togetherly.shared.generated.resources.purchase_error_unknown

/**
 * Distinct from [AppError.toUiText] on purpose: [PurchaseError]'s cases are already
 * business-meaningful, user-nameable outcomes ("the store is unavailable", "you already own
 * this") rather than an internal technical reason a user shouldn't see — that's exactly the copy
 * Family Plus purchase surfaces need (see this feature's own task spec's message list), so this
 * mapper gives each branch its own string instead of collapsing to one generic message.
 */
fun PurchaseError.toUiText(): UiText = when (this) {
    PurchaseError.ProductUnavailable -> UiText.Resource(Res.string.purchase_error_product_unavailable)
    PurchaseError.StoreUnavailable -> UiText.Resource(Res.string.purchase_error_store_unavailable)
    PurchaseError.NetworkProblem -> UiText.Resource(Res.string.purchase_error_network_problem)
    PurchaseError.ConfigurationProblem -> UiText.Resource(Res.string.purchase_error_configuration_problem)
    PurchaseError.PurchaseNotAllowed -> UiText.Resource(Res.string.purchase_error_not_allowed)
    PurchaseError.AlreadyOwned -> UiText.Resource(Res.string.purchase_error_already_owned)
    PurchaseError.Unknown -> UiText.Resource(Res.string.purchase_error_unknown)
}

/**
 * [EntitlementRepository][com.togetherly.domain.purchase.repository.EntitlementRepository]'s
 * package/access reads report failures as a plain [AppError] (its own [DataResult][com.togetherly.core.result.DataResult]
 * contract), but [com.togetherly.data.purchase.DefaultRevenueCatDataSource] always wraps a real
 * failure as [AppError.Purchase] — this extracts that [PurchaseError] reason and maps it through
 * [PurchaseError.toUiText] (the specific "unable to load plans"/"store unavailable"/"connection
 * problem" copy this feature's own task spec asks for) rather than falling back to
 * [AppError.toUiText]'s single generic message, which would otherwise silently swallow the
 * distinction for every package-load or access-refresh failure.
 */
fun AppError.toPurchaseAwareUiText(): UiText = (this as? AppError.Purchase)?.reason?.toUiText() ?: toUiText()
