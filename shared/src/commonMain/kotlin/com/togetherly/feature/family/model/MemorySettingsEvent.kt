package com.togetherly.feature.family.model

sealed interface MemorySettingsEvent {
    data object OpenManageMemories : MemorySettingsEvent
    data object SaveCompleted : MemorySettingsEvent
    data object NavigatedBackWithoutSaving : MemorySettingsEvent
}
