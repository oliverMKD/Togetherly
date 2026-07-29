package com.togetherly.domain.completion.repository

import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.QuestCompletion
import kotlinx.coroutines.flow.Flow

/**
 * MVP supports exactly one active quest session at a time — [saveActiveSession] replaces
 * whatever session already exists. [clearActiveSession] is idempotent.
 *
 * [saveCompletion] creates or replaces by [CompletionId]; [deleteCompletion] on an unknown ID
 * is idempotent and does not perform media cleanup — a completion's [QuestCompletion.media]
 * references are left for a future dedicated deletion use case to handle.
 * [observeCompletions]/[getCompletions] return completions ordered newest first by
 * [QuestCompletion.completedAt].
 *
 * Turning an active session into a saved completion and then clearing the active session are
 * two separate operations here, not one fused step — coordinating that sequence (and rolling
 * back correctly if one half fails) is a future use case's responsibility, not an undocumented
 * side effect of either method.
 */
interface CompletionRepository {

    fun observeActiveSession(): Flow<DataResult<ActiveQuestSession?>>

    suspend fun getActiveSession(): DataResult<ActiveQuestSession?>

    suspend fun saveActiveSession(
        session: ActiveQuestSession,
    ): DataResult<Unit>

    suspend fun clearActiveSession(): DataResult<Unit>

    fun observeCompletions(): Flow<DataResult<List<QuestCompletion>>>

    fun observeCompletion(
        completionId: CompletionId,
    ): Flow<DataResult<QuestCompletion?>>

    suspend fun getCompletions(): DataResult<List<QuestCompletion>>

    suspend fun getCompletion(
        completionId: CompletionId,
    ): DataResult<QuestCompletion?>

    suspend fun saveCompletion(
        completion: QuestCompletion,
    ): DataResult<Unit>

    suspend fun deleteCompletion(
        completionId: CompletionId,
    ): DataResult<Unit>
}
