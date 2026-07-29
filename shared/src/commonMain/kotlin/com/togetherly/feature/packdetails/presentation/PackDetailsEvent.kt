package com.togetherly.feature.packdetails.presentation

import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId

/**
 * [OpenPaywall] carries [QuestPackId] only — the nav layer converts it to the plain `String`
 * [com.togetherly.navigation.destination.RootDestination.FamilyPlusPaywall.packId] expects, the
 * same boundary conversion [com.togetherly.feature.questdetail.presentation.QuestDetailEvent.OpenPaywall]
 * already does for its own `questId`. Purchasing/parental-gating both happen outside this feature —
 * see this feature's own task spec ("Do not start a purchase directly from Pack Details", "Do not
 * duplicate RevenueCat logic").
 */
sealed interface PackDetailsEvent {
    data object NavigateBack : PackDetailsEvent
    data class OpenQuestDetail(val questId: QuestId) : PackDetailsEvent
    data class OpenPaywall(val packId: QuestPackId) : PackDetailsEvent
}
