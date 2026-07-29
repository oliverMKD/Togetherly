package com.togetherly.feature.family.model

sealed interface LegalAction {
    data object BackClicked : LegalAction
    data object PrivacyPolicyClicked : LegalAction
    data object TermsOfUseClicked : LegalAction
    data object SubscriptionTermsClicked : LegalAction
    data object OpenSourceLicensesClicked : LegalAction
}
