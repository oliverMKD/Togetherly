package com.togetherly.feature.questdetail.model

import kotlinx.serialization.Serializable

/**
 * Where the user navigated to Quest Detail *from* — a nav argument on
 * [com.togetherly.navigation.destination.RootDestination.QuestDetail], not a domain concept
 * ([com.togetherly.domain.daily.DailyQuestSource] is a different, unrelated thing: why a *daily*
 * selection was chosen, not where a screen was opened from). Defaults to [TODAY] on the existing
 * destination shape so every pre-Explore call site keeps compiling unchanged; Explore's own future
 * screens are the first callers to pass [EXPLORE]/[PACK]/[SAVED] explicitly.
 */
@Serializable
enum class QuestOpenSource {
    TODAY,
    EXPLORE,
    PACK,
    SAVED,
    JOURNEY,
}
