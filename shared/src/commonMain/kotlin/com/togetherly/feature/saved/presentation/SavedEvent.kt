package com.togetherly.feature.saved.presentation

import com.togetherly.domain.quest.QuestId

sealed interface SavedEvent {
    data class OpenQuestDetail(val questId: QuestId) : SavedEvent
    data object NavigateBack : SavedEvent
}
