package com.togetherly.feature.onboarding.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.component.progress.TogetherlyStepProgress
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.onboarding.model.OnboardingField
import com.togetherly.feature.onboarding.model.OnboardingStep
import com.togetherly.feature.onboarding.model.OnboardingUiState
import com.togetherly.feature.onboarding.presentation.steps.AgeBandsStepContent
import com.togetherly.feature.onboarding.presentation.steps.FamilyNameStepContent
import com.togetherly.feature.onboarding.presentation.steps.InterestsStepContent
import com.togetherly.feature.onboarding.presentation.steps.PreferencesStepContent
import com.togetherly.feature.onboarding.presentation.steps.ReminderStepContent
import com.togetherly.feature.onboarding.presentation.steps.ReviewStepContent
import com.togetherly.feature.onboarding.presentation.steps.WelcomeStepContent
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.onboarding_back_content_description
import togetherly.shared.generated.resources.onboarding_continue
import togetherly.shared.generated.resources.onboarding_review_back_and_edit
import togetherly.shared.generated.resources.onboarding_review_primary
import togetherly.shared.generated.resources.onboarding_skip_name
import togetherly.shared.generated.resources.onboarding_welcome_primary

/**
 * Onboarding's whole flow renders through this one stateless screen — [state] in, [onAction] out,
 * nothing else. [com.togetherly.feature.onboarding.presentation.OnboardingRoute] is the only
 * stateful wrapper around it; every step content composable (`com.togetherly.feature.onboarding.presentation.steps`)
 * is itself state-in/action-out too, so nothing below this function ever resolves a ViewModel.
 *
 * The shared scaffold ([TogetherlyScreen] + [TogetherlyTopBar] + a step-progress row + a stable
 * bottom action area) is identical across all seven steps — only the scrollable middle content and
 * which bottom actions appear ever change per [OnboardingUiState.step]. [TogetherlyScreen]'s own
 * scrolling + max-content-width + window-inset handling (see its KDoc from the design-system step)
 * is what keeps every step keyboard-safe and readable on larger displays without this screen
 * reimplementing any of that.
 */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
) {
    TogetherlyScreen(
        topBar = {
            TogetherlyTopBar(
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = stringResource(Res.string.onboarding_back_content_description),
                        onClick = { onAction(OnboardingAction.BackClicked) },
                        enabled = !state.isSaving,
                    )
                },
            )
        },
        bottomBar = { OnboardingBottomActions(state, onAction) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
        ) {
            if (state.step != OnboardingStep.WELCOME) {
                TogetherlyStepProgress(
                    currentStep = state.step.ordinal + 1,
                    totalSteps = OnboardingStep.entries.size,
                )
            }
            when (state.step) {
                OnboardingStep.WELCOME -> WelcomeStepContent()
                OnboardingStep.FAMILY_NAME -> FamilyNameStepContent(state, onAction)
                OnboardingStep.AGE_BANDS -> AgeBandsStepContent(state, onAction)
                OnboardingStep.INTERESTS -> InterestsStepContent(state, onAction)
                OnboardingStep.PREFERENCES -> PreferencesStepContent(state, onAction)
                OnboardingStep.REMINDER -> ReminderStepContent(state, onAction)
                OnboardingStep.REVIEW -> ReviewStepContent(state, onAction)
            }
        }
    }
}

@Composable
private fun OnboardingBottomActions(state: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.togetherlySpacing.m),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
    ) {
        when (state.step) {
            OnboardingStep.WELCOME -> TogetherlyPrimaryButton(
                label = stringResource(Res.string.onboarding_welcome_primary),
                onClick = { onAction(OnboardingAction.ContinueClicked) },
                modifier = Modifier.fillMaxWidth(),
            )
            OnboardingStep.FAMILY_NAME -> {
                TogetherlyPrimaryButton(
                    label = stringResource(Res.string.onboarding_continue),
                    onClick = { onAction(OnboardingAction.ContinueClicked) },
                    modifier = Modifier.fillMaxWidth(),
                )
                TogetherlyTextButton(
                    label = stringResource(Res.string.onboarding_skip_name),
                    onClick = { onAction(OnboardingAction.SkipNameClicked) },
                )
            }
            OnboardingStep.AGE_BANDS, OnboardingStep.INTERESTS, OnboardingStep.PREFERENCES, OnboardingStep.REMINDER ->
                TogetherlyPrimaryButton(
                    label = stringResource(Res.string.onboarding_continue),
                    onClick = { onAction(OnboardingAction.ContinueClicked) },
                    modifier = Modifier.fillMaxWidth(),
                )
            OnboardingStep.REVIEW -> {
                TogetherlyPrimaryButton(
                    label = stringResource(Res.string.onboarding_review_primary),
                    onClick = { onAction(OnboardingAction.CreateFamilyClicked) },
                    loading = state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                TogetherlyTextButton(
                    label = stringResource(Res.string.onboarding_review_back_and_edit),
                    onClick = { onAction(OnboardingAction.BackClicked) },
                    enabled = !state.isSaving,
                )
            }
        }
    }
}

/** A small, reusable field-level error line — every selection step uses this the same way. */
@Composable
internal fun OnboardingFieldError(state: OnboardingUiState, field: OnboardingField) {
    val error = state.validationErrors[field] ?: return
    Text(
        text = error.asString(),
        style = MaterialTheme.togetherlyTypography.bodyS,
        color = MaterialTheme.togetherlyColors.error,
    )
}
