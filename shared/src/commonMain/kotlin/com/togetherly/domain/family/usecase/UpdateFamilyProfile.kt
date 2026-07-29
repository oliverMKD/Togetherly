package com.togetherly.domain.family.usecase

import com.togetherly.core.datetime.AppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.map
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyDisplayName
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.family.ReminderPreference
import com.togetherly.domain.family.repository.FamilyRepository
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.validation.DomainValidationException

data class UpdateFamilyProfileCommand(
    val displayName: FamilyDisplayName?,
    val childAgeBands: Set<AgeBand>,
    val interests: Set<QuestCategory>,
    val preferredDurations: Set<DurationBand>,
    val locationPreference: LocationPreference,
    val preparationPreference: PreparationPreference,
    val preferredEnergyLevels: Set<EnergyLevel>,
    val reminderPreference: ReminderPreference?,
)

/**
 * Never generates a new [com.togetherly.domain.family.FamilyId] — the existing profile's
 * identity and [FamilyProfile.createdAt] are always preserved; only [FamilyProfile.updatedAt]
 * moves forward.
 */
class UpdateFamilyProfile(
    private val familyRepository: FamilyRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(command: UpdateFamilyProfileCommand): DataResult<FamilyProfile> {
        val existingResult = familyRepository.getProfile()
        if (existingResult is DataResult.Error) return existingResult
        val existing = (existingResult as DataResult.Success).value
            ?: return DataResult.Error(AppError.Validation(ValidationError.MISSING_FAMILY_PROFILE))

        val updated = try {
            existing.copy(
                displayName = command.displayName,
                childAgeBands = command.childAgeBands,
                interests = command.interests,
                preferredDurations = command.preferredDurations,
                locationPreference = command.locationPreference,
                preparationPreference = command.preparationPreference,
                preferredEnergyLevels = command.preferredEnergyLevels,
                reminderPreference = command.reminderPreference,
                updatedAt = clock.now(),
            )
        } catch (e: DomainValidationException) {
            return DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
        }

        return familyRepository.saveProfile(updated).map { updated }
    }
}
