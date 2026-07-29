package com.togetherly.domain.completion.usecase

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.domain.completion.MediaEdit
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.MemoryNote
import com.togetherly.domain.completion.PendingMediaReference
import com.togetherly.domain.completion.PendingVoiceReference
import com.togetherly.domain.completion.SaveCompletionMemoryCommand
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.repository.FakePrivateMediaCommitter
import com.togetherly.domain.completion.validQuestCompletion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

private val COMPLETION_ID = CompletionId("completion-1")

class SaveCompletionMemoryTest {

    private fun buildUseCase(
        completionRepository: FakeCompletionRepository = FakeCompletionRepository(),
        mediaCommitter: FakePrivateMediaCommitter = FakePrivateMediaCommitter(),
    ) = Triple(
        SaveCompletionMemory(completionRepository, mediaCommitter, SequentialIdGenerator("media")),
        completionRepository,
        mediaCommitter,
    )

    @Test
    fun savingNoteOnlyUpdatesTheCompletion() = runTest {
        val (useCase, repository, _) = buildUseCase()
        repository.saveCompletion(validQuestCompletion(id = COMPLETION_ID))
        val command = SaveCompletionMemoryCommand(completionId = COMPLETION_ID, note = "What a day")

        val result = useCase(command)

        val completion = (result as DataResult.Success).value
        assertEquals(MemoryNote("What a day"), completion.note)
        assertEquals(emptySet(), completion.reactions)
        assertTrue(completion.media.isEmpty())
    }

    @Test
    fun blankNoteBecomesNull() = runTest {
        val (useCase, repository, _) = buildUseCase()
        repository.saveCompletion(validQuestCompletion(id = COMPLETION_ID, note = MemoryNote("existing")))
        val command = SaveCompletionMemoryCommand(completionId = COMPLETION_ID, note = "   ")

        val result = useCase(command)

        val completion = (result as DataResult.Success).value
        assertEquals(null, completion.note)
    }

    @Test
    fun savingReactionsOnlyUpdatesTheCompletion() = runTest {
        val (useCase, repository, _) = buildUseCase()
        repository.saveCompletion(validQuestCompletion(id = COMPLETION_ID))
        val command = SaveCompletionMemoryCommand(
            completionId = COMPLETION_ID,
            note = null,
            reactions = setOf(FamilyReaction.HAPPY, FamilyReaction.SILLY),
        )

        val result = useCase(command)

        val completion = (result as DataResult.Success).value
        assertEquals(setOf(FamilyReaction.HAPPY, FamilyReaction.SILLY), completion.reactions)
    }

    @Test
    fun savingWithoutOptionalContentLeavesAValidCompletion() = runTest {
        val (useCase, repository, _) = buildUseCase()
        repository.saveCompletion(validQuestCompletion(id = COMPLETION_ID))
        val command = SaveCompletionMemoryCommand(completionId = COMPLETION_ID, note = null)

        val result = useCase(command)

        val completion = (result as DataResult.Success).value
        assertEquals(null, completion.note)
        assertEquals(emptySet(), completion.reactions)
        assertTrue(completion.media.isEmpty())
    }

