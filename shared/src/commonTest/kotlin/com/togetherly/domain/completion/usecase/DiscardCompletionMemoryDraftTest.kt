package com.togetherly.domain.completion.usecase

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.CompletionMemoryDraft
import com.togetherly.domain.completion.PendingMediaReference
import com.togetherly.domain.completion.PendingVoiceReference
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.repository.FakePrivateMediaCommitter
import com.togetherly.domain.completion.validQuestCompletion
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

private val COMPLETION_ID = CompletionId("completion-1")

class DiscardCompletionMemoryDraftTest {

    @Test
    fun discardingDeletesBothPendingFiles() = runTest {
        val mediaCommitter = FakePrivateMediaCommitter()
        val useCase = DiscardCompletionMemoryDraft(mediaCommitter)
        val pendingPhoto = PendingMediaReference("pending-photo")
        val pendingVoice = PendingVoiceReference(PendingMediaReference("pending-voice"), 5.seconds)
        val draft = CompletionMemoryDraft(
            completionId = COMPLETION_ID,
            note = "draft note",
            reactions = emptySet(),
            pendingPhoto = pendingPhoto,
            pendingVoice = pendingVoice,
        )

        val result = useCase(draft)

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(listOf(pendingPhoto), mediaCommitter.deletePendingCalls.filter { it == pendingPhoto })
        assertTrue(pendingVoice.reference in mediaCommitter.deletePendingCalls)
    }

    @Test
    fun discardingWithNoPendingMediaIsANoOp() = runTest {
        val mediaCommitter = FakePrivateMediaCommitter()
        val useCase = DiscardCompletionMemoryDraft(mediaCommitter)
        val draft = CompletionMemoryDraft(
            completionId = COMPLETION_ID,
            note = "",
            reactions = emptySet(),
            pendingPhoto = null,
            pendingVoice = null,
        )

        val result = useCase(draft)

        assertEquals(DataResult.Success(Unit), result)
        assertTrue(mediaCommitter.deletePendingCalls.isEmpty())
    }

    @Test
    fun discardingDoesNotAffectAnyPersistedCompletion() = runTest {
        val completionRepository = FakeCompletionRepository()
        val original = validQuestCompletion(id = COMPLETION_ID)
        completionRepository.saveCompletion(original)
        val mediaCommitter = FakePrivateMediaCommitter()
        val useCase = DiscardCompletionMemoryDraft(mediaCommitter)
        val draft = CompletionMemoryDraft(
            completionId = COMPLETION_ID,
            note = "",
            reactions = emptySet(),
            pendingPhoto = PendingMediaReference("pending-photo"),
            pendingVoice = null,
        )

        useCase(draft)

        assertEquals(original, (completionRepository.getCompletion(COMPLETION_ID) as DataResult.Success).value)
    }

    @Test
    fun aDeletePendingFailureIsSurfacedAndStopsFurtherDeletion() = runTest {
        val mediaCommitter = FakePrivateMediaCommitter()
        val error = AppError.Storage(StorageError.DELETE_FAILED)
        mediaCommitter.setDeletePendingError(error)
        val useCase = DiscardCompletionMemoryDraft(mediaCommitter)
        val draft = CompletionMemoryDraft(
            completionId = COMPLETION_ID,
            note = "",
            reactions = emptySet(),
            pendingPhoto = PendingMediaReference("pending-photo"),
            pendingVoice = PendingVoiceReference(PendingMediaReference("pending-voice"), 5.seconds),
        )

        val result = useCase(draft)

        assertEquals(DataResult.Error(error), result)
        // The photo delete failed, so the voice delete is never attempted — only one call total.
        assertEquals(1, mediaCommitter.deletePendingCalls.size)
    }
}
