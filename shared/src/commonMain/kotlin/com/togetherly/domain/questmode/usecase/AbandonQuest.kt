package com.togetherly.domain.questmode.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.repository.CompletionRepository

/**
 * Explicitly clears the active session — never creates a
 * [com.togetherly.domain.completion.QuestCompletion], never touches the persisted
 * [com.togetherly.domain.daily.DailyQuest], and never records a
 * [com.togetherly.domain.daily.DismissedQuest]. Abandoning a quest is not the same as completing
 * it, rerolling it, or dismissing it — those are three separate, already-existing concerns this
 * use case deliberately does not reach into.
 */
class AbandonQuest(
    private val completionRepository: CompletionRepository,
) {
    suspend operator fun invoke(): DataResult<Unit> = completionRepository.clearActiveSession()
}