    @Test
    fun missingCompletionReturnsTypedError() = runTest {
        val (useCase, _, _) = buildUseCase()
        val command = SaveCompletionMemoryCommand(completionId = CompletionId("unknown"), note = null)

        val result = useCase(command)

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.COMPLETION_NOT_FOUND)), result)
    }

    @Test
    fun invalidNoteReturnsTypedErrorWithoutTouchingTheCompletion() = runTest {
        val (useCase, repository, _) = buildUseCase()
        val original = validQuestCompletion(id = COMPLETION_ID)
        repository.saveCompletion(original)
        val command = SaveCompletionMemoryCommand(completionId = COMPLETION_ID, note = "  untrimmed  ")

        val result = useCase(command)

        assertEquals(DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT)), result)
        assertEquals(original, (repository.getCompletion(COMPLETION_ID) as DataResult.Success).value)
    }

    @Test
    fun commitsAPendingPhoto() = runTest {
        val (useCase, repository, mediaCommitter) = buildUseCase()
        repository.saveCompletion(validQuestCompletion(id = COMPLETION_ID))
        val command = SaveCompletionMemoryCommand(
            completionId = COMPLETION_ID,
            note = null,
            photo = MediaEdit.Replace(PendingMediaReference("pending-photo")),
        )

        val result = useCase(command)

        val completion = (result as DataResult.Success).value
        val photo = completion.media.filterIsInstance<MemoryMedia.Photo>().single()
        assertEquals(listOf(photo), mediaCommitter.committedPhotoCalls)
    }

    @Test
    fun commitsAPendingVoice() = runTest {
        val (useCase, repository, mediaCommitter) = buildUseCase()
        repository.saveCompletion(validQuestCompletion(id = COMPLETION_ID))
        val pendingVoice = PendingVoiceReference(PendingMediaReference("pending-voice"), 10.seconds)
        val command = SaveCompletionMemoryCommand(
            completionId = COMPLETION_ID,
            note = null,
            voice = MediaEdit.Replace(pendingVoice),
        )

        val result = useCase(command)

        val completion = (result as DataResult.Success).value
        val voice = completion.media.filterIsInstance<MemoryMedia.Voice>().single()
        assertEquals(10.seconds, voice.duration)
        assertEquals(listOf(voice), mediaCommitter.committedVoiceCalls)
    }

    @Test
    fun commitsBothPhotoAndVoice() = runTest {
        val (useCase, repository, mediaCommitter) = buildUseCase()
        repository.saveCompletion(validQuestCompletion(id = COMPLETION_ID))
        val command = SaveCompletionMemoryCommand(
            completionId = COMPLETION_ID,
            note = null,
            photo = MediaEdit.Replace(PendingMediaReference("pending-photo")),
            voice = MediaEdit.Replace(PendingVoiceReference(PendingMediaReference("pending-voice"), 5.seconds)),
        )

        val result = useCase(command)

        val completion = (result as DataResult.Success).value
        assertEquals(2, completion.media.size)
        assertEquals(1, mediaCommitter.committedPhotoCalls.size)
        assertEquals(1, mediaCommitter.committedVoiceCalls.size)
    }

    @Test
    fun photoCommitFailureLeavesTheCompletionUntouched() = runTest {
        val (useCase, repository, mediaCommitter) = buildUseCase()
        val original = validQuestCompletion(id = COMPLETION_ID)
        repository.saveCompletion(original)
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        mediaCommitter.setCommitPhotoError(error)
        val command = SaveCompletionMemoryCommand(
            completionId = COMPLETION_ID,
            note = null,
            photo = MediaEdit.Replace(PendingMediaReference("pending-photo")),
        )

        val result = useCase(command)

        assertEquals(DataResult.Error(error), result)
        assertEquals(original, (repository.getCompletion(COMPLETION_ID) as DataResult.Success).value)
        assertTrue(mediaCommitter.deleteCommittedCalls.isEmpty())
    }

    @Test
    fun voiceFailureCleansUpTheNewlyCommittedPhoto() = runTest {
        val (useCase, repository, mediaCommitter) = buildUseCase()
        val original = validQuestCompletion(id = COMPLETION_ID)
        repository.saveCompletion(original)
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        mediaCommitter.setCommitVoiceError(error)
        val command = SaveCompletionMemoryCommand(
            completionId = COMPLETION_ID,
            note = null,
            photo = MediaEdit.Replace(PendingMediaReference("pending-photo")),
            voice = MediaEdit.Replace(PendingVoiceReference(PendingMediaReference("pending-voice"), 5.seconds)),
        )

        val result = useCase(command)

        assertEquals(DataResult.Error(error), result)
        val committedPhoto = mediaCommitter.committedPhotoCalls.single()
        assertEquals(listOf<MemoryMedia>(committedPhoto), mediaCommitter.deleteCommittedCalls)
        assertEquals(original, (repository.getCompletion(COMPLETION_ID) as DataResult.Success).value)
    }

    @Test
    fun databaseFailureCleansUpAllNewlyCommittedMedia() = runTest {
        val (useCase, repository, mediaCommitter) = buildUseCase()
        val original = validQuestCompletion(id = COMPLETION_ID)
        repository.saveCompletion(original)
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        repository.setSaveCompletionError(error)
        val command = SaveCompletionMemoryCommand(
            completionId = COMPLETION_ID,
            note = null,
            photo = MediaEdit.Replace(PendingMediaReference("pending-photo")),
            voice = MediaEdit.Replace(PendingVoiceReference(PendingMediaReference("pending-voice"), 5.seconds)),
        )

        val result = useCase(command)

        assertEquals(DataResult.Error(error), result)
        assertEquals(2, mediaCommitter.deleteCommittedCalls.size)
        assertEquals(original, (repository.getCompletion(COMPLETION_ID) as DataResult.Success).value)
    }

    @Test
    fun existingMediaSurvivesAFailedReplacement() = runTest {
        val (useCase, repository, mediaCommitter) = buildUseCase()
        val existingPhoto = MemoryMedia.Photo(MemoryMediaId("existing-photo"), MediaReference("ref-existing"))
        val original = validQuestCompletion(id = COMPLETION_ID, media = listOf(existingPhoto))
        repository.saveCompletion(original)
        val error = AppError.Storage(StorageError.WRITE_FAILED)
        repository.setSaveCompletionError(error)
        val command = SaveCompletionMemoryCommand(
            completionId = COMPLETION_ID,
            note = null,
            photo = MediaEdit.Replace(PendingMediaReference("pending-photo")),
        )

        val result = useCase(command)

        assertEquals(DataResult.Error(error), result)
        // The newly committed replacement photo is cleaned up, but the pre-existing one is never
        // deleted since the database write that would have stopped referencing it never succeeded.
        assertTrue(existingPhoto !in mediaCommitter.deleteCommittedCalls)
        assertEquals(original, (repository.getCompletion(COMPLETION_ID) as DataResult.Success).value)
    }

    @Test
    fun explicitPhotoRemovalDeletesTheExistingFileAfterSaving() = runTest {
        val (useCase, repository, mediaCommitter) = buildUseCase()
        val existingPhoto = MemoryMedia.Photo(MemoryMediaId("existing-photo"), MediaReference("ref-existing"))
        val original = validQuestCompletion(id = COMPLETION_ID, media = listOf(existingPhoto))
        repository.saveCompletion(original)
        val command = SaveCompletionMemoryCommand(completionId = COMPLETION_ID, note = null, photo = MediaEdit.Remove)

        val result = useCase(command)

        val completion = (result as DataResult.Success).value
        assertTrue(completion.media.filterIsInstance<MemoryMedia.Photo>().isEmpty())
        assertEquals(listOf<MemoryMedia>(existingPhoto), mediaCommitter.deleteCommittedCalls)
    }

    @Test
    fun unchangedPhotoRemainsAttachedAndUntouched() = runTest {
        val (useCase, repository, mediaCommitter) = buildUseCase()
        val existingPhoto = MemoryMedia.Photo(MemoryMediaId("existing-photo"), MediaReference("ref-existing"))
        val original = validQuestCompletion(id = COMPLETION_ID, media = listOf(existingPhoto))
        repository.saveCompletion(original)
        val command = SaveCompletionMemoryCommand(completionId = COMPLETION_ID, note = null, photo = MediaEdit.Unchanged)

        val result = useCase(command)

        val completion = (result as DataResult.Success).value
        assertEquals(listOf(existingPhoto), completion.media)
        assertTrue(mediaCommitter.deleteCommittedCalls.isEmpty())
        assertTrue(mediaCommitter.committedPhotoCalls.isEmpty())
    }

    @Test
    fun cancellationDuringVoiceCommitCleansUpTheNewlyCommittedPhotoThenRethrows() = runTest {
        val (useCase, repository, mediaCommitter) = buildUseCase()
        repository.saveCompletion(validQuestCompletion(id = COMPLETION_ID))
        mediaCommitter.throwOnNextCommitVoice(CancellationException("cancelled"))
        val command = SaveCompletionMemoryCommand(
            completionId = COMPLETION_ID,
            note = null,
            photo = MediaEdit.Replace(PendingMediaReference("pending-photo")),
            voice = MediaEdit.Replace(PendingVoiceReference(PendingMediaReference("pending-voice"), 5.seconds)),
        )

        assertFailsWith<CancellationException> { useCase(command) }

        val committedPhoto = mediaCommitter.committedPhotoCalls.single()
        assertEquals(listOf<MemoryMedia>(committedPhoto), mediaCommitter.deleteCommittedCalls)
    }

    @Test
    fun duplicateSaveDoesNotCreateDuplicateMediaIds() = runTest {
        val (useCase, repository, mediaCommitter) = buildUseCase()
        repository.saveCompletion(validQuestCompletion(id = COMPLETION_ID))
        val command = SaveCompletionMemoryCommand(
            completionId = COMPLETION_ID,
            note = null,
            photo = MediaEdit.Replace(PendingMediaReference("pending-photo")),
        )

        val first = (useCase(command) as DataResult.Success).value
        val second = (useCase(command) as DataResult.Success).value

        val firstPhoto = first.media.filterIsInstance<MemoryMedia.Photo>().single()
        val secondPhoto = second.media.filterIsInstance<MemoryMedia.Photo>().single()
        assertEquals(1, second.media.size)
        assertTrue(firstPhoto.id != secondPhoto.id)
        assertEquals(listOf<MemoryMedia>(firstPhoto), mediaCommitter.deleteCommittedCalls)
        val finalCompletion = (repository.getCompletion(COMPLETION_ID) as DataResult.Success).value
        assertEquals(listOf(secondPhoto), finalCompletion?.media)
    }
}
