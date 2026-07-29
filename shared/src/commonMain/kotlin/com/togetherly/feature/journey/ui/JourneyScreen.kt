package com.togetherly.feature.journey.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.completion.MediaReference
import com.togetherly.feature.journey.model.JourneyUiState
import com.togetherly.feature.journey.presentation.JourneyAction
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.ds_component_dismiss
import togetherly.shared.generated.resources.journey_delete_cancel_action
import togetherly.shared.generated.resources.journey_delete_confirm_action
import togetherly.shared.generated.resources.journey_delete_dialog_body
import togetherly.shared.generated.resources.journey_delete_dialog_title
import togetherly.shared.generated.resources.journey_empty_action
import togetherly.shared.generated.resources.journey_empty_body
import togetherly.shared.generated.resources.journey_empty_title
import togetherly.shared.generated.resources.journey_top_bar_title

/**
 * Stateless top-level screen, the same Screen/Route split every other feature in this app follows
 * (see [com.togetherly.feature.memory.ui.CompletionMemoryScreen]'s own KDoc). [loadPhotoBytes] is
 * the one exception to "everything through state/onAction" — see
 * [JourneyTimelineEntry]'s own KDoc for why.
 *
 * `scrollable = false` on [TogetherlyScreen]: the [LazyColumn] below owns its own scrolling, and
 * nesting it inside another scrollable container would give it an unbounded height constraint.
 */
@Composable
fun JourneyScreen(
    state: JourneyUiState,
    onAction: (JourneyAction) -> Unit,
    loadPhotoBytes: suspend (MediaReference) -> ByteArray?,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        scrollable = false,
        topBar = { TogetherlyTopBar(title = stringResource(Res.string.journey_top_bar_title)) },
    ) {
        when (state) {
            JourneyUiState.Loading -> Unit
            JourneyUiState.Empty -> JourneyEmptyState(onAction = onAction)
            is JourneyUiState.Content -> JourneyContent(state = state, onAction = onAction, loadPhotoBytes = loadPhotoBytes)
            is JourneyUiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TogetherlyInlineError(
                    message = state.message.asString(),
                    onRetry = if (state.canRetry) ({ onAction(JourneyAction.RetryClicked) }) else null,
                )
            }
        }
    }

    if (state is JourneyUiState.Content && state.pendingDeleteCompletionId != null) {
        JourneyDeleteConfirmationDialog(isDeleting = state.isDeleting, onAction = onAction)
    }
}

@Composable
private fun JourneyDeleteConfirmationDialog(isDeleting: Boolean, onAction: (JourneyAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onAction(JourneyAction.DismissDeleteDialog) },
        title = { Text(stringResource(Res.string.journey_delete_dialog_title), style = MaterialTheme.togetherlyTypography.titleL) },
        text = { Text(stringResource(Res.string.journey_delete_dialog_body), style = MaterialTheme.togetherlyTypography.bodyM) },
        confirmButton = {
            TextButton(onClick = { onAction(JourneyAction.ConfirmDeleteClicked) }, enabled = !isDeleting) {
                Text(stringResource(Res.string.journey_delete_confirm_action), color = MaterialTheme.togetherlyColors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(JourneyAction.DismissDeleteDialog) }, enabled = !isDeleting) {
                Text(stringResource(Res.string.journey_delete_cancel_action))
            }
        },
    )
}

@Composable
private fun JourneyEmptyState(onAction: (JourneyAction) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s),
        ) {
            Text(
                text = stringResource(Res.string.journey_empty_title),
                style = MaterialTheme.togetherlyTypography.titleL,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            Text(
                text = stringResource(Res.string.journey_empty_body),
                style = MaterialTheme.togetherlyTypography.bodyM,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )
            TogetherlyTextButton(
                label = stringResource(Res.string.journey_empty_action),
                onClick = { onAction(JourneyAction.GoToTodayClicked) },
            )
        }
    }
}

@Composable
private fun JourneyContent(
    state: JourneyUiState.Content,
    onAction: (JourneyAction) -> Unit,
    loadPhotoBytes: suspend (MediaReference) -> ByteArray?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = MaterialTheme.togetherlySpacing.m),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
    ) {
        item(key = "constellation-header") {
            JourneyConstellationHeader(summary = state.summary, stars = state.stars)
        }

        state.transientError?.let { error ->
            item(key = "transient-error") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = error.asString(),
                        style = MaterialTheme.togetherlyTypography.bodyS,
                        color = MaterialTheme.togetherlyColors.error,
                        modifier = Modifier.weight(1f),
                    )
                    TogetherlyIconButton(
                        icon = { Text("✕", style = MaterialTheme.togetherlyTypography.labelM) },
                        contentDescription = stringResource(Res.string.ds_component_dismiss),
                        onClick = { onAction(JourneyAction.TransientErrorDismissed) },
                    )
                }
            }
        }

        items(items = state.entries, key = { it.completionId.value }) { entry ->
            JourneyTimelineEntry(
                entry = entry,
                isPlayingVoice = entry.voice != null && entry.voice.mediaId == state.playingVoiceId,
                onAction = onAction,
                loadPhotoBytes = loadPhotoBytes,
            )
        }
    }
}
