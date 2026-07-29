package com.togetherly.data.local.keys

import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import kotlinx.datetime.DayOfWeek

/**
 * Explicit, case-sensitive lookups — never `enum.name` or `enumValueOf()` — so a database value
 * stays stable even if a Kotlin enum constant is ever renamed, and an unrecognized stored value
 * is always a reportable mapping problem (surfaced by a future domain mapper) rather than a
 * silent crash or a name that quietly drifted with a refactor.
 */
internal fun AgeBand.toStorageKey(): String = when (this) {
    AgeBand.AGE_6_TO_8 -> "age_6_8"
    AgeBand.AGE_9_TO_11 -> "age_9_11"
    AgeBand.AGE_12_TO_13 -> "age_12_13"
}

internal fun ageBandFromStorageKey(key: String): AgeBand? = when (key) {
    "age_6_8" -> AgeBand.AGE_6_TO_8
    "age_9_11" -> AgeBand.AGE_9_TO_11
    "age_12_13" -> AgeBand.AGE_12_TO_13
    else -> null
}

internal fun String.toAgeBand(): DataResult<AgeBand> =
    ageBandFromStorageKey(this)?.let { DataResult.Success(it) } ?: unknownStorageKey()

internal fun DurationBand.toStorageKey(): String = when (this) {
    DurationBand.FIVE_MINUTES -> "five_minutes"
    DurationBand.TEN_MINUTES -> "ten_minutes"
    DurationBand.TWENTY_MINUTES -> "twenty_minutes"
    DurationBand.THIRTY_PLUS_MINUTES -> "thirty_plus_minutes"
}

internal fun durationBandFromStorageKey(key: String): DurationBand? = when (key) {
    "five_minutes" -> DurationBand.FIVE_MINUTES
    "ten_minutes" -> DurationBand.TEN_MINUTES
    "twenty_minutes" -> DurationBand.TWENTY_MINUTES
    "thirty_plus_minutes" -> DurationBand.THIRTY_PLUS_MINUTES
    else -> null
}

internal fun String.toDurationBand(): DataResult<DurationBand> =
    durationBandFromStorageKey(this)?.let { DataResult.Success(it) } ?: unknownStorageKey()

internal fun LocationPreference.toStorageKey(): String = when (this) {
    LocationPreference.INDOOR -> "indoor"
    LocationPreference.OUTDOOR -> "outdoor"
    LocationPreference.BOTH -> "both"
}

internal fun locationPreferenceFromStorageKey(key: String): LocationPreference? = when (key) {
    "indoor" -> LocationPreference.INDOOR
    "outdoor" -> LocationPreference.OUTDOOR
    "both" -> LocationPreference.BOTH
    else -> null
}

internal fun String.toLocationPreference(): DataResult<LocationPreference> =
    locationPreferenceFromStorageKey(this)?.let { DataResult.Success(it) } ?: unknownStorageKey()

internal fun PreparationPreference.toStorageKey(): String = when (this) {
    PreparationPreference.NONE -> "none"
    PreparationPreference.SIMPLE_MATERIALS -> "simple_materials"
    PreparationPreference.ANY -> "any"
}

internal fun preparationPreferenceFromStorageKey(key: String): PreparationPreference? = when (key) {
    "none" -> PreparationPreference.NONE
    "simple_materials" -> PreparationPreference.SIMPLE_MATERIALS
    "any" -> PreparationPreference.ANY
    else -> null
}

internal fun String.toPreparationPreference(): DataResult<PreparationPreference> =
    preparationPreferenceFromStorageKey(this)?.let { DataResult.Success(it) } ?: unknownStorageKey()

// EnergyLevel.toStorageKey()/toEnergyLevel() already exist in QuestContextStorageKeys.kt (same
// package) — reused here rather than duplicated.

internal fun DayOfWeek.toStorageKey(): String = when (this) {
    DayOfWeek.MONDAY -> "monday"
    DayOfWeek.TUESDAY -> "tuesday"
    DayOfWeek.WEDNESDAY -> "wednesday"
    DayOfWeek.THURSDAY -> "thursday"
    DayOfWeek.FRIDAY -> "friday"
    DayOfWeek.SATURDAY -> "saturday"
    DayOfWeek.SUNDAY -> "sunday"
}

internal fun dayOfWeekFromStorageKey(key: String): DayOfWeek? = when (key) {
    "monday" -> DayOfWeek.MONDAY
    "tuesday" -> DayOfWeek.TUESDAY
    "wednesday" -> DayOfWeek.WEDNESDAY
    "thursday" -> DayOfWeek.THURSDAY
    "friday" -> DayOfWeek.FRIDAY
    "saturday" -> DayOfWeek.SATURDAY
    "sunday" -> DayOfWeek.SUNDAY
    else -> null
}

internal fun String.toDayOfWeek(): DataResult<DayOfWeek> =
    dayOfWeekFromStorageKey(this)?.let { DataResult.Success(it) } ?: unknownStorageKey()
