package com.togetherly.feature.memory.presentation

import com.togetherly.domain.completion.CompletionId

sealed interface CompletionMemoryEvent {
    data object LaunchPhotoPicker : CompletionMemoryEvent
    data object RequestMicrophonePermission : CompletionMemoryEvent
    data class MemorySaved(val completionId: CompletionId) : CompletionMemoryEvent
    data object MemorySkipped : CompletionMemoryEvent
    data object NavigateBack : CompletionMemoryEvent
}
