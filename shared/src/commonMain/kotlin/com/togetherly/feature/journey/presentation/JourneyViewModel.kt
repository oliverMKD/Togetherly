package com.togetherly.feature.journey.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.datetime.AppTimeZoneProvider
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.AnalyticsScreen
import com.togetherly.core.telemetry.MemoryDeleted
import com.togetherly.core.telemetry.MemoryOpened
import com.togetherly.core.telemetry.ProductAnalytics
import com.togetherly.core.ui.toUiText
import com.togetherly.data.media.VoicePlaybackController
import com.togetherly.data.media.VoicePlaybackState
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.usecase.DeleteCompletion
import com.togetherly.domain.journey.JourneyConstellationPolicy
import com.togetherly.domain.journey.JourneyEntry
import com.togetherly.domain.journey.deriveJourneySummary
import com.togetherly.domain.journey.repository.JourneyRepository
import com.togetherly.feature.journey.mapper.toUi
import com.togetherly.feature.journey.model.JourneyUiState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * No `Journey UI mapper`/`error mapper` dependency is injected — both are plain top-level
 * functions ([com.togetherly.feature.journey.mapper.toUi], [com.togetherly.core.ui.toUiText]),
 * the same convention [com.togetherly.feature.completion.presentation.CompletionCelebrationViewModel]
 * already follows for its own date/category mapping. [constellationPolicy] derives both the
 * decorative [JourneyUiState.Content.stars] and, via [deriveJourneySummary], the summary — from the
 * *same* [JourneyEntry] list each load produces, so Journey is never fetched twice per load.
 *
 * [observeJourney] uses [JourneyRepository.observeJourney] (live), not the one-shot
 * [JourneyRepository.getJourney] — this tab's `ViewModel` survives tab switches (`saveState`/
 * `restoreState` in [com.togetherly.navigation.shell.MainShell]), so [onScreenStarted]'s
 * [hasStarted] guard means a one-shot fetch would only ever run once per process, and a quest
 * completed on Today while Journey stays mounted in the back stack would never appear — the same
 * problem [com.togetherly.feature.today.presentation.TodayViewModel.onScreenStarted] already solves
 * for its own family-profile observer, for the identical reason. [journeyCollectionJob] exists so
 * [JourneyAction.RetryClicked] can restart the collection: [com.togetherly.data.catchStorageReadErrors]
 * terminates the underlying Flow after emitting an error, so recovering from one requires a fresh
 * [JourneyRepository.observeJourney] call, not just waiting on the old (now-completed) Flow.
 *
 * [voicePlaybackController] is injected directly (not Route-bound), the same pattern
 * [com.togetherly.feature.memory.presentation.CompletionMemoryViewModel] uses for its own voice
 * playback — see that class's own KDoc for why this one, unlike a photo picker or a permission
 * request, needs no platform Composable at the Route boundary. [lastRequestedVoiceId] exists only
 * because [VoicePlaybackState] itself carries no notion of *which* clip is active; combined with
 * that state it drives [JourneyUiState.Content.playingVoiceId], which is non-null only while a
 * clip is actively *playing* (not paused) — the field has room for exactly one flag, not two.
 */
