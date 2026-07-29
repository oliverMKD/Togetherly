package com.togetherly.domain.family

import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.validation.DomainValidationException
import com.togetherly.domain.validation.DomainValidationReason
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

private val CREATED_AT = Instant.parse("2026-01-01T00:00:00Z")
private val UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z")

private fun validFamilyProfile(
    id: FamilyId = FamilyId("family-1"),
    displayName: FamilyDisplayName? = FamilyDisplayName("The Smiths"),
    childAgeBands: Set<AgeBand> = setOf(AgeBand.AGE_6_TO_8),
    interests: Set<QuestCategory> = setOf(QuestCategory.CREATE),
    preferredDurations: Set<DurationBand> = setOf(DurationBand.TEN_MINUTES),
    locationPreference: LocationPreference = LocationPreference.BOTH,
    preparationPreference: PreparationPreference = PreparationPreference.SIMPLE_MATERIALS,
    reminderPreference: ReminderPreference? = null,
    createdAt: Instant = CREATED_AT,
    updatedAt: Instant = CREATED_AT,
) = FamilyProfile(
    id = id,
    displayName = displayName,
    childAgeBands = childAgeBands,
    interests = interests,
    preferredDurations = preferredDurations,
    locationPreference = locationPreference,
    preparationPreference = preparationPreference,
    reminderPreference = reminderPreference,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

class FamilyProfileTest {

    @Test
    fun validFamilyProfileIsAccepted() {
        val profile = validFamilyProfile()

        assertEquals(setOf(AgeBand.AGE_6_TO_8), profile.childAgeBands)
    }

    @Test
    fun missingAgeBandsIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validFamilyProfile(childAgeBands = emptySet())
        }
        assertEquals(DomainValidationReason.EMPTY_COLLECTION, exception.reason)
    }

    @Test
    fun missingInterestsIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validFamilyProfile(interests = emptySet())
        }
        assertEquals(DomainValidationReason.EMPTY_COLLECTION, exception.reason)
    }

    @Test
    fun missingPreferredDurationsIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validFamilyProfile(preferredDurations = emptySet())
        }
        assertEquals(DomainValidationReason.EMPTY_COLLECTION, exception.reason)
    }

    @Test
    fun updatedAtBeforeCreatedAtIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            validFamilyProfile(createdAt = UPDATED_AT, updatedAt = CREATED_AT)
        }
        assertEquals(DomainValidationReason.INVALID_ORDER, exception.reason)
    }

    @Test
    fun displayNameIsOptional() {
        val withName = validFamilyProfile(displayName = FamilyDisplayName("The Smiths"))
        val withoutName = validFamilyProfile(displayName = null)

        assertEquals(FamilyDisplayName("The Smiths"), withName.displayName)
        assertEquals(null, withoutName.displayName)
    }

    @Test
    fun reminderPreferenceIsOptional() {
        val profile = validFamilyProfile(reminderPreference = null)

        assertEquals(null, profile.reminderPreference)
    }

    @Test
    fun reminderPreferenceWithNoEnabledDaysIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            ReminderPreference(
                enabledDays = emptySet(),
                localTime = LocalTime(18, 0),
            )
        }
        assertEquals(DomainValidationReason.EMPTY_COLLECTION, exception.reason)
    }

    @Test
    fun validReminderScheduleIsAccepted() {
        val reminder = ReminderPreference(
            enabledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            localTime = LocalTime(18, 0),
        )
        val profile = validFamilyProfile(reminderPreference = reminder)

        assertEquals(reminder, profile.reminderPreference)
    }

    @Test
    fun preferenceUpdatePreservesIdentityAndCreationTime() {
        val original = validFamilyProfile(createdAt = CREATED_AT, updatedAt = CREATED_AT)

        val updated = original.updatePreferences(
            interests = setOf(QuestCategory.MOVE),
            preferredDurations = setOf(DurationBand.TWENTY_MINUTES),
            locationPreference = LocationPreference.OUTDOOR,
            preparationPreference = PreparationPreference.NONE,
            updatedAt = UPDATED_AT,
        )

        assertEquals(original.id, updated.id)
        assertEquals(original.createdAt, updated.createdAt)
        assertEquals(setOf(QuestCategory.MOVE), updated.interests)
        assertEquals(UPDATED_AT, updated.updatedAt)
    }

    @Test
    fun invalidPreferenceUpdateIsRejected() {
        val original = validFamilyProfile()

        val exception = assertFailsWith<DomainValidationException> {
            original.updatePreferences(
                interests = emptySet(),
                preferredDurations = setOf(DurationBand.TWENTY_MINUTES),
                locationPreference = LocationPreference.OUTDOOR,
                preparationPreference = PreparationPreference.NONE,
                updatedAt = UPDATED_AT,
            )
        }
        assertEquals(DomainValidationReason.EMPTY_COLLECTION, exception.reason)
    }
}
