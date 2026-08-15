package com.togetherly.feature.family.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.button.TogetherlySecondaryButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.component.card.TogetherlyCard
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.component.gate.TogetherlyParentalGateDialog
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.family.model.DataManagementAction
import com.togetherly.feature.family.model.DataManagementConfirmationStage
import com.togetherly.feature.family.model.DataManagementUiState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.data_management_delete_all_action
import togetherly.shared.generated.resources.data_management_delete_all_description
import togetherly.shared.generated.resources.data_management_delete_all_stage_one_body
import togetherly.shared.generated.resources.data_management_delete_all_stage_one_continue_action
import togetherly.shared.generated.resources.data_management_delete_all_stage_one_title
import togetherly.shared.generated.resources.data_management_delete_memories_action
import togetherly.shared.generated.resources.data_management_delete_memories_confirm_body
import togetherly.shared.generated.resources.data_management_delete_memories_confirm_title
import togetherly.shared.generated.resources.data_management_delete_memories_description
import togetherly.shared.generated.resources.data_management_dialog_cancel_action
import togetherly.shared.generated.resources.data_management_intro
import togetherly.shared.generated.resources.data_management_reset_quest_history_action
import togetherly.shared.generated.resources.data_management_reset_quest_history_confirm_body
import togetherly.shared.generated.resources.data_management_reset_quest_history_confirm_title
import togetherly.shared.generated.resources.data_management_reset_quest_history_description
import togetherly.shared.generated.resources.data_management_title
import togetherly.shared.generated.resources.ds_component_dismiss
import togetherly.shared.generated.resources.ds_component_back_content_description

@Composable
internal fun DataManagementScreen(
    state: DataManagementUiState,
    onAction: (DataManagementAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                title = stringResource(Res.string.data_management_title),
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = stringResource(Res.string.ds_component_back_content_description),
                        onClick = { onAction(DataManagementAction.BackClicked) },
                    )
                },
            )
        },
    ) {
        if (state.isBusy) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TogetherlyLoadingIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
            ) {
                Text(
                    text = stringResource(Res.string.data_management_intro),
                    style = MaterialTheme.togetherlyTypography.bodyM,
                    color = MaterialTheme.togetherlyColors.foregroundSecondary,
                )
                if (state.message != null) {
                    TogetherlyInlineError(message = state.message.asString())
                    TogetherlyTextButton(
                        label = stringResource(Res.string.ds_component_dismiss),
                        onClick = { onAction(DataManagementAction.MessageDismissed) },
                    )
                }
                DestructiveActionRow(
                    titleResource = Res.string.data_management_delete_memories_action,
                    descriptionResource = Res.string.data_management_delete_memories_description,
                    onClick = { onAction(DataManagementAction.DeleteMemoriesClicked) },
                )
                DestructiveActionRow(
                    titleResource = Res.string.data_management_reset_quest_history_action,
                    descriptionResource = Res.string.data_management_reset_quest_history_description,
                    onClick = { onAction(DataManagementAction.ResetQuestHistoryClicked) },
                )
                DestructiveActionRow(
                    titleResource = Res.string.data_management_delete_all_action,
                    descriptionResource = Res.string.data_management_delete_all_description,
                    onClick = { onAction(DataManagementAction.DeleteAllDataClicked) },
                )
            }
        }
    }

    DataManagementDialogs(state = state, onAction = onAction)
}

/**
 * The entry-point row for one destructive action — always a neutral [TogetherlySecondaryButton],
 * never colored to signal danger on its own (see this screen's own accessibility requirement:
 * destructive actions are never identified by color alone). The action's own unmistakable label
 * ("Delete memories", "Reset quest history", "Delete all local data" — never an ambiguous label
 * like a bare "Reset") plus the description underneath it are what communicate consequence.
 */
@Composable
private fun DestructiveActionRow(
    titleResource: StringResource,
    descriptionResource: StringResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s),
        ) {
            Text(
                text = stringResource(titleResource),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            Text(
                text = stringResource(descriptionResource),
                style = MaterialTheme.togetherlyTypography.bodyM,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )
            TogetherlySecondaryButton(
                label = stringResource(titleResource),
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DataManagementDialogs(
    state: DataManagementUiState,
    onAction: (DataManagementAction) -> Unit,
) {
    when (state.confirmationStage) {
        DataManagementConfirmationStage.NONE -> Unit

        DataManagementConfirmationStage.CONFIRM_DELETE_MEMORIES -> DestructiveConfirmDialog(
            titleResource = Res.string.data_management_delete_memories_confirm_title,
            bodyResource = Res.string.data_management_delete_memories_confirm_body,
            confirmLabelResource = Res.string.data_management_delete_memories_action,
            onConfirm = { onAction(DataManagementAction.DestructiveActionConfirmed) },
            onDismiss = { onAction(DataManagementAction.ConfirmationDismissed) },
        )

        DataManagementConfirmationStage.CONFIRM_RESET_QUEST_HISTORY -> DestructiveConfirmDialog(
            titleResource = Res.string.data_management_reset_quest_history_confirm_title,
            bodyResource = Res.string.data_management_reset_quest_history_confirm_body,
            confirmLabelResource = Res.string.data_management_reset_quest_history_action,
            onConfirm = { onAction(DataManagementAction.DestructiveActionConfirmed) },
            onDismiss = { onAction(DataManagementAction.ConfirmationDismissed) },
        )

        DataManagementConfirmationStage.DELETE_ALL_STAGE_ONE -> DestructiveConfirmDialog(
            titleResource = Res.string.data_management_delete_all_stage_one_title,
            bodyResource = Res.string.data_management_delete_all_stage_one_body,
            confirmLabelResource = Res.string.data_management_delete_all_stage_one_continue_action,
            onConfirm = { onAction(DataManagementAction.DeleteAllDataContinueClicked) },
            onDismiss = { onAction(DataManagementAction.ConfirmationDismissed) },
        )

        DataManagementConfirmationStage.DELETE_ALL_STAGE_TWO -> TogetherlyParentalGateDialog(
            onConfirmed = { onAction(DataManagementAction.DestructiveActionConfirmed) },
            onDismiss = { onAction(DataManagementAction.ConfirmationDismissed) },
        )
    }
}

/**
 * Same shape as every per-screen discard dialog in this feature area (see
 * `FamilyProfileEditorScreen.kt`'s own `DiscardChangesDialog`) — a plain M3 [AlertDialog] with an
 * error-tinted [TextButton] for the destructive action. Color is additive here, never the sole
 * signal: [confirmLabelResource] is always the action's own explicit name, never "OK"/"Confirm".
 */
@Composable
private fun DestructiveConfirmDialog(
    titleResource: StringResource,
    bodyResource: StringResource,
    confirmLabelResource: StringResource,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleResource), style = MaterialTheme.togetherlyTypography.titleL) },
        text = { Text(stringResource(bodyResource), style = MaterialTheme.togetherlyTypography.bodyM) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(confirmLabelResource), color = MaterialTheme.togetherlyColors.error)
            }
        },
        dismissButton = {
            TogetherlyTextButton(label = stringResource(Res.string.data_management_dialog_cancel_action), onClick = onDismiss)
        },
    )
}
