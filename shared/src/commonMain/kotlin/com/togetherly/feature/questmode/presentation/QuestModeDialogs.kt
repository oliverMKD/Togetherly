package com.togetherly.feature.questmode.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.togetherly.designsystem.component.card.TogetherlyCard
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.questmode_abandon_cancel_action
import togetherly.shared.generated.resources.questmode_abandon_confirm_action
import togetherly.shared.generated.resources.questmode_abandon_confirm_body
import togetherly.shared.generated.resources.questmode_abandon_confirm_title
import togetherly.shared.generated.resources.questmode_exit_abandon_action
import togetherly.shared.generated.resources.questmode_exit_body
import togetherly.shared.generated.resources.questmode_exit_continue_action
import togetherly.shared.generated.resources.questmode_exit_keep_action
import togetherly.shared.generated.resources.questmode_exit_title

/**
 * A custom [Dialog] rather than [AlertDialog] specifically because this needs three distinct
 * actions — "Keep for later" (navigate back, session retained), "Continue quest" (dismiss, stay
 * here), "Abandon quest" (opens [AbandonConfirmationDialog], never abandons directly) —
 * [AlertDialog] only ever offers two button slots. Leaving via any of the first two never abandons
 * anything by itself.
 */
@Composable
internal fun ExitConfirmationDialog(onAction: (QuestModeAction) -> Unit) {
    Dialog(onDismissRequest = { onAction(QuestModeAction.DialogDismissed) }) {
        TogetherlyCard {
            Column(
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.l),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s),
            ) {
                Text(stringResource(Res.string.questmode_exit_title), style = MaterialTheme.togetherlyTypography.titleL)
                Text(stringResource(Res.string.questmode_exit_body), style = MaterialTheme.togetherlyTypography.bodyM)

                TextButton(onClick = { onAction(QuestModeAction.KeepInProgressClicked) }) {
                    Text(stringResource(Res.string.questmode_exit_keep_action))
                }
                TextButton(onClick = { onAction(QuestModeAction.DialogDismissed) }) {
                    Text(stringResource(Res.string.questmode_exit_continue_action))
                }
                TextButton(onClick = { onAction(QuestModeAction.AbandonClicked) }) {
                    Text(
                        text = stringResource(Res.string.questmode_exit_abandon_action),
                        color = MaterialTheme.togetherlyColors.error,
                    )
                }
            }
        }
    }
}

/**
 * The second, explicitly destructive confirmation — only reachable from [ExitConfirmationDialog]'s
 * own "Abandon quest" choice, never a single tap away from the normal exit path.
 */
@Composable
internal fun AbandonConfirmationDialog(onAction: (QuestModeAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(QuestModeAction.DialogDismissed) },
        title = { Text(stringResource(Res.string.questmode_abandon_confirm_title), style = MaterialTheme.togetherlyTypography.titleL) },
        text = { Text(stringResource(Res.string.questmode_abandon_confirm_body), style = MaterialTheme.togetherlyTypography.bodyM) },
        confirmButton = {
            TextButton(onClick = { onAction(QuestModeAction.AbandonConfirmed) }) {
                Text(
                    text = stringResource(Res.string.questmode_abandon_confirm_action),
                    color = MaterialTheme.togetherlyColors.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(QuestModeAction.DialogDismissed) }) {
                Text(stringResource(Res.string.questmode_abandon_cancel_action))
            }
        },
    )
}
