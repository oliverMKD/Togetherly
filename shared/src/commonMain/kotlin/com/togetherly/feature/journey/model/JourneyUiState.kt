package com.togetherly.feature.journey.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MemoryMediaId
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
 * [Content.transientError] is the one field beyond this feature's own spec snippet — added for the
 * same reason [com.togetherly.feature.today.presentation.TodayUiState.transientError] exists: a
 * failed [com.togetherly.feature.journey.presentation.JourneyAction.PlayVoiceClicked] must show a
 * non-destructive error without discarding [entries]/[stars], and [JourneyAction.TransientErrorDismissed]
 * needs a field to actually clear. Every other shape here is exactly as specified.
 *
 * [Content.playingVoiceId] is non-null exactly while that entry's clip is actively *playing* — a
 * paused clip clears it, matching the single-flag shape this state has room for; see
 * [com.togetherly.feature.journey.presentation.JourneyViewModel]'s own KDoc.
 */
@Immutable
sealed interface JourneyUiState {

    data object Loading : JourneyUiState

    data object Empty : JourneyUiState

    data class Content(
        val summary: JourneySummaryUi,
        val stars: PersistentList<JourneyStarUi> = persistentListOf(),
        val entries: PersistentList<JourneyEntryUi> = persistentListOf(),
        val playingVoiceId: MemoryMediaId? = null,
        val transientError: UiText? = null,
        /** Non-null exactly while the delete confirmation dialog (Step 13.5) is shown for this completion. */
        val pendingDeleteCompletionId: CompletionId? = null,
        val isDeleting: Boolean = false,
    ) : JourneyUiState

    data class Error(
        val message: UiText,
        val canRetry: Boolean,
    ) : JourneyUiState
}
