package com.togetherly.domain.daily.repository

import com.togetherly.core.result.DataResult
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.DismissedQuest

/**
 * The atomic boundary for a reroll's dismiss-and-replace step — the reason this exists as its own
 * contract rather than two separate [DailyQuestRepository] calls: recording [previous] as
 * dismissed and saving [replacement] as the new selection must commit together or not at all.
 * [RerollDailyQuest][com.togetherly.domain.daily.usecase.RerollDailyQuest] never records the
 * dismissal via [DailyQuestRepository.recordDismissal] directly for this reason — a crash or write
 * failure between two separate calls would leave a quest dismissed with no replacement selected,
 * silently reverting today's selection to nothing. A failed recommendation never reaches this
 * boundary at all; it's called only once a replacement is already chosen.
 */
interface DailyQuestTransaction {

    suspend fun replaceWithReroll(
        previous: DismissedQuest,
        replacement: DailyQuest,
    ): DataResult<Unit>
}
