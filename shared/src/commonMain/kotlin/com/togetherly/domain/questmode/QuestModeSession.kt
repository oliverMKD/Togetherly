package com.togetherly.domain.questmode

import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.quest.FamilyQuest

/**
 * Everything Quest Mode needs to render one screen — the persisted session (authoritative; see
 * [com.togetherly.domain.questmode.usecase.LoadQuestMode]'s own KDoc for why navigation-supplied
 * IDs are never trusted over it), the quest it's for, and the current timer snapshot.
 */
data class QuestModeSession(
    val activeSession: ActiveQuestSession,
    val quest: FamilyQuest,
    val timerState: QuestTimerState,
)
