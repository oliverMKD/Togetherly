package com.togetherly.domain.family.repository

import com.togetherly.core.result.DataResult

/**
 * The "Reset quest history" contract (Step 13.7) — a narrower sibling of [FamilyDataCleaner]:
 * deletes daily selections, dismissal history, the active session, and every completion (with its
 * reaction/media metadata rows, via the same cascade [FamilyDataCleaner] relies on), but — unlike
 * [FamilyDataCleaner] — never touches the family profile, its preference rows, or saved quests.
 * Saved quests are deliberately preserved here: a family's curated "quests we want to come back
 * to" list is not itself a history record the way a completion or a dismissal is (see
 * `docs/local-data-deletion.md`).
 *
 * Never deletes the actual photo/voice *files* a cleared completion's media metadata referenced —
 * a caller that also needs those gone must pair this with [com.togetherly.domain.completion.repository.PrivateMediaCommitter.deleteCommitted]
 * for each [com.togetherly.domain.completion.MemoryMedia] read *before* calling this (see
 * [com.togetherly.domain.localdata.usecase.ResetQuestHistory]).
 *
 * Idempotent: calling this with nothing left to reset still succeeds.
 */
interface QuestHistoryCleaner {

    suspend fun resetQuestHistory(): DataResult<Unit>
}
