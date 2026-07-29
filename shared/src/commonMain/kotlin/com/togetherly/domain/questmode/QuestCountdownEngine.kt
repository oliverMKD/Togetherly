package com.togetherly.domain.questmode

import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.quest.QuestTimer
import kotlinx.coroutines.flow.Flow

/**
 * A cold source of [QuestTimerState] snapshots for one [ActiveQuestSession]/[QuestTimer] pair —
 * collecting starts the tick loop, cancelling the collector stops it; there is no independent
 * lifecycle to manage. Never put a lifecycle-aware class here — the ViewModel (Step 9.3) owns
 * collection and decides when to start/stop it.
 */
interface QuestCountdownEngine {

    fun observe(
        session: ActiveQuestSession,
        timer: QuestTimer,
    ): Flow<QuestTimerState>
}
