package com.togetherly.feature.family.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.component.selection.TogetherlyChipFlowRow
import com.togetherly.designsystem.component.selection.TogetherlyChoiceChip
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.feature.family.model.QuestPreferencesAction
import com.togetherly.feature.family.model.QuestPreferencesField
import com.togetherly.feature.family.model.QuestPreferencesUiState
import com.togetherly.feature.onboarding.presentation.label
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.family_discard_dialog_body
import togetherly.shared.generated.resources.family_discard_dialog_cancel_action
import togetherly.shared.generated.resources.family_discard_dialog_confirm_action
import togetherly.shared.generated.resources.family_discard_dialog_title
import togetherly.shared.generated.resources.quest_preferences_durations_title
import togetherly.shared.generated.resources.quest_preferences_energy_title
import togetherly.shared.generated.resources.quest_preferences_explanation
import togetherly.shared.generated.resources.quest_preferences_location_title
import togetherly.shared.generated.resources.quest_preferences_preparation_title
import togetherly.shared.generated.resources.quest_preferences_reset_action
import togetherly.shared.generated.resources.quest_preferences_save_action
import togetherly.shared.generated.resources.quest_preferences_title
import togetherly.shared.generated.resources.ds_component_back_content_description

@Composable
internal fun QuestPreferencesScreen(
    state: QuestPreferencesUiState,
    onAction: (QuestPreferencesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                title = stringResource(Res.string.quest_preferences_title),
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = stringResource(Res.string.ds_component_back_content_description),
                        onClick = { onAction(QuestPreferencesAction.BackClicked) },
                    )
                },
            )
        },
    ) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TogetherlyLoadingIndicator()
            }
            else -> QuestPreferencesContent(state = state, onAction = onAction)
        }
    }

    if (state.showDiscardDialog) {
        QuestPreferencesDiscardDialog(onAction = onAction)
    }
}

@Composable
private fun QuestPreferencesContent(
    state: QuestPreferencesUiState,
    onAction: (QuestPreferencesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
    ) {
        Text(
            text = stringResource(Res.string.quest_preferences_explanation),
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )

        if (state.error != null) {
            TogetherlyInlineError(message = state.error.asString())
        }

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
            Text(
                text = stringResource(Res.string.quest_preferences_durations_title),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            TogetherlyChipFlowRow(modifier = Modifier.fillMaxWidth()) {
                DurationBand.entries.forEach { duration ->
                    TogetherlyChoiceChip(
                        label = duration.label(),
                        selected = duration in state.selectedDurations,
                        onClick = { onAction(QuestPreferencesAction.DurationToggled(duration)) },
                    )
                }
            }
            state.validationErrors[QuestPreferencesField.DURATIONS]?.let { error ->
                Text(text = error.asString(), style = MaterialTheme.togetherlyTypography.bodyS, color = MaterialTheme.togetherlyColors.error)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
            Text(
                text = stringResource(Res.string.quest_preferences_energy_title),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            TogetherlyChipFlowRow(modifier = Modifier.fillMaxWidth()) {
                EnergyLevel.entries.forEach { energy ->
                    TogetherlyChoiceChip(
                        label = energy.label(),
                        selected = energy in state.selectedEnergyLevels,
                        onClick = { onAction(QuestPreferencesAction.EnergyToggled(energy)) },
                    )
                }
            }
            state.validationErrors[QuestPreferencesField.ENERGY]?.let { error ->
                Text(text = error.asString(), style = MaterialTheme.togetherlyTypography.bodyS, color = MaterialTheme.togetherlyColors.error)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
            Text(
                text = stringResource(Res.string.quest_preferences_location_title),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            TogetherlyChipFlowRow(modifier = Modifier.fillMaxWidth()) {
                LocationPreference.entries.forEach { location ->
                    TogetherlyChoiceChip(
                        label = location.label(),
                        selected = location == state.locationPreference,
                        onClick = { onAction(QuestPreferencesAction.LocationPreferenceChanged(location)) },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
            Text(
                text = stringResource(Res.string.quest_preferences_preparation_title),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            TogetherlyChipFlowRow(modifier = Modifier.fillMaxWidth()) {
                PreparationPreference.entries.forEach { preparation ->
                    TogetherlyChoiceChip(
                        label = preparation.label(),
                        selected = preparation == state.preparationPreference,
                        onClick = { onAction(QuestPreferencesAction.PreparationPreferenceChanged(preparation)) },
                    )
                }
            }
        }

        TogetherlyTextButton(
            label = stringResource(Res.string.quest_preferences_reset_action),
            onClick = { onAction(QuestPreferencesAction.ResetToDefaultsClicked) },
        )

        TogetherlyPrimaryButton(
            label = stringResource(Res.string.quest_preferences_save_action),
            onClick = { onAction(QuestPreferencesAction.SaveClicked) },
            modifier = Modifier.fillMaxWidth(),
            loading = state.isSaving,
        )
    }
}

@Composable
private fun QuestPreferencesDiscardDialog(onAction: (QuestPreferencesAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(QuestPreferencesAction.DismissDiscardDialog) },
        title = { Text(stringResource(Res.string.family_discard_dialog_title), style = MaterialTheme.togetherlyTypography.titleL) },
        text = { Text(stringResource(Res.string.family_discard_dialog_body), style = MaterialTheme.togetherlyTypography.bodyM) },
        confirmButton = {
            TextButton(onClick = { onAction(QuestPreferencesAction.DiscardConfirmed) }) {
                Text(stringResource(Res.string.family_discard_dialog_confirm_action), color = MaterialTheme.togetherlyColors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(QuestPreferencesAction.DismissDiscardDialog) }) {
                Text(stringResource(Res.string.family_discard_dialog_cancel_action))
            }
        },
    )
}
