package com.togetherly.feature.journey.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.card.TogetherlyCard
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.core.ui.asString
import com.togetherly.domain.completion.MediaReference
import com.togetherly.feature.journey.model.JourneyEntryUi
import com.togetherly.feature.journey.presentation.JourneyAction
import com.togetherly.feature.memory.ui.PrivatePhotoThumbnail
import com.togetherly.feature.today.presentation.color
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.journey_delete_content_description
import togetherly.shared.generated.resources.journey_missing_quest_title
import togetherly.shared.generated.resources.journey_photo_content_description
import togetherly.shared.generated.resources.journey_voice_pause_action
import togetherly.shared.generated.resources.journey_voice_play_action

private val PHOTO_THUMBNAIL_SIZE = 72.dp

/**
 * One row of the private, chronological memory timeline. A missing [JourneyEntryUi.questTitle]
 * never hides the entry — it renders [Res.string.journey_missing_quest_title] instead (see
 * [com.togetherly.domain.journey.JourneyEntry]'s own KDoc on why the completion always stays
 * visible). [loadPhotoBytes] mirrors [com.togetherly.feature.memory.ui.CompletionMemoryScreen]'s
 * own seam: the one exception to "everything through state/onAction", since photo bytes are
 * loaded transiently and never stored in [com.togetherly.feature.journey.model.JourneyUiState].
 */
@Composable
internal fun JourneyTimelineEntry(
    entry: JourneyEntryUi,
    isPlayingVoice: Boolean,
    onAction: (JourneyAction) -> Unit,
    loadPhotoBytes: suspend (MediaReference) -> ByteArray?,
    modifier: Modifier = Modifier,
) {
    TogetherlyCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${entry.completedDate} · ${entry.completedTime}",
                    style = MaterialTheme.togetherlyTypography.labelM,
                    color = MaterialTheme.togetherlyColors.foregroundSecondary,
                )
                Spacer(modifier = Modifier.weight(1f))
                TogetherlyIconButton(
                    icon = { Text("✕", style = MaterialTheme.togetherlyTypography.bodyM) },
                    contentDescription = stringResource(Res.string.journey_delete_content_description),
                    onClick = { onAction(JourneyAction.DeleteClicked(entry.completionId)) },
                )
            }

            Text(
                text = entry.questTitle ?: stringResource(Res.string.journey_missing_quest_title),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = entry.category?.color() ?: MaterialTheme.togetherlyColors.foregroundPrimary,
            )

            if (entry.reactions.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
                    entry.reactions.forEach { reaction ->
                        Text(
                            text = "${reaction.emoji} ${reaction.label.asString()}",
                            style = MaterialTheme.togetherlyTypography.bodyS,
                            color = MaterialTheme.togetherlyColors.foregroundSecondary,
                        )
                    }
                }
            }

            entry.note?.let { note ->
                Text(text = note, style = MaterialTheme.togetherlyTypography.bodyM, color = MaterialTheme.togetherlyColors.foregroundPrimary)
            }

            entry.photo?.let { photo ->
                val contentDescription = stringResource(Res.string.journey_photo_content_description)
                PrivatePhotoThumbnail(
                    reference = photo.reference,
                    loadBytes = { loadPhotoBytes(photo.reference) },
                    contentDescription = contentDescription,
                    modifier = Modifier.size(PHOTO_THUMBNAIL_SIZE),
                )
            }

            entry.voice?.let { voice ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
                    TogetherlyIconButton(
                        icon = { Text(if (isPlayingVoice) "❚❚" else "▶", style = MaterialTheme.togetherlyTypography.titleM) },
                        contentDescription = stringResource(
                            if (isPlayingVoice) Res.string.journey_voice_pause_action else Res.string.journey_voice_play_action,
                        ),
                        onClick = {
                            onAction(
                                if (isPlayingVoice) {
                                    JourneyAction.PauseVoiceClicked
                                } else {
                                    JourneyAction.PlayVoiceClicked(voice.mediaId, voice.reference)
                                },
                            )
                        },
                    )
                    Text(
                        text = voice.durationLabel,
                        style = MaterialTheme.togetherlyTypography.bodyM,
                        color = MaterialTheme.togetherlyColors.foregroundPrimary,
                    )
                }
            }
        }
    }
}
