package com.togetherly.feature.journey.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.togetherly.core.ui.UiText
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.journey.JourneyMilestone
import com.togetherly.domain.journey.StarPosition
import com.togetherly.domain.journey.StarVisualVariant
import com.togetherly.feature.journey.model.JourneyEntryUi
import com.togetherly.feature.journey.model.JourneyStarUi
import com.togetherly.feature.journey.model.JourneySummaryUi
import com.togetherly.feature.journey.model.JourneyUiState
import com.togetherly.feature.journey.model.PrivatePhotoUi
import com.togetherly.feature.journey.model.ReactionUi
import com.togetherly.feature.journey.model.VoiceMemoryUi
import com.togetherly.feature.today.model.QuestCategoryUi
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.error_generic_message

/**
 * A living catalogue of Journey's rendered states — never a substitute for
 * [com.togetherly.feature.journey.presentation.JourneyViewModelTest] (behavior) or the Compose UI
 * tests (semantics/interaction); these only prove appearance.
 */

private fun previewEntry(
    id: String,
    questTitle: String? = "Backyard Scavenger Hunt",
    category: QuestCategoryUi? = QuestCategoryUi.DISCOVER,
    note: String? = null,
    reactions: List<ReactionUi> = emptyList(),
    photo: PrivatePhotoUi? = null,
    voice: VoiceMemoryUi? = null,
) = JourneyEntryUi(
    completionId = CompletionId(id),
    questTitle = questTitle,
    category = category,
    completedDate = "June 15, 2026",
    completedTime = "4:30 PM",
    note = note,
    reactions = reactions.toPersistentList(),
    photo = photo,
    voice = voice,
)

private fun previewStar(
    id: String,
    category: QuestCategoryUi?,
    x: Float,
    y: Float,
    hasPhoto: Boolean = false,
    hasVoice: Boolean = false,
) = JourneyStarUi(
    completionId = CompletionId(id),
    position = StarPosition(x, y),
    visualVariant = StarVisualVariant.MEDIUM,
    category = category,
    hasNote = false,
    hasPhoto = hasPhoto,
    hasVoice = hasVoice,
)

private fun previewContent(
    entries: List<JourneyEntryUi>,
    totalCompletions: Int = entries.size,
    activeDayCount: Int = entries.size,
    milestones: Set<JourneyMilestone> = emptySet(),
    playingVoiceId: MemoryMediaId? = null,
    transientError: UiText? = null,
) = JourneyUiState.Content(
    summary = JourneySummaryUi(
        totalCompletions = totalCompletions,
        activeDayCount = activeDayCount,
        achievedMilestones = milestones.toPersistentSet(),
    ),
    stars = entries
        .mapIndexed { index, entry ->
            val fraction = (index * 0.15f).coerceAtMost(0.6f)
            previewStar(
                entry.completionId.value,
                entry.category,
                x = 0.2f + fraction,
                y = 0.3f + fraction,
                hasPhoto = entry.photo != null,
                hasVoice = entry.voice != null,
            )
        }
        .toPersistentList(),
    entries = entries.toPersistentList(),
    playingVoiceId = playingVoiceId,
    transientError = transientError,
)

@Composable
private fun JourneyPreview(state: JourneyUiState, darkTheme: Boolean = false) {
    TogetherlyTheme(darkTheme = darkTheme) {
        JourneyScreen(state = state, onAction = {}, loadPhotoBytes = { null })
    }
}

@Preview
@Composable
private fun JourneyLoadingPreview() {
    JourneyPreview(JourneyUiState.Loading)
}

@Preview
@Composable
private fun JourneyEmptyPreview() {
    JourneyPreview(JourneyUiState.Empty)
}

@Preview
@Composable
private fun JourneyErrorPreview() {
    JourneyPreview(JourneyUiState.Error(message = UiText.Resource(Res.string.error_generic_message), canRetry = true))
}

@Preview
@Composable
private fun JourneyOneCompletionPreview() {
    JourneyPreview(previewContent(entries = listOf(previewEntry("c1")), milestones = setOf(JourneyMilestone.FIRST_STAR)))
}

@Preview
@Composable
private fun JourneyMultipleEntriesPreview() {
    JourneyPreview(
        previewContent(
            entries = listOf(
                previewEntry("c1", questTitle = "Backyard Scavenger Hunt", category = QuestCategoryUi.DISCOVER),
                previewEntry("c2", questTitle = "Kindness Note", category = QuestCategoryUi.KINDNESS),
                previewEntry("c3", questTitle = "Silly Dance Party", category = QuestCategoryUi.SILLY),
            ),
            milestones = setOf(JourneyMilestone.FIRST_STAR, JourneyMilestone.THREE_STARS),
        ),
    )
}

@Preview
@Composable
private fun JourneyMissingQuestPreview() {
    JourneyPreview(previewContent(entries = listOf(previewEntry("c1", questTitle = null, category = null))))
}

@Preview
@Composable
private fun JourneyNoteOnlyPreview() {
    JourneyPreview(previewContent(entries = listOf(previewEntry("c1", note = "We found every hidden treasure!"))))
}

@Preview
@Composable
private fun JourneyPhotoPreview() {
    JourneyPreview(
        previewContent(entries = listOf(previewEntry("c1", photo = PrivatePhotoUi(MediaReference("completions/c1/photo-1.jpg"))))),
    )
}

@Preview
@Composable
private fun JourneyVoicePreview() {
    val voice = VoiceMemoryUi(MemoryMediaId("media-1"), MediaReference("completions/c1/voice-1.m4a"), "0:24")
    JourneyPreview(
        previewContent(entries = listOf(previewEntry("c1", voice = voice)), playingVoiceId = voice.mediaId),
    )
}

@Preview
@Composable
private fun JourneyMixedMemoriesPreview() {
    val voice = VoiceMemoryUi(MemoryMediaId("media-1"), MediaReference("completions/c1/voice-1.m4a"), "0:18")
    JourneyPreview(
        previewContent(
            entries = listOf(
                previewEntry(
                    "c1",
                    note = "So much laughing today.",
                    reactions = listOf(ReactionUi(UiText.Dynamic("Loved it"), "🥰"), ReactionUi(UiText.Dynamic("Surprised"), "😲")),
                    photo = PrivatePhotoUi(MediaReference("completions/c1/photo-1.jpg")),
                    voice = voice,
                ),
            ),
            transientError = UiText.Resource(Res.string.error_generic_message),
        ),
    )
}

@Preview
@Composable
private fun JourneyMixedMemoriesDarkPreview() {
    val voice = VoiceMemoryUi(MemoryMediaId("media-1"), MediaReference("completions/c1/voice-1.m4a"), "0:18")
    JourneyPreview(
        state = previewContent(
            entries = listOf(
                previewEntry(
                    "c1",
                    note = "So much laughing today.",
                    reactions = listOf(ReactionUi(UiText.Dynamic("Loved it"), "🥰")),
                    photo = PrivatePhotoUi(MediaReference("completions/c1/photo-1.jpg")),
                    voice = voice,
                ),
            ),
        ),
        darkTheme = true,
    )
}

@Preview(fontScale = 2f)
@Composable
private fun JourneyLargeFontPreview() {
    JourneyPreview(
        previewContent(
            entries = listOf(previewEntry("c1", note = "A note that should still read clearly at a large font size.")),
        ),
    )
}
