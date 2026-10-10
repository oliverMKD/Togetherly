package com.togetherly.feature.language.model

sealed interface LanguageAction {
    data class LanguageSelected(val code: String?) : LanguageAction
    data object RestartPromptDismissed : LanguageAction
    data object BackClicked : LanguageAction
}
