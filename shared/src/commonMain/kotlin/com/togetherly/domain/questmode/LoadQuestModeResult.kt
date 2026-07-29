package com.togetherly.domain.questmode

import com.togetherly.core.error.AppError

/**
 * [SessionMismatch] is the specific, non-generic case where a persisted active session exists but
 * its own [com.togetherly.domain.completion.ActiveQuestSession.completionId] doesn't match the ID
 * Quest Mode was navigated with — a stale route, a replaced session, or a completed-and-cleared one
 * reopened from an old back-stack entry. This is never silently treated as "load whatever session
 * *is* active" — see [com.togetherly.domain.questmode.usecase.LoadQuestMode]'s own KDoc.
 */
sealed interface LoadQuestModeResult {

    data class Success(
        val session: QuestModeSession,
    ) : LoadQuestModeResult

    data object NoActiveSession : LoadQuestModeResult

    data object SessionMismatch : LoadQuestModeResult

    data object QuestUnavailable : LoadQuestModeResult

    data class Failure(
        val error: AppError,
    ) : LoadQuestModeResult
}
