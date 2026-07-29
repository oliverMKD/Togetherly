package com.togetherly.feature.onboarding.presentation.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.togetherly.designsystem.component.card.TogetherlySelectableCard
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySize
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.feature.onboarding.model.OnboardingField
import com.togetherly.feature.onboarding.model.OnboardingUiState
import com.togetherly.feature.onboarding.presentation.OnboardingAction
import com.togetherly.feature.onboarding.presentation.OnboardingFieldError
import com.togetherly.feature.onboarding.presentation.color
import com.togetherly.feature.onboarding.presentation.description
import com.togetherly.feature.onboarding.presentation.label
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.onboarding_interests_supporting
import togetherly.shared.generated.resources.onboarding_interests_title

/**
 * No dedicated icon per category — the design system has no icon library (a deliberate Step 7.2
 * scope boundary), so "an accessible icon if available" resolves to "not available yet": the
 * colored dot below is decorative only (cleared from semantics), and the real accessible identity
 * is [QuestCategory.label] plus [QuestCategory.description] as ordinary text.
 */
@Composable
internal fun InterestsStepContent(state: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
    ) {
        Text(
            text = stringResource(Res.string.onboarding_interests_title),
            style = MaterialTheme.togetherlyTypography.headlineM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
        Text(
            text = stringResource(Res.string.onboarding_interests_supporting),
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
            QuestCategory.entries.forEach { category ->
                TogetherlySelectableCard(
                    selected = category in state.selectedInterests,
                    onClick = { onAction(OnboardingAction.InterestToggled(category)) },
                    title = category.label(),
                    supportingText = category.description(),
                    modifier = Modifier.fillMaxWidth(),
                    multiSelect = true,
                    leadingContent = { CategoryDot(category) },
                )
            }
        }
        OnboardingFieldError(state, OnboardingField.INTERESTS)
    }
}

@Composable
private fun CategoryDot(category: QuestCategory) {
    Box(
        modifier = Modifier
            .size(MaterialTheme.togetherlySize.iconM)
            .background(category.color(), CircleShape)
            .clearAndSetSemantics { },
    )
}
