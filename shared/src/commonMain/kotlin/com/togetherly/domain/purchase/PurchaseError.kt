package com.togetherly.domain.purchase

sealed interface PurchaseError {
    data object ProductUnavailable : PurchaseError
    data object StoreUnavailable : PurchaseError
    data object NetworkProblem : PurchaseError
    data object ConfigurationProblem : PurchaseError
    data object PurchaseNotAllowed : PurchaseError
    data object AlreadyOwned : PurchaseError
    data object Unknown : PurchaseError
}
