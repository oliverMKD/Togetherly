package com.togetherly.domain.daily.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.DismissedQuest

/**
 * Delegates to the same [dailyQuestRepository] instance a test also holds, so assertions against
 * that fake still see the effects of a reroll. No real concurrency to guard here (single-threaded
 * test execution) — the "atomicity" this proves is only that both writes happen, or (via
 * [setNextError]) neither does.
 */
class FakeDailyQuestTransaction(
    private val dailyQuestRepository: FakeDailyQuestRepository,
) : DailyQuestTransaction {

    private var nextError: AppError? = null

    fun setNextError(error: AppError) {
        nextError = error
    }

    override suspend fun replaceWithReroll(
        previous: DismissedQuest,
        replacement: DailyQuest,
    ): DataResult<Unit> {
        nextError?.let {
            nextError = null
            return DataResult.Error(it)
        }
        val dismissalResult = dailyQuestRepository.recordDismissal(previous)
        if (dismissalResult is DataResult.Error) return dismissalResult
        return dailyQuestRepository.saveDailyQuest(replacement)
    }
}
