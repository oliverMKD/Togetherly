package com.togetherly.feature.language.model

import androidx.compose.runtime.Immutable

/** [selectedCode] mirrors [com.togetherly.core.locale.AppLocaleController.language] — `null` means "follow system". */
@Immutable
data class LanguageUiState(
    val selectedCode: String? = null,
    val showRestartPrompt: Boolean = false,
)
