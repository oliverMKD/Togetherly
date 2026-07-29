package com.togetherly.feature.family.model

sealed interface OpenSourceLicensesAction {
    data object BackClicked : OpenSourceLicensesAction
    data class LicenseClicked(val license: OpenSourceLicense) : OpenSourceLicensesAction
}
