package com.togetherly.feature.onboarding.presentation.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.designsystem.component.selection.TogetherlyChipFlowRow
import com.togetherly.designsystem.component.selection.TogetherlyChoiceChip
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.feature.onboarding.model.OnboardingField
import com.togetherly.feature.onboarding.model.OnboardingUiState
import com.togetherly.feature.onboarding.presentation.OnboardingAction
import com.togetherly.feature.onboarding.presentation.OnboardingFieldError
import com.togetherly.feature.onboarding.presentation.label
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.onboarding_preferences_location_label
import togetherly.shared.generated.resources.onboarding_preferences_preparation_label
import togetherly.shared.generated.resources.onboarding_preferences_time_label
import togetherly.shared.generated.resources.onboarding_preferences_time_supporting
import togetherly.shared.generated.resources.onboarding_preferences_title

/**
 * All three preferences (time, location, preparation) stay on one step: [TogetherlyScreen][com.togetherly.designsystem.component.layout.TogetherlyScreen]'s
 * content already scrolls, so a large font scale makes this page taller rather than clipping or
 * overlapping — there is no density problem to split across two presentation steps for. If a
 * future redesign adds substantially more content here, split there, not now.
 *
 * Location/preparation use chips rather than [TogetherlySelectableCard][com.togetherly.designsystem.component.card.TogetherlySelectableCard]'s
 * single-select `Role.RadioButton` treatment — a deliberate, common trade-off (plain "choice
 * chips" for a small single-select set) favoring a compact, readable screen over role-perfect
 * semantics for a 3-option group; both directions are documented here rather than silently chosen.
 */
@Composable
internal fun PreferencesStepContent(state: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
    ) {
        Text(
            text = stringResource(Res.string.onboarding_preferences_title),
            style = MaterialTheme.togetherlyTypography.headlineM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
            Text(
                text = stringResource(Res.string.onboarding_preferences_time_label),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            Text(
                text = stringResource(Res.string.onboarding_preferences_time_supporting),
                style = MaterialTheme.togetherlyTypography.bodyS,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )
            TogetherlyChipFlowRow(modifier = Modifier.fillMaxWidth()) {
                DurationBand.entries.forEach { duration ->
                    TogetherlyChoiceChip(
                        label = duration.label(),
                        selected = duration in state.selectedDurations,
                        onClick = { onAction(OnboardingAction.DurationToggled(duration)) },
                    )
                }
            }
            OnboardingFieldError(state, OnboardingField.DURATIONS)
        }

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
            Text(
                text = stringResource(Res.string.onboarding_preferences_location_label),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            TogetherlyChipFlowRow(modifier = Modifier.fillMaxWidth()) {
                LocationPreference.entries.forEach { location ->
                    TogetherlyChoiceChip(
                        label = location.label(),
                        selected = location == state.locationPreference,
                        onClick = { onAction(OnboardingAction.LocationSelected(location)) },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
            Text(
                text = stringResource(Res.string.onboarding_preferences_preparation_label),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            TogetherlyChipFlowRow(modifier = Modifier.fillMaxWidth()) {
                PreparationPreference.entries.forEach { preparation ->
                    TogetherlyChoiceChip(
                        label = preparation.label(),
                        selected = preparation == state.preparationPreference,
                        onClick = { onAction(OnboardingAction.PreparationSelected(preparation)) },
                    )
                }
            }
        }
    }
}
