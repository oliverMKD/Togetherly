package com.togetherly.feature.onboarding.presentation.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.core.datetime.localizedTimeDisplay
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.card.TogetherlyCard
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.feature.onboarding.model.OnboardingUiState
import com.togetherly.feature.onboarding.presentation.OnboardingAction
import com.togetherly.feature.onboarding.presentation.label
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.onboarding_review_field_age_bands
import togetherly.shared.generated.resources.onboarding_review_field_durations
import togetherly.shared.generated.resources.onboarding_review_field_interests
import togetherly.shared.generated.resources.onboarding_review_field_location
import togetherly.shared.generated.resources.onboarding_review_field_preparation
import togetherly.shared.generated.resources.onboarding_review_field_reminder
import togetherly.shared.generated.resources.onboarding_review_field_team_name
import togetherly.shared.generated.resources.onboarding_review_no_reminder
import togetherly.shared.generated.resources.onboarding_review_reminder_value
import togetherly.shared.generated.resources.onboarding_review_team_name_default
import togetherly.shared.generated.resources.onboarding_review_title

@Composable
internal fun ReviewStepContent(state: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
    ) {
        Text(
            text = stringResource(Res.string.onboarding_review_title),
            style = MaterialTheme.togetherlyTypography.headlineM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )

        val teamName = state.familyName.trim().ifEmpty { stringResource(Res.string.onboarding_review_team_name_default) }
        val ageBandsText = AgeBand.entries.filter { it in state.selectedAgeBands }.map { it.label() }.joinToString(", ")
        val interestsText = QuestCategory.entries.filter { it in state.selectedInterests }.map { it.label() }.joinToString(", ")
        val durationsText = DurationBand.entries.filter { it in state.selectedDurations }.map { it.label() }.joinToString(", ")
        val reminderText = if (state.reminderEnabled && state.reminderDays.isNotEmpty() && state.reminderTime != null) {
            val days = DayOfWeek.entries.filter { it in state.reminderDays }.map { it.label() }.joinToString(", ")
            stringResource(Res.string.onboarding_review_reminder_value, days, state.reminderTime.localizedTimeDisplay())
        } else {
            stringResource(Res.string.onboarding_review_no_reminder)
        }

        TogetherlyCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                ReviewRow(stringResource(Res.string.onboarding_review_field_team_name), teamName)
                ReviewDivider()
                ReviewRow(stringResource(Res.string.onboarding_review_field_age_bands), ageBandsText)
                ReviewDivider()
                ReviewRow(stringResource(Res.string.onboarding_review_field_interests), interestsText)
                ReviewDivider()
                ReviewRow(stringResource(Res.string.onboarding_review_field_durations), durationsText)
                ReviewDivider()
                ReviewRow(stringResource(Res.string.onboarding_review_field_location), state.locationPreference.label())
                ReviewDivider()
                ReviewRow(stringResource(Res.string.onboarding_review_field_preparation), state.preparationPreference.label())
                ReviewDivider()
                ReviewRow(stringResource(Res.string.onboarding_review_field_reminder), reminderText)
            }
        }

        val saveError = state.saveError
        if (saveError != null) {
            TogetherlyInlineError(
                message = saveError.asString(),
                onRetry = { onAction(OnboardingAction.RetrySaveClicked) },
            )
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xxs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.togetherlyTypography.labelM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.togetherlyTypography.bodyL,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
    }
}

@Composable
private fun ReviewDivider() {
    HorizontalDivider(color = MaterialTheme.togetherlyColors.borderSubtle)
}