class JourneyViewModel(
    private val journeyRepository: JourneyRepository,
    private val constellationPolicy: JourneyConstellationPolicy,
    private val voicePlaybackController: VoicePlaybackController,
    private val deleteCompletion: DeleteCompletion,
    private val timeZoneProvider: AppTimeZoneProvider,
    private val analytics: ProductAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow<JourneyUiState>(JourneyUiState.Loading)
    val uiState: StateFlow<JourneyUiState> = _uiState.asStateFlow()

    private val _events = Channel<JourneyEvent>(Channel.BUFFERED)
    val events: Flow<JourneyEvent> = _events.receiveAsFlow()

    private var hasStarted = false
    private var lastRequestedVoiceId: MemoryMediaId? = null
    private var journeyCollectionJob: Job? = null

    init {
        viewModelScope.launch {
            voicePlaybackController.observeState().collect { state -> onPlaybackState(state) }
        }
    }

    fun onAction(action: JourneyAction) {
        when (action) {
            JourneyAction.ScreenStarted -> onScreenStarted()
            JourneyAction.RetryClicked -> observeJourney()
            JourneyAction.GoToTodayClicked -> viewModelScope.launch { _events.send(JourneyEvent.NavigateToToday) }
            is JourneyAction.PlayVoiceClicked -> onPlayVoiceClicked(action.mediaId, action.reference)
            JourneyAction.PauseVoiceClicked -> viewModelScope.launch { voicePlaybackController.pause() }
            JourneyAction.StopVoiceClicked -> viewModelScope.launch { voicePlaybackController.stop() }
            JourneyAction.TransientErrorDismissed -> onTransientErrorDismissed()
            is JourneyAction.DeleteClicked -> onDeleteClicked(action.completionId)
            JourneyAction.ConfirmDeleteClicked -> onConfirmDeleteClicked()
            JourneyAction.DismissDeleteDialog -> onDismissDeleteDialog()
        }
    }

    private fun onScreenStarted() {
        if (hasStarted) return
        hasStarted = true
        analytics.screen(AnalyticsScreen.JOURNEY)
        observeJourney()
    }

    private fun observeJourney() {
        journeyCollectionJob?.cancel()
        _uiState.value = JourneyUiState.Loading
        journeyCollectionJob = viewModelScope.launch {
            journeyRepository.observeJourney().collect { result ->
                when (result) {
                    is DataResult.Error -> _uiState.value = JourneyUiState.Error(message = result.error.toUiText(), canRetry = true)
                    is DataResult.Success -> applyEntries(result.value)
                }
            }
        }
    }

    private fun applyEntries(entries: List<JourneyEntry>) {
        if (entries.isEmpty()) {
            _uiState.value = JourneyUiState.Empty
            return
        }

        val timeZone = timeZoneProvider.current()
        val stars = constellationPolicy.arrange(entries).map { it.toUi() }.toPersistentList()
        val entriesUi = entries
            .sortedByDescending { it.completedAt }
            .map { it.toUi(timeZone) }
            .toPersistentList()
        val summary = deriveJourneySummary(entries, timeZone).toUi()

        _uiState.value = JourneyUiState.Content(
            summary = summary,
            stars = stars,
            entries = entriesUi,
            playingVoiceId = lastRequestedVoiceId,
        )
    }

    private fun onPlayVoiceClicked(mediaId: MemoryMediaId, reference: MediaReference) {
        lastRequestedVoiceId = mediaId
        viewModelScope.launch {
            val result = voicePlaybackController.play(reference)
            if (result is DataResult.Error) {
                lastRequestedVoiceId = null
                _uiState.update { current ->
                    if (current is JourneyUiState.Content) {
                        current.copy(playingVoiceId = null, transientError = result.error.toUiText())
                    } else {
                        current
                    }
                }
            } else {
                analytics.capture(MemoryOpened)
            }
        }
    }

    private fun onPlaybackState(state: VoicePlaybackState) {
        val activeId = if (state is VoicePlaybackState.Playing) lastRequestedVoiceId else null
        _uiState.update { current -> if (current is JourneyUiState.Content) current.copy(playingVoiceId = activeId) else current }
    }

    private fun onTransientErrorDismissed() {
        _uiState.update { current -> if (current is JourneyUiState.Content) current.copy(transientError = null) else current }
    }

    private fun onDeleteClicked(completionId: CompletionId) {
        _uiState.update { current -> if (current is JourneyUiState.Content) current.copy(pendingDeleteCompletionId = completionId) else current }
    }

    private fun onDismissDeleteDialog() {
        _uiState.update { current -> if (current is JourneyUiState.Content) current.copy(pendingDeleteCompletionId = null) else current }
    }

    /**
     * No explicit reload on success — [observeJourney]'s live collection already reacts to the
     * underlying Room write and rebuilds [stars]/[summary]/[entries] together via [applyEntries],
     * the same way it reacts to a completion added from Today. That rebuild also resets
     * [JourneyUiState.Content.isDeleting]/[JourneyUiState.Content.pendingDeleteCompletionId] back to
     * their defaults, since [applyEntries] always constructs a fresh [JourneyUiState.Content].
     */
    private fun onConfirmDeleteClicked() {
        val current = _uiState.value
        if (current !is JourneyUiState.Content) return
        val completionId = current.pendingDeleteCompletionId ?: return

        _uiState.update { if (it is JourneyUiState.Content) it.copy(isDeleting = true) else it }
        viewModelScope.launch {
            when (val result = deleteCompletion(completionId)) {
                is DataResult.Success -> analytics.capture(MemoryDeleted)
                is DataResult.Error -> _uiState.update {
                    if (it is JourneyUiState.Content) {
                        it.copy(isDeleting = false, pendingDeleteCompletionId = null, transientError = result.error.toUiText())
                    } else {
                        it
                    }
                }
            }
        }
    }
}
