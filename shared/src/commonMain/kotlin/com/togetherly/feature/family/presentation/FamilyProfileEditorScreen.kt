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
import com.togetherly.designsystem.component.card.TogetherlySelectableCard
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.component.input.TogetherlyTextField
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.component.selection.TogetherlyChipFlowRow
import com.togetherly.designsystem.component.selection.TogetherlyChoiceChip
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyDisplayName
import com.togetherly.domain.family.LocationPreference
import com.togetherly.feature.family.model.FamilyProfileAction
import com.togetherly.feature.family.model.FamilyProfileField
import com.togetherly.feature.family.model.FamilyProfileUiState
import com.togetherly.feature.onboarding.presentation.label
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.family_discard_dialog_body
import togetherly.shared.generated.resources.family_discard_dialog_cancel_action
import togetherly.shared.generated.resources.family_discard_dialog_confirm_action
import togetherly.shared.generated.resources.family_discard_dialog_title
import togetherly.shared.generated.resources.family_profile_editor_age_bands_title
import togetherly.shared.generated.resources.family_profile_editor_durations_title
import togetherly.shared.generated.resources.family_profile_editor_location_title
import togetherly.shared.generated.resources.family_profile_editor_save_action
import togetherly.shared.generated.resources.family_profile_editor_title
import togetherly.shared.generated.resources.onboarding_family_name_label
import togetherly.shared.generated.resources.onboarding_family_name_placeholder

/** Shows the remaining-characters counter only once close to the limit — same threshold as [com.togetherly.feature.onboarding.presentation.steps.FamilyNameStepContent]. */
private const val SHOW_COUNTER_WITHIN_REMAINING = 20

@Composable
internal fun FamilyProfileEditorScreen(
    state: FamilyProfileUiState,
    onAction: (FamilyProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                title = stringResource(Res.string.family_profile_editor_title),
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = "Back",
                        onClick = { onAction(FamilyProfileAction.BackClicked) },
                    )
                },
            )
        },
    ) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TogetherlyLoadingIndicator()
            }
            else -> FamilyProfileEditorContent(state = state, onAction = onAction)
        }
    }

    if (state.showDiscardDialog) {
        DiscardChangesDialog(onAction = onAction)
    }
}

@Composable
private fun FamilyProfileEditorContent(
    state: FamilyProfileUiState,
    onAction: (FamilyProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
    ) {
        if (state.error != null) {
            TogetherlyInlineError(message = state.error.asString())
        }

        val remaining = FamilyDisplayName.MAX_LENGTH - state.familyName.length
        TogetherlyTextField(
            value = state.familyName,
            onValueChange = { onAction(FamilyProfileAction.FamilyNameChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.onboarding_family_name_label),
            placeholder = stringResource(Res.string.onboarding_family_name_placeholder),
            errorText = state.validationErrors[FamilyProfileField.FAMILY_NAME]?.asString(),
            singleLine = true,
            characterLimit = FamilyDisplayName.MAX_LENGTH.takeIf { remaining <= SHOW_COUNTER_WITHIN_REMAINING },
        )

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
            Text(
                text = stringResource(Res.string.family_profile_editor_age_bands_title),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            AgeBand.entries.forEach { band ->
                TogetherlySelectableCard(
                    selected = band in state.selectedAgeBands,
                    onClick = { onAction(FamilyProfileAction.AgeBandToggled(band)) },
                    title = band.label(),
                    modifier = Modifier.fillMaxWidth(),
                    multiSelect = true,
                )
            }
            state.validationErrors[FamilyProfileField.AGE_BANDS]?.let { error ->
                Text(text = error.asString(), style = MaterialTheme.togetherlyTypography.bodyS, color = MaterialTheme.togetherlyColors.error)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
            Text(
                text = stringResource(Res.string.family_profile_editor_durations_title),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            TogetherlyChipFlowRow(modifier = Modifier.fillMaxWidth()) {
                DurationBand.entries.forEach { duration ->
                    TogetherlyChoiceChip(
                        label = duration.label(),
                        selected = duration in state.selectedDurations,
                        onClick = { onAction(FamilyProfileAction.DurationToggled(duration)) },
                    )
                }
            }
            state.validationErrors[FamilyProfileField.DURATIONS]?.let { error ->
                Text(text = error.asString(), style = MaterialTheme.togetherlyTypography.bodyS, color = MaterialTheme.togetherlyColors.error)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
            Text(
                text = stringResource(Res.string.family_profile_editor_location_title),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            TogetherlyChipFlowRow(modifier = Modifier.fillMaxWidth()) {
                LocationPreference.entries.forEach { location ->
                    TogetherlyChoiceChip(
                        label = location.label(),
                        selected = location == state.locationPreference,
                        onClick = { onAction(FamilyProfileAction.LocationPreferenceChanged(location)) },
                    )
                }
            }
        }

        TogetherlyPrimaryButton(
            label = stringResource(Res.string.family_profile_editor_save_action),
            onClick = { onAction(FamilyProfileAction.SaveClicked) },
            modifier = Modifier.fillMaxWidth(),
            loading = state.isSaving,
        )
    }
}

@Composable
private fun DiscardChangesDialog(onAction: (FamilyProfileAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(FamilyProfileAction.DismissDiscardDialog) },
        title = { Text(stringResource(Res.string.family_discard_dialog_title), style = MaterialTheme.togetherlyTypography.titleL) },
        text = { Text(stringResource(Res.string.family_discard_dialog_body), style = MaterialTheme.togetherlyTypography.bodyM) },
        confirmButton = {
            TextButton(onClick = { onAction(FamilyProfileAction.DiscardConfirmed) }) {
                Text(stringResource(Res.string.family_discard_dialog_confirm_action), color = MaterialTheme.togetherlyColors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(FamilyProfileAction.DismissDiscardDialog) }) {
                Text(stringResource(Res.string.family_discard_dialog_cancel_action))
            }
        },
    )
}
