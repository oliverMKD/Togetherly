package com.togetherly.domain.completion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

private val COMPLETION_ID = CompletionId("completion-1")

class CompletionMemoryDraftTest {

    @Test
    fun draftWithNoPendingMediaMapsToUnchangedEdits() {
        val draft = CompletionMemoryDraft(
            completionId = COMPLETION_ID,
            note = "note",
            reactions = setOf(FamilyReaction.HAPPY),
            pendingPhoto = null,
            pendingVoice = null,
        )

        val command = draft.toSaveCommand()

        assertEquals(MediaEdit.Unchanged, command.photo)
        assertEquals(MediaEdit.Unchanged, command.voice)
        assertEquals("note", command.note)
        assertEquals(setOf(FamilyReaction.HAPPY), command.reactions)
    }

    @Test
    fun draftWithPendingMediaMapsToReplaceEdits() {
        val pendingPhoto = PendingMediaReference("pending-photo")
        val pendingVoice = PendingVoiceReference(PendingMediaReference("pending-voice"), 5.seconds)
        val draft = CompletionMemoryDraft(
            completionId = COMPLETION_ID,
            note = "",
            reactions = emptySet(),
            pendingPhoto = pendingPhoto,
            pendingVoice = pendingVoice,
        )

        val command = draft.toSaveCommand()

        assertEquals(MediaEdit.Replace(pendingPhoto), command.photo)
        assertEquals(MediaEdit.Replace(pendingVoice), command.voice)
    }
}
