package com.togetherly.domain.completion.repository

import com.togetherly.core.result.DataResult

/**
 * The "Delete memories" contract (Step 13.7) — deliberately narrower than deleting a completion.
 * A memory (note + photo/voice media) is not a separate table from [com.togetherly.domain.completion.QuestCompletion]
 * in this app's schema; [clearAllMemoryContent] clears every completion's
 * [com.togetherly.domain.completion.QuestCompletion.note] and deletes every
 * [com.togetherly.domain.completion.MemoryMedia] metadata row, while leaving the completion row
 * itself (id, quest, timestamps) and its [com.togetherly.domain.completion.FamilyReaction]s
 * completely untouched — a completion (a fact: "this quest happened on this day") survives even
 * when its memory content (what a family chose to attach afterward) is cleared. See
 * `docs/local-data-deletion.md` for the full rationale.
 *
 * Never deletes the actual photo/voice *files* — a caller that also needs those gone must pair
 * this with [PrivateMediaCommitter.deleteCommitted] for each [com.togetherly.domain.completion.MemoryMedia]
 * read *before* calling this (see [com.togetherly.domain.localdata.usecase.DeleteMemories]), the
 * same "database row first, file best-effort after" ordering [com.togetherly.domain.completion.usecase.DeleteCompletion]
 * already establishes.
 *
 * Idempotent: calling this with nothing left to clear still succeeds.
 */
interface MemoryCleaner {

    suspend fun clearAllMemoryContent(): DataResult<Unit>
}
