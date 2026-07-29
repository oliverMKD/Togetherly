package com.togetherly.domain.localdata.usecase

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.repository.CompletionRepository
import com.togetherly.domain.completion.repository.PrivateMediaCommitter
import com.togetherly.domain.family.repository.QuestHistoryCleaner
import kotlinx.coroutines.sync.Mutex

/**
 * "Reset quest history" (Step 13.7) — same read-then-wipe-then-best-effort-delete-files ordering
 * as [DeleteMemories], but wipes daily selections, dismissals, the active session, and every
 * completion (via [QuestHistoryCleaner]) rather than just memory content. Saved quests, the family
 * profile, and every family preference are deliberately preserved — see [QuestHistoryCleaner]'s
 * own KDoc for why saved quests specifically are not treated as history here.
 *
 * Returns the count of individual file deletions that failed — see [DeleteMemories]'s own KDoc for
 * why a non-zero count still means overall [DataResult.Success].
 */
class ResetQuestHistory(
    private val completionRepository: CompletionRepository,
    private val questHistoryCleaner: QuestHistoryCleaner,
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

            val resetResult = questHistoryCleaner.resetQuestHistory()
            if (resetResult is DataResult.Error) return resetResult

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
