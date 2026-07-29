package com.togetherly.feature.family.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf

/**
 * The Family tab root's summary — deliberately narrow: only what
 * [com.togetherly.feature.family.presentation.FamilyProfileSummarySection] is allowed to show (see
 * that composable's own KDoc for the explicit "do not show" list from the step spec). [familyName]
 * is `null` when no display name was ever set (it's an optional field on
 * [com.togetherly.domain.family.FamilyProfile]) — the summary falls back to generic copy rather
 * than showing a blank.
 */
@Immutable
data class FamilyUiState(
    val isLoading: Boolean = true,
    val familyName: String? = null,
    val ageBands: PersistentSet<AgeBand> = persistentSetOf(),
    val preferredDurations: PersistentSet<DurationBand> = persistentSetOf(),
    val locationPreference: LocationPreference = LocationPreference.BOTH,
    val message: UiText? = null,
    val error: UiText? = null,
)
