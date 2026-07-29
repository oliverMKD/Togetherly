package com.togetherly.domain.completion.usecase

import com.togetherly.core.datetime.AppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.domain.completion.MemoryNote
import com.togetherly.domain.completion.QuestCompletion
import com.togetherly.domain.completion.complete
import com.togetherly.domain.completion.repository.CompletionRepository
import com.togetherly.domain.completion.repository.QuestSessionTransaction

/**
 * Reads the active session first (to build [QuestCompletion] from it), then commits through
 * [questSessionTransaction] — saving the completion and clearing the active session as one atomic
 * step (see [QuestSessionTransaction.completeActiveSession]'s own KDoc). That initial read and the
 * eventual write are still two separate calls, so [questSessionTransaction] re-verifies the active
 * session hasn't changed in between before committing anything; if it has, this returns
 * [AppError.Validation] with [ValidationError.ACTIVE_SESSION_MISMATCH] rather than completing (and
 * clearing) a session this call no longer has an accurate view of. A failure while saving leaves
 * the active session exactly as it was — there is no partially-applied outcome to reason about.
 */
class CompleteQuest(
    private val completionRepository: CompletionRepository,
    private val questSessionTransaction: QuestSessionTransaction,
    private val clock: AppClock,
) {
    suspend operator fun invoke(
        note: MemoryNote? = null,
        reactions: Set<FamilyReaction> = emptySet(),
    ): DataResult<QuestCompletion> {
        val sessionResult = completionRepository.getActiveSession()
        if (sessionResult is DataResult.Error) return sessionResult
        val session = (sessionResult as DataResult.Success).value
            ?: return DataResult.Error(AppError.Validation(ValidationError.NO_ACTIVE_SESSION))

        val completion = session.complete(
            completedAt = clock.now(),
            note = note,
            reactions = reactions,
        )

        val completeResult = questSessionTransaction.completeActiveSession(completion)
        if (completeResult is DataResult.Error) return completeResult

        return DataResult.Success(completion)
    }
}
