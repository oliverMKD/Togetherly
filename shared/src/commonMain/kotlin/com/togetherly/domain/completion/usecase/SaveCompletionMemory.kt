package com.togetherly.domain.completion.usecase

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.id.IdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.onError
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MediaEdit
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.PendingMediaReference
import com.togetherly.domain.completion.PendingVoiceReference
import com.togetherly.domain.completion.QuestCompletion
import com.togetherly.domain.completion.SaveCompletionMemoryCommand
import com.togetherly.domain.completion.normalizeMemoryNote
import com.togetherly.domain.completion.repository.CompletionRepository
import com.togetherly.domain.completion.repository.PrivateMediaCommitter
import com.togetherly.domain.validation.DomainValidationException
import kotlinx.coroutines.CancellationException

/**
 * Bundles the error actually returned from a failed save with any failures hit while attempting
 * best-effort compensating cleanup, so a cleanup failure can never silently overwrite or replace
 * [primary] — there is no lower-level diagnostics/log sink in this codebase yet, so
 * [cleanupFailures] exists to make that invariant explicit in code rather than assumed. Wiring
 * these into real diagnostics (without ever surfacing them as the returned error) is future work.
 */
internal data class CompensatingCleanupOutcome(
    val primary: AppError,
    val cleanupFailures: List<AppError>,
)

/**
 * Photo/voice commits (filesystem writes) and [CompletionRepository.saveCompletion] (a Room
 * transaction) cannot share one native transaction, so this use case uses compensating cleanup
 * instead: media is committed first, and only if every commit *and* the database write succeed
 * is any previously-committed (now superseded) media deleted. If a commit or the database write
 * fails partway through, everything newly committed *during this call* is deleted again and the
 * database is left untouched — an existing, unrelated committed memory is never touched by a
 * failed replacement. Cancellation is treated the same as any other failure: whatever was newly
 * committed is cleaned up best-effort before the [CancellationException] is rethrown.
 */
class SaveCompletionMemory(
    private val completionRepository: CompletionRepository,
    private val mediaCommitter: PrivateMediaCommitter,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(command: SaveCompletionMemoryCommand): DataResult<QuestCompletion> {
        val existing = when (val result = completionRepository.getCompletion(command.completionId)) {
            is DataResult.Error -> return result
            is DataResult.Success -> result.value
                ?: return DataResult.Error(AppError.Validation(ValidationError.COMPLETION_NOT_FOUND))
        }

        val note = try {
            normalizeMemoryNote(command.note)
        } catch (e: DomainValidationException) {
            return DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
        }

        val existingPhoto = existing.media.filterIsInstance<MemoryMedia.Photo>().firstOrNull()
        val existingVoice = existing.media.filterIsInstance<MemoryMedia.Voice>().firstOrNull()
        val newlyCommitted = mutableListOf<MemoryMedia>()

        try {
            val photo = when (val result = resolvePhoto(command.photo, existing.id, existingPhoto, newlyCommitted)) {
                is DataResult.Error -> return DataResult.Error(cleanUpAfterFailure(result.error, newlyCommitted).primary)
                is DataResult.Success -> result.value
            }

            val voice = when (val result = resolveVoice(command.voice, existing.id, existingVoice, newlyCommitted)) {
                is DataResult.Error -> return DataResult.Error(cleanUpAfterFailure(result.error, newlyCommitted).primary)
                is DataResult.Success -> result.value
            }

            val updated = existing.copy(
                note = note,
                reactions = command.reactions,
                media = listOfNotNull(photo, voice),
            )

            val saveResult = completionRepository.saveCompletion(updated)
            if (saveResult is DataResult.Error) {
                return DataResult.Error(cleanUpAfterFailure(saveResult.error, newlyCommitted).primary)
            }

            deleteIfSuperseded(command.photo, existingPhoto)
            deleteIfSuperseded(command.voice, existingVoice)

            return DataResult.Success(updated)
        } catch (cancellation: CancellationException) {
            newlyCommitted.forEach { media -> runCatching { mediaCommitter.deleteCommitted(media) } }
            throw cancellation
        }
    }

    private suspend fun resolvePhoto(
        edit: MediaEdit<PendingMediaReference>,
        completionId: CompletionId,
        existingPhoto: MemoryMedia.Photo?,
        newlyCommitted: MutableList<MemoryMedia>,
    ): DataResult<MemoryMedia.Photo?> = when (edit) {
        MediaEdit.Unchanged -> DataResult.Success(existingPhoto)
        MediaEdit.Remove -> DataResult.Success(null)
        is MediaEdit.Replace -> {
            val mediaId = MemoryMediaId(idGenerator.generate())
            when (val result = mediaCommitter.commitPhoto(edit.value, completionId, mediaId)) {
                is DataResult.Error -> result
                is DataResult.Success -> {
                    newlyCommitted += result.value
                    DataResult.Success(result.value)
                }
            }
        }
    }

    private suspend fun resolveVoice(
        edit: MediaEdit<PendingVoiceReference>,
        completionId: CompletionId,
        existingVoice: MemoryMedia.Voice?,
        newlyCommitted: MutableList<MemoryMedia>,
    ): DataResult<MemoryMedia.Voice?> = when (edit) {
        MediaEdit.Unchanged -> DataResult.Success(existingVoice)
        MediaEdit.Remove -> DataResult.Success(null)
        is MediaEdit.Replace -> {
            val mediaId = MemoryMediaId(idGenerator.generate())
            when (val result = mediaCommitter.commitVoice(edit.value, completionId, mediaId)) {
                is DataResult.Error -> result
                is DataResult.Success -> {
                    newlyCommitted += result.value
                    DataResult.Success(result.value)
                }
            }
        }
    }

    /**
     * Deletes the previously committed file only once the database has already been updated to
     * no longer reference it — called solely on the success path, after [CompletionRepository]
     * confirms [updated][QuestCompletion] is persisted, so a superseded file is only ever removed
     * once nothing in storage still points to it. Its own failure is left for a future orphan
     * scan (see the private-storage cleanup work); it does not affect the already-successful save.
     */
    private suspend fun deleteIfSuperseded(edit: MediaEdit<*>, existing: MemoryMedia?) {
        if (existing == null) return
        val isSuperseded = edit is MediaEdit.Remove || edit is MediaEdit.Replace<*>
        if (isSuperseded) {
            mediaCommitter.deleteCommitted(existing)
        }
    }

    private suspend fun cleanUpAfterFailure(
        primary: AppError,
        newlyCommitted: List<MemoryMedia>,
    ): CompensatingCleanupOutcome {
        val cleanupFailures = mutableListOf<AppError>()
        newlyCommitted.forEach { media ->
            mediaCommitter.deleteCommitted(media).onError { cleanupFailures += it }
        }
        return CompensatingCleanupOutcome(primary, cleanupFailures)
    }
}
