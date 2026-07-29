package com.togetherly.feature.family.model

sealed interface MemorySettingsAction {
    data class AllowPhotosChanged(val value: Boolean) : MemorySettingsAction
    data class AllowVoiceMemoriesChanged(val value: Boolean) : MemorySettingsAction
    data class AllowTextNotesChanged(val value: Boolean) : MemorySettingsAction
    data class ShowMemoryPromptAfterQuestsChanged(val value: Boolean) : MemorySettingsAction
    data object ManageMemoriesClicked : MemorySettingsAction
    data object SaveClicked : MemorySettingsAction
    data object BackClicked : MemorySettingsAction
    data object DiscardConfirmed : MemorySettingsAction
    data object DismissDiscardDialog : MemorySettingsAction
}
