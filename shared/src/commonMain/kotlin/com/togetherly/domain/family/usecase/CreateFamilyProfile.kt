package com.togetherly.domain.family.usecase

import com.togetherly.core.datetime.AppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.id.IdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.map
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyDisplayName
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.family.ReminderPreference
import com.togetherly.domain.family.repository.FamilyRepository
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.validation.DomainValidationException

/** [preferredEnergyLevels] defaults to empty — onboarding doesn't collect it yet (Step 13.3's own quest preferences screen is where a family first sets it). */
data class CreateFamilyProfileCommand(
    val displayName: FamilyDisplayName?,
    val childAgeBands: Set<AgeBand>,
    val interests: Set<QuestCategory>,
    val preferredDurations: Set<DurationBand>,
    val locationPreference: LocationPreference,
    val preparationPreference: PreparationPreference,
    val preferredEnergyLevels: Set<EnergyLevel> = emptySet(),
    val reminderPreference: ReminderPreference?,
)

class CreateFamilyProfile(
    private val familyRepository: FamilyRepository,
    private val clock: AppClock,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(command: CreateFamilyProfileCommand): DataResult<FamilyProfile> {
        val now = clock.now()

        val profile = try {
            FamilyProfile(
                id = FamilyId(idGenerator.generate()),
                displayName = command.displayName,
                childAgeBands = command.childAgeBands,
                interests = command.interests,
                preferredDurations = command.preferredDurations,
                locationPreference = command.locationPreference,
                preparationPreference = command.preparationPreference,
                preferredEnergyLevels = command.preferredEnergyLevels,
                reminderPreference = command.reminderPreference,
                createdAt = now,
                updatedAt = now,
            )
        } catch (e: DomainValidationException) {
            return DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
        }

        return familyRepository.saveProfile(profile).map { profile }
    }
}
