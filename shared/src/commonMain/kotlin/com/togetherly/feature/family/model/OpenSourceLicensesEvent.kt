package com.togetherly.feature.family.model

sealed interface OpenSourceLicensesEvent {
    data object NavigateBack : OpenSourceLicensesEvent
    data class OpenExternalUrl(val url: String) : OpenSourceLicensesEvent
}
