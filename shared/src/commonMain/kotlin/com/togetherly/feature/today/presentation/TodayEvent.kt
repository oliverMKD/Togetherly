package com.togetherly.feature.today.presentation

import com.togetherly.domain.quest.QuestId

/**
 * One-off effects only — never a substitute for a state field (same rule as
 * [com.togetherly.feature.onboarding.presentation.OnboardingEvent]).
 *
 * Today's primary action opens quest detail; Quest Detail owns Start Quest (Step 8.6). So
 * [TodayAction.StartClicked] emits [OpenQuestDetail], never a separate "start" event — there is
 * deliberately no `StartQuest` event here despite [TodayAction.StartClicked]'s name, since the
 * detail screen is where Start actually happens.
 */
sealed interface TodayEvent {
    data class OpenQuestDetail(val questId: QuestId) : TodayEvent
    data object RerollLimitReached : TodayEvent
}
