package com.togetherly.feature.onboarding.presentation.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.input.TogetherlyTextField
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.family.FamilyDisplayName
import com.togetherly.feature.onboarding.model.OnboardingField
import com.togetherly.feature.onboarding.model.OnboardingUiState
import com.togetherly.feature.onboarding.presentation.OnboardingAction
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.onboarding_family_name_label
import togetherly.shared.generated.resources.onboarding_family_name_placeholder
import togetherly.shared.generated.resources.onboarding_family_name_supporting
import togetherly.shared.generated.resources.onboarding_family_name_title

/** Shows the remaining-characters counter only once the family name is getting close to the limit — not from the first keystroke. */
private const val SHOW_COUNTER_WITHIN_REMAINING = 20

@Composable
internal fun FamilyNameStepContent(state: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
    ) {
        Text(
            text = stringResource(Res.string.onboarding_family_name_title),
            style = MaterialTheme.togetherlyTypography.headlineM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
        Text(
            text = stringResource(Res.string.onboarding_family_name_supporting),
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
        val remaining = FamilyDisplayName.MAX_LENGTH - state.familyName.length
        TogetherlyTextField(
            value = state.familyName,
            onValueChange = { onAction(OnboardingAction.FamilyNameChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.onboarding_family_name_label),
            placeholder = stringResource(Res.string.onboarding_family_name_placeholder),
            errorText = state.validationErrors[OnboardingField.FAMILY_NAME]?.asString(),
            singleLine = true,
            characterLimit = FamilyDisplayName.MAX_LENGTH.takeIf { remaining <= SHOW_COUNTER_WITHIN_REMAINING },
        )
    }
}
