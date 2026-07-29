package com.togetherly.domain.questmode.usecase

import com.togetherly.core.datetime.AppClock
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.repository.CompletionRepository
import com.togetherly.domain.quest.repository.QuestRepository
import com.togetherly.domain.questmode.LoadQuestModeResult
import com.togetherly.domain.questmode.QuestModeSession
import com.togetherly.domain.questmode.QuestTimerPolicy

/**
 * The persisted active session is always authoritative — [completionId] (navigation-supplied) is
 * only ever compared against it, never trusted on its own to resolve a quest. This is what makes
 * [LoadQuestModeResult.SessionMismatch] possible in the first place: a caller can navigate here
 * with a stale or otherwise-wrong ID, and this use case must notice rather than silently loading
 * whatever session happens to be active.
 */
class LoadQuestMode(
    private val completionRepository: CompletionRepository,
    private val questRepository: QuestRepository,
    private val timerPolicy: QuestTimerPolicy,
    private val clock: AppClock,
) {
    suspend operator fun invoke(completionId: CompletionId): LoadQuestModeResult {
        val sessionResult = completionRepository.getActiveSession()
        if (sessionResult is DataResult.Error) return LoadQuestModeResult.Failure(sessionResult.error)
        val session = (sessionResult as DataResult.Success).value
            ?: return LoadQuestModeResult.NoActiveSession

        if (session.completionId != completionId) return LoadQuestModeResult.SessionMismatch

        val questResult = questRepository.getQuest(session.questId)
        if (questResult is DataResult.Error) return LoadQuestModeResult.Failure(questResult.error)
        val quest = (questResult as DataResult.Success).value
            ?: return LoadQuestModeResult.QuestUnavailable

        val timerState = timerPolicy.resolve(session, quest.timer, clock.now())

        return LoadQuestModeResult.Success(QuestModeSession(session, quest, timerState))
    }
}
