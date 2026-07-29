package com.togetherly.feature.memory.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.memory.presentation.CompletionMemoryAction
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.memory_discard_body
import togetherly.shared.generated.resources.memory_discard_cancel_action
import togetherly.shared.generated.resources.memory_discard_confirm_action
import togetherly.shared.generated.resources.memory_discard_title

/**
 * Only reachable from [CompletionMemoryAction.BackClicked] when there are unsaved changes — see
 * [com.togetherly.feature.memory.presentation.CompletionMemoryViewModel.hasUnsavedChanges]'s own
 * KDoc for what counts as "unsaved." Discarding deletes only pending (never-committed) files —
 * see [com.togetherly.domain.completion.usecase.DiscardCompletionMemoryDraft]'s own KDoc.
 */
@Composable
internal fun DiscardConfirmationDialog(onAction: (CompletionMemoryAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(CompletionMemoryAction.DialogDismissed) },
        title = { Text(stringResource(Res.string.memory_discard_title), style = MaterialTheme.togetherlyTypography.titleL) },
        text = { Text(stringResource(Res.string.memory_discard_body), style = MaterialTheme.togetherlyTypography.bodyM) },
        confirmButton = {
            TextButton(onClick = { onAction(CompletionMemoryAction.DiscardConfirmed) }) {
                Text(
                    text = stringResource(Res.string.memory_discard_confirm_action),
                    color = MaterialTheme.togetherlyColors.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(CompletionMemoryAction.DialogDismissed) }) {
                Text(stringResource(Res.string.memory_discard_cancel_action))
            }
        },
    )
}
