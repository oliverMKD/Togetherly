package com.togetherly.domain.completion.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionMemoryDraft
import com.togetherly.domain.completion.repository.PrivateMediaCommitter

/**
 * Deletes only the pending (never-committed) files staged by [draft], if any — it has no
 * [CompletionRepository][com.togetherly.domain.completion.repository.CompletionRepository]
 * dependency at all, so it is structurally incapable of touching the persisted completion, and it
 * never sees or deletes already-committed media since a draft only ever references pending files.
 */
class DiscardCompletionMemoryDraft(
    private val mediaCommitter: PrivateMediaCommitter,
) {
    suspend operator fun invoke(draft: CompletionMemoryDraft): DataResult<Unit> {
        draft.pendingPhoto?.let { pendingPhoto ->
            val result = mediaCommitter.deletePending(pendingPhoto)
            if (result is DataResult.Error) return result
        }
        draft.pendingVoice?.let { pendingVoice ->
            val result = mediaCommitter.deletePending(pendingVoice.reference)
            if (result is DataResult.Error) return result
        }
        return DataResult.Success(Unit)
    }
}
