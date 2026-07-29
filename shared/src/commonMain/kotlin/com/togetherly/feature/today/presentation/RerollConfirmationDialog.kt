package com.togetherly.feature.today.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.designsystem.theme.togetherlyTypography
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.today_reroll_confirm_body
import togetherly.shared.generated.resources.today_reroll_confirm_title
import togetherly.shared.generated.resources.today_reroll_keep_action
import togetherly.shared.generated.resources.today_reroll_show_another_action

/**
 * Names the exact consequence before it happens — "use today's free swap" — rather than a vague
 * "are you sure?", since consuming the free reroll can't be undone once confirmed
 * ([RerollDailyQuest][com.togetherly.domain.daily.usecase.RerollDailyQuest] only ever records the
 * dismissal once a replacement is actually chosen, but from here on that's exactly what confirming
 * requests).
 */
@Composable
internal fun RerollConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(text = stringResource(Res.string.today_reroll_confirm_title), style = MaterialTheme.togetherlyTypography.titleL)
        },
        text = {
            Text(text = stringResource(Res.string.today_reroll_confirm_body), style = MaterialTheme.togetherlyTypography.bodyM)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.today_reroll_show_another_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.today_reroll_keep_action))
            }
        },
    )
}
