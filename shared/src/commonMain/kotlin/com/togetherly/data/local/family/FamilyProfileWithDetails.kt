package com.togetherly.data.local.family

import androidx.room.Embedded
import androidx.room.Relation

internal data class FamilyProfileWithDetails(
    @Embedded
    val profile: FamilyProfileEntity,

    @Relation(parentColumn = "id", entityColumn = "familyId")
    val ageBands: List<FamilyAgeBandEntity>,

    @Relation(parentColumn = "id", entityColumn = "familyId")
    val interests: List<FamilyInterestEntity>,

    @Relation(parentColumn = "id", entityColumn = "familyId")
    val durationPreferences: List<FamilyDurationPreferenceEntity>,

    @Relation(parentColumn = "id", entityColumn = "familyId")
    val energyPreferences: List<FamilyEnergyPreferenceEntity>,

    @Relation(parentColumn = "id", entityColumn = "familyId")
    val reminderDays: List<FamilyReminderDayEntity>,
)
