package com.togetherly.domain.family.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.repository.FamilyDataCleaner

/**
 * The one stable call site presentation code depends on for a full family-data wipe — never a
 * direct [FamilyDataCleaner] call from presentation code, and never
 * [com.togetherly.domain.family.repository.FamilyRepository.deleteProfile] used as a stand-in for
 * this (see that method's own KDoc). Media file cleanup is a separate, not-yet-implemented
 * responsibility this use case does not perform — see [FamilyDataCleaner]'s own KDoc.
 */
class DeleteAllFamilyData(
    private val familyDataCleaner: FamilyDataCleaner,
) {
    suspend operator fun invoke(): DataResult<Unit> = familyDataCleaner.deleteAllFamilyData()
}
