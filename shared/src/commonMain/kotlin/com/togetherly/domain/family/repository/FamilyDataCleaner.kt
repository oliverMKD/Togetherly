package com.togetherly.domain.family.repository

import com.togetherly.core.result.DataResult

/**
 * The explicit, full-wipe contract [FamilyRepository.deleteProfile]'s own KDoc points to —
 * clearing a family's profile alone is a distinct, narrower operation from clearing every table a
 * family owns, and the two are never conflated: [FamilyRepository.deleteProfile] must keep working
 * exactly as documented (profile + preference rows only) regardless of whether this contract also
 * exists.
 *
 * [deleteAllFamilyData] transactionally deletes every row this app's family owns: the profile and
 * its preference rows, daily selections, dismissal history, saved quests, the active session, and
 * every completion along with its reaction/media metadata rows. It must never delete the bundled
 * quest catalogue (not a database table at all — JSON-backed, see docs/content-system.md) or
 * database/schema metadata needed for migrations. Deleting the actual photo/voice *files* a
 * completion's media metadata referenced is a separate responsibility this contract does not
 * perform — see docs/persistence.md's media-metadata-versus-files section; a caller that needs a
 * true full wipe must coordinate a media file cleaner alongside this call.
 *
 * Idempotent: calling this with nothing left to delete still succeeds.
 */
interface FamilyDataCleaner {

    suspend fun deleteAllFamilyData(): DataResult<Unit>
}
