package com.togetherly.feature.onboarding.presentation.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.designsystem.component.card.TogetherlySelectableCard
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.family.AgeBand
import com.togetherly.feature.onboarding.model.OnboardingField
import com.togetherly.feature.onboarding.model.OnboardingUiState
import com.togetherly.feature.onboarding.presentation.OnboardingAction
import com.togetherly.feature.onboarding.presentation.OnboardingFieldError
import com.togetherly.feature.onboarding.presentation.label
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.onboarding_age_bands_privacy_note
import togetherly.shared.generated.resources.onboarding_age_bands_supporting
import togetherly.shared.generated.resources.onboarding_age_bands_title

@Composable
internal fun AgeBandsStepContent(state: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
    ) {
        Text(
            text = stringResource(Res.string.onboarding_age_bands_title),
            style = MaterialTheme.togetherlyTypography.headlineM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
        Text(
            text = stringResource(Res.string.onboarding_age_bands_supporting),
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
            AgeBand.entries.forEach { band ->
                TogetherlySelectableCard(
                    selected = band in state.selectedAgeBands,
                    onClick = { onAction(OnboardingAction.AgeBandToggled(band)) },
                    title = band.label(),
                    modifier = Modifier.fillMaxWidth(),
                    multiSelect = true,
                )
            }
        }
        OnboardingFieldError(state, OnboardingField.AGE_BANDS)
        Text(
            text = stringResource(Res.string.onboarding_age_bands_privacy_note),
            style = MaterialTheme.togetherlyTypography.bodyS,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
    }
}
