package com.togetherly.domain.completion.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.repository.CompletionRepository
import com.togetherly.domain.completion.repository.PrivateMediaCommitter

/**
 * Deletes the Room row first, then — best-effort — the completion's own committed photo/voice
 * files, the same ordering [SaveCompletionMemory] uses when a replacement supersedes existing
 * media: a file is only ever removed once nothing in the database still references it. A crash
 * between the two steps can leave an orphaned file with no database reference, never the reverse
 * (a database reference to a now-missing file) — see [com.togetherly.data.media.PendingMediaOrphanCleaner]'s
 * own KDoc for why an orphaned file is the safe failure mode this codebase prefers (Step 10.7
 * privacy/filesystem audit; this use case previously left media on disk entirely, deferred since
 * Step 4.4 before [PrivateMediaCommitter] existed).
 *
 * A media deletion failure is not surfaced as an overall failure: the completion itself is
 * already gone by that point, and returning an error here would incorrectly suggest the deletion
 * as a whole didn't happen. An unreferenced file left behind by such a failure is exactly what a
 * future orphan/consistency sweep is for, not something this call site retries.
 */
class DeleteCompletion(
    private val completionRepository: CompletionRepository,
    private val mediaCommitter: PrivateMediaCommitter,
) {
    suspend operator fun invoke(completionId: CompletionId): DataResult<Unit> {
        val existing = when (val result = completionRepository.getCompletion(completionId)) {
            is DataResult.Error -> return result
            is DataResult.Success -> result.value
        }

        val deleteResult = completionRepository.deleteCompletion(completionId)
        if (deleteResult is DataResult.Error) return deleteResult

        existing?.media?.forEach { media -> mediaCommitter.deleteCommitted(media) }

        return DataResult.Success(Unit)
    }
}
