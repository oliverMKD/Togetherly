package com.togetherly.domain.localdata.usecase

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.repository.CompletionRepository
import com.togetherly.domain.completion.repository.MemoryCleaner
import com.togetherly.domain.completion.repository.PrivateMediaCommitter
import kotlinx.coroutines.sync.Mutex

/**
 * "Delete memories" (Step 13.7) — reads every completion's [com.togetherly.domain.completion.MemoryMedia]
 * *before* clearing anything (so the files can still be found afterward), clears memory content via
 * [MemoryCleaner] (database rows: notes + media metadata — the source of truth), then best-effort
 * deletes each referenced file via [mediaCommitter] — the same "database row first, file
 * best-effort after" ordering [com.togetherly.domain.completion.usecase.DeleteCompletion] already
 * establishes, so a crash between the two steps can only ever leave an orphaned file, never a
 * database reference to a missing one.
 *
 * Returns the count of individual file deletions that failed — 0 means fully clean. A non-zero
 * count is still an overall [DataResult.Success]: the database rows (the data this action's own
 * name promises to delete) are unconditionally gone by that point, so returning an error here
 * would incorrectly suggest the deletion as a whole didn't happen. A leftover file this leaves
 * behind is exactly what [com.togetherly.data.media.PendingMediaOrphanCleaner]-style sweeps exist
 * for, not something this call site retries.
 *
 * Never touches [com.togetherly.domain.completion.QuestCompletion] rows themselves, reactions,
 * saved quests, the family profile, or RevenueCat purchase history — see [MemoryCleaner]'s own
 * KDoc.
 *
 * [mutex] rejects a concurrent invocation with [ValidationError.INVALID_STATE] rather than
 * queuing it — this app has no legitimate reason to run two memory deletions at once, so the
 * second caller (a duplicate tap the UI's own busy-state should already prevent) is told to back
 * off instead of silently interleaving with the first.
 */
class DeleteMemories(
    private val completionRepository: CompletionRepository,
    private val memoryCleaner: MemoryCleaner,
    private val mediaCommitter: PrivateMediaCommitter,
) {
    private val mutex = Mutex()

    suspend operator fun invoke(): DataResult<Int> {
        if (!mutex.tryLock()) {
            return DataResult.Error(AppError.Validation(ValidationError.INVALID_STATE))
        }
        try {
            val existing = when (val result = completionRepository.getCompletions()) {
                is DataResult.Error -> return result
                is DataResult.Success -> result.value
            }
            val mediaToDelete = existing.flatMap { it.media }

            val clearResult = memoryCleaner.clearAllMemoryContent()
            if (clearResult is DataResult.Error) return clearResult

            var failedFileDeletions = 0
            mediaToDelete.forEach { media ->
                if (mediaCommitter.deleteCommitted(media) is DataResult.Error) failedFileDeletions++
            }

            return DataResult.Success(failedFileDeletions)
        } finally {
            mutex.unlock()
        }
    }
}
