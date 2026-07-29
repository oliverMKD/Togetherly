package com.togetherly.domain.completion.usecase

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.repository.CompletionRepository

/**
 * Answers "has this exact completion ID already been persisted?" — the idempotency check Quest
 * Mode needs whenever it loads with a [CompletionId] that no longer matches an active session (a
 * double-navigation, a process recreated after [CompleteQuest] already succeeded, or a stale
 * back-stack entry reopened after the fact). [com.togetherly.domain.questmode.usecase.LoadQuestMode]
 * itself never performs this check — it only ever answers "is this the currently active session,"
 * which is exactly why this exists as its own, separate use case: a completed session is by
 * definition no longer active, so [LoadQuestMode] alone can't distinguish "never existed / was
 * abandoned" from "already completed successfully." Never creates a completion itself — read-only.
 */
sealed interface CompletionTransitionResult {
    data class AlreadyCompleted(val completionId: CompletionId) : CompletionTransitionResult
    data object NotCompleted : CompletionTransitionResult
    data class Failure(val error: AppError) : CompletionTransitionResult
}

class ResolveCompletionTransition(
    private val completionRepository: CompletionRepository,
) {
    suspend operator fun invoke(completionId: CompletionId): CompletionTransitionResult {
        return when (val result = completionRepository.getCompletion(completionId)) {
            is DataResult.Error -> CompletionTransitionResult.Failure(result.error)
            is DataResult.Success -> if (result.value != null) {
                CompletionTransitionResult.AlreadyCompleted(completionId)
            } else {
                CompletionTransitionResult.NotCompleted
            }
        }
    }
}
