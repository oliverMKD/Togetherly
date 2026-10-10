package com.togetherly.feature.language.model

sealed interface LanguageEvent {
    data object NavigateBack : LanguageEvent
}
