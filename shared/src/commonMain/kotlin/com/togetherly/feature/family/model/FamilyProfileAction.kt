package com.togetherly.feature.family.model

import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference

/** No `ParticipantCountChanged` action — see [FamilyProfileUiState]'s own KDoc for why. */
sealed interface FamilyProfileAction {
    data class FamilyNameChanged(val value: String) : FamilyProfileAction
    data class AgeBandToggled(val value: AgeBand) : FamilyProfileAction
    data class DurationToggled(val value: DurationBand) : FamilyProfileAction
    data class LocationPreferenceChanged(val value: LocationPreference) : FamilyProfileAction
    data object SaveClicked : FamilyProfileAction
    data object BackClicked : FamilyProfileAction
    data object DiscardConfirmed : FamilyProfileAction
    data object DismissDiscardDialog : FamilyProfileAction
}
