package com.togetherly.domain.questmode

/**
 * The two outcomes a future exit-confirmation UI (Step 9.3) can resolve to. Leaving Quest Mode via
 * Back keeps the session by default — [ABANDON] is never the implicit outcome of navigating away;
 * it requires the family to explicitly choose it, then a dedicated confirmation, then
 * [com.togetherly.domain.questmode.usecase.AbandonQuest].
 */
enum class QuestModeExitChoice {
    KEEP_IN_PROGRESS,
    ABANDON,
}
