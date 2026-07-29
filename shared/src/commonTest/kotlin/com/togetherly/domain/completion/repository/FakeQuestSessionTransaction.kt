package com.togetherly.domain.completion.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.completion.QuestCompletion

/**
 * Mirrors [RoomQuestSessionTransaction][com.togetherly.data.completion.RoomQuestSessionTransaction]'s
 * conflict/mismatch semantics against the same [completionRepository] instance a test also holds,
 * so assertions against that fake still see the effects of a start/complete call. There is no real
 * concurrency to guard here (single-threaded test execution), so the "atomicity" is just doing the
 * check and the write as consecutive, uninterrupted statements.
 */
class FakeQuestSessionTransaction(
    private val completionRepository: FakeCompletionRepository,
) : QuestSessionTransaction {

    override suspend fun startActiveSession(session: ActiveQuestSession): DataResult<Unit> {
        val existingResult = completionRepository.getActiveSession()
        if (existingResult is DataResult.Error) return existingResult
        val existing = (existingResult as DataResult.Success).value

        return when {
            existing == null -> completionRepository.saveActiveSession(session)
            existing.completionId == session.completionId -> DataResult.Success(Unit)
            else -> DataResult.Error(AppError.Validation(ValidationError.ACTIVE_SESSION_CONFLICT))
        }
    }

    override suspend fun completeActiveSession(completion: QuestCompletion): DataResult<Unit> {
        val existingResult = completionRepository.getActiveSession()
        if (existingResult is DataResult.Error) return existingResult
        val existing = (existingResult as DataResult.Success).value

        if (existing == null || existing.completionId != completion.id) {
            return DataResult.Error(AppError.Validation(ValidationError.ACTIVE_SESSION_MISMATCH))
        }

        val saveResult = completionRepository.saveCompletion(completion)
        if (saveResult is DataResult.Error) return saveResult

        return completionRepository.clearActiveSession()
    }

    override suspend fun replaceActiveSession(session: ActiveQuestSession): DataResult<Unit> =
        completionRepository.saveActiveSession(session)
}
