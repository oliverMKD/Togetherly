package com.togetherly.domain.completion.repository

import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.completion.QuestCompletion

/**
 * The atomic, check-and-write boundary for the active-session lifecycle — the reason this exists
 * as its own contract rather than living on [CompletionRepository]: [StartQuest][com.togetherly.domain.completion.usecase.StartQuest]
 * and [CompleteQuest][com.togetherly.domain.completion.usecase.CompleteQuest] each need to check
 * the current active-session state and then write, and a use case doing that as two separate
 * repository calls is a real race (a second caller can act between the read and the write). Both
 * methods here perform their check and their write inside one database transaction, so no such
 * window exists — implementations must never fake this by simply calling
 * [CompletionRepository.getActiveSession] followed by a later, separate write.
 *
 * Both methods key off [ActiveQuestSession.completionId]/[QuestCompletion.id] — the same value,
 * assigned once when a session starts (see [com.togetherly.domain.completion.complete]) — never a
 * fabricated or fresh identity.
 */
interface QuestSessionTransaction {

    /**
     * Starts [session] as the active session. Succeeds (and writes) when no session is currently
     * active. Succeeds without writing again when the currently active session already has the
     * same [ActiveQuestSession.completionId] as [session] (a retried start is idempotent, never a
     * conflict). Returns [com.togetherly.core.error.ValidationError.ACTIVE_SESSION_CONFLICT] when
     * a *different* session is already active — it is never silently replaced.
     */
    suspend fun startActiveSession(
        session: ActiveQuestSession,
    ): DataResult<Unit>

    /**
     * Saves [completion] and clears the active session as one atomic step: both commit, or
     * neither does — a failure while saving the completion leaves the active session exactly as
     * it was, and the active session is never cleared without the completion having actually been
     * saved. First re-verifies that the active session this completion was built from
     * ([QuestCompletion.id] matching [ActiveQuestSession.completionId]) is still the one active;
     * if it changed or was cleared since the caller last read it, this returns
     * [com.togetherly.core.error.ValidationError.ACTIVE_SESSION_MISMATCH] instead of completing
     * (and clearing) a session the caller no longer has an accurate view of.
     */
    suspend fun completeActiveSession(
        completion: QuestCompletion,
    ): DataResult<Unit>

    /**
     * Unconditionally makes [session] the active one, replacing whatever was there — the explicit,
     * user-confirmed counterpart to [startActiveSession]'s conflict-preserving default (see
     * [com.togetherly.domain.completion.usecase.ReplaceActiveQuestSession]'s own KDoc for why that
     * has to be a separate call, never a hidden fallback inside [startActiveSession] itself). Still
     * runs inside one transaction so a concurrent reader never observes a half-written session.
     */
    suspend fun replaceActiveSession(
        session: ActiveQuestSession,
    ): DataResult<Unit>
}
