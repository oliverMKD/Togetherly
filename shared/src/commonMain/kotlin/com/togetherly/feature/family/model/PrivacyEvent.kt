package com.togetherly.feature.family.model

sealed interface PrivacyEvent {
    data object NavigateBack : PrivacyEvent
}
