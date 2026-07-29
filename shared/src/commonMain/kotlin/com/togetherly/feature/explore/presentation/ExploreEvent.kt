package com.togetherly.feature.explore.presentation

import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId

/**
 * Navigation only ever passes typed IDs — never a domain model — same convention as every other
 * feature's own events. [OpenFilters] reaches [com.togetherly.navigation.destination.RootDestination.ExploreFilters]
 * (Step 12.1, real content Step 12.4). [OpenSaved] reaches [com.togetherly.navigation.destination.RootDestination.Saved]
 * (Step 12.4).
 */
sealed interface ExploreEvent {
    data class OpenQuestDetail(val questId: QuestId) : ExploreEvent
    data class OpenPackDetails(val packId: QuestPackId) : ExploreEvent
    data object OpenFilters : ExploreEvent
    data object OpenSaved : ExploreEvent
}
