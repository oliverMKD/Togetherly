package com.togetherly.domain.completion.repository

import com.togetherly.core.error.AppError
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.map
import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.QuestCompletion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeCompletionRepository : CompletionRepository {

    private val activeSessionFlow = MutableStateFlow<DataResult<ActiveQuestSession?>>(DataResult.Success(null))
    private val completionsFlow = MutableStateFlow<DataResult<List<QuestCompletion>>>(DataResult.Success(emptyList()))

    private var nextError: AppError? = null
    private var saveCompletionError: AppError? = null
    private var saveCompletionThrowable: Throwable? = null
    private var deleteCompletionError: AppError? = null

    fun setNextError(error: AppError) {
        nextError = error
    }

    /**
     * Targets [saveCompletion] specifically, since [nextError] is consumed by whichever
     * operation runs next and use cases like [com.togetherly.domain.completion.usecase.CompleteQuest]
     * call [getActiveSession] before [saveCompletion] — a shared "next call" error can't target
     * a specific later step in that sequence.
     */
    fun setSaveCompletionError(error: AppError) {
        saveCompletionError = error
    }

    /**
     * Targets [deleteCompletion] specifically, the same reason [setSaveCompletionError] exists —
     * [com.togetherly.domain.completion.usecase.DeleteCompletion] calls [getCompletion] before
     * [deleteCompletion], so a shared "next call" error can't isolate a failure to the delete step.
     */
    fun setDeleteCompletionError(error: AppError) {
        deleteCompletionError = error
    }

    /**
     * Makes the next [saveCompletion] call throw [throwable] instead of returning a [DataResult] —
     * used to simulate coroutine cancellation reaching the database write, since a returned
     * [DataResult.Error] can't stand in for a thrown [kotlinx.coroutines.CancellationException].
     */
    fun throwOnNextSaveCompletion(throwable: Throwable) {
        saveCompletionThrowable = throwable
    }

    private fun consumeError(): AppError? {
        val error = nextError
        nextError = null
        return error
    }

    private fun currentCompletions(): List<QuestCompletion> =
        (completionsFlow.value as? DataResult.Success)?.value ?: emptyList()

    private fun emitCompletionsNewestFirst(completions: List<QuestCompletion>) {
        completionsFlow.value = DataResult.Success(completions.sortedByDescending { it.completedAt })
    }

    override fun observeActiveSession(): Flow<DataResult<ActiveQuestSession?>> = activeSessionFlow

    override suspend fun getActiveSession(): DataResult<ActiveQuestSession?> {
        consumeError()?.let { return DataResult.Error(it) }
        return activeSessionFlow.value
    }

    override suspend fun saveActiveSession(session: ActiveQuestSession): DataResult<Unit> {
        consumeError()?.let { return DataResult.Error(it) }
        activeSessionFlow.value = DataResult.Success(session)
        return DataResult.Success(Unit)
    }

    override suspend fun clearActiveSession(): DataResult<Unit> {
        consumeError()?.let { return DataResult.Error(it) }
        activeSessionFlow.value = DataResult.Success(null)
        return DataResult.Success(Unit)
    }

    override fun observeCompletions(): Flow<DataResult<List<QuestCompletion>>> = completionsFlow

    override fun observeCompletion(completionId: CompletionId): Flow<DataResult<QuestCompletion?>> =
        completionsFlow.map { result -> result.map { completions -> completions.find { it.id == completionId } } }

    override suspend fun getCompletions(): DataResult<List<QuestCompletion>> {
        consumeError()?.let { return DataResult.Error(it) }
        return completionsFlow.value
    }

    override suspend fun getCompletion(completionId: CompletionId): DataResult<QuestCompletion?> {
        consumeError()?.let { return DataResult.Error(it) }
        return completionsFlow.value.map { completions -> completions.find { it.id == completionId } }
    }

    override suspend fun saveCompletion(completion: QuestCompletion): DataResult<Unit> {
        saveCompletionThrowable?.let {
            saveCompletionThrowable = null
            throw it
        }
        saveCompletionError?.let {
            saveCompletionError = null
            return DataResult.Error(it)
        }
        consumeError()?.let { return DataResult.Error(it) }
        val withoutExisting = currentCompletions().filterNot { it.id == completion.id }
        emitCompletionsNewestFirst(withoutExisting + completion)
        return DataResult.Success(Unit)
    }

    override suspend fun deleteCompletion(completionId: CompletionId): DataResult<Unit> {
        deleteCompletionError?.let {
            deleteCompletionError = null
            return DataResult.Error(it)
        }
        consumeError()?.let { return DataResult.Error(it) }
        emitCompletionsNewestFirst(currentCompletions().filterNot { it.id == completionId })
        return DataResult.Success(Unit)
    }
}
