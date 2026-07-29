package com.togetherly.feature.journey.presentation

import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMediaId

sealed interface JourneyAction {
    data object ScreenStarted : JourneyAction
    data object RetryClicked : JourneyAction
    data object GoToTodayClicked : JourneyAction
    data class PlayVoiceClicked(val mediaId: MemoryMediaId, val reference: MediaReference) : JourneyAction
    data object PauseVoiceClicked : JourneyAction
    data object StopVoiceClicked : JourneyAction
    data object TransientErrorDismissed : JourneyAction

    /** [DeleteClicked] only ever shows the confirmation dialog — [ConfirmDeleteClicked] is the one action that actually calls [com.togetherly.domain.completion.usecase.DeleteCompletion]. */
    data class DeleteClicked(val completionId: CompletionId) : JourneyAction
    data object ConfirmDeleteClicked : JourneyAction
    data object DismissDeleteDialog : JourneyAction
}
