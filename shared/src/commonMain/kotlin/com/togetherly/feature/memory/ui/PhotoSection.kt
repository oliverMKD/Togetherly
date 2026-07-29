package com.togetherly.feature.memory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.togetherly.designsystem.component.button.TogetherlySecondaryButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.memory.model.CompletionMemoryUiState
import com.togetherly.feature.memory.model.PhotoPreviewReference
import com.togetherly.feature.memory.presentation.CompletionMemoryAction
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.memory_photo_add_action
import togetherly.shared.generated.resources.memory_photo_heading
import togetherly.shared.generated.resources.memory_photo_privacy_note
import togetherly.shared.generated.resources.memory_photo_remove_action
import togetherly.shared.generated.resources.memory_photo_replace_action
import togetherly.shared.generated.resources.memory_photo_thumbnail_description

@Composable
internal fun PhotoSection(
    state: CompletionMemoryUiState,
    onAction: (CompletionMemoryAction) -> Unit,
    loadPhotoBytes: suspend (PhotoPreviewReference) -> ByteArray?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
        Text(
            text = stringResource(Res.string.memory_photo_heading),
            style = MaterialTheme.togetherlyTypography.titleM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )

        val photo = state.photo
        if (photo != null) {
            PrivatePhotoThumbnail(
                reference = photo.reference,
                loadBytes = { loadPhotoBytes(photo.reference) },
                contentDescription = stringResource(Res.string.memory_photo_thumbnail_description),
                modifier = Modifier.size(120.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
                TogetherlyTextButton(
                    label = stringResource(Res.string.memory_photo_replace_action),
                    onClick = { onAction(CompletionMemoryAction.AddPhotoClicked) },
                    enabled = !state.isSaving,
                )
                TogetherlyTextButton(
                    label = stringResource(Res.string.memory_photo_remove_action),
                    onClick = { onAction(CompletionMemoryAction.RemovePhotoClicked) },
                    enabled = !state.isSaving,
                )
            }
        } else {
            TogetherlySecondaryButton(
                label = stringResource(Res.string.memory_photo_add_action),
                onClick = { onAction(CompletionMemoryAction.AddPhotoClicked) },
                enabled = !state.isSaving,
            )
        }

        Text(
            text = stringResource(Res.string.memory_photo_privacy_note),
            style = MaterialTheme.togetherlyTypography.bodyS,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
    }
}
