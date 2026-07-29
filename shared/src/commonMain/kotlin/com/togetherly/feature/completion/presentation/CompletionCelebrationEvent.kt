package com.togetherly.feature.completion.presentation

import com.togetherly.domain.completion.CompletionId

sealed interface CompletionCelebrationEvent {
    data object NavigateToToday : CompletionCelebrationEvent
    data class NavigateToMemory(val completionId: CompletionId) : CompletionCelebrationEvent
}
