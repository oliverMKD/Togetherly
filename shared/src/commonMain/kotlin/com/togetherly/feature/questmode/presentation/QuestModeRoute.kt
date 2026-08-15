package com.togetherly.feature.questmode.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.core.feedback.KeepScreenOnEffect
import com.togetherly.core.feedback.QuestFeedbackController
import com.togetherly.domain.completion.CompletionId
import com.togetherly.feature.questmode.model.QuestTimerUi
import kotlinx.coroutines.flow.Flow
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * [QuestModeRoute] owns state/event collection; [QuestModeScreen] is stateless. Platform feedback
 * and keep-screen-on both resolve at this exact boundary — never inside [QuestModeViewModel]
 * (which stays platform-independent) and never inside [QuestModeScreen] (which stays a plain,
 * reusable Composable with no Koin lookups of its own).
 *
 * [QuestFeedbackController.timerFinished] fires once per [QuestModeEvent.TimerFinished] and
 * [QuestFeedbackController.questCompleted] once per [QuestModeEvent.NavigateToCompletion] — guarded
 * twice, at two different scopes, deliberately: the ViewModel's own `timerFinishedEmitted` flag
 * (see that event's KDoc) dedups within one ViewModel instance's lifetime, while
 * [QuestModeRouteEffects]'s own `timerFinishedHandled`/`completionNavigationHandled`
 * (`rememberSaveable`, keyed on [completionId]) additionally survive ViewModel recreation — a
 * restored process re-running this same effect must never replay a terminal event a second time
 * (see [QuestModeRouteEffectsTest] for the process-death scenario this specifically guards against).
 * [QuestFeedbackController.questCompleted] fires before actually navigating, but without waiting on
 * it — a slow or failed haptic must never delay leaving Quest Mode.
 *
 * [KeepScreenOnEffect] is enabled only while [QuestModeUiState.Content]'s quest requests it
 * ([com.togetherly.feature.questmode.model.QuestModeContentUi.keepScreenOnRequested]), the timer is
 * actually still running, and phone-down mode is off — every other state (loading, error, an
 * untimed quest, a finished timer, phone-down) disables it.
 */
@Composable
fun QuestModeRoute(
    completionId: CompletionId,
    onNavigateBack: () -> Unit,
    onNavigateToToday: () -> Unit,
    onNavigateToCompletion: (CompletionId) -> Unit,
    onTimerFinished: () -> Unit,
    viewModel: QuestModeViewModel = koinViewModel(key = completionId.value) { parametersOf(completionId) },
    feedbackController: QuestFeedbackController = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.onAction(QuestModeAction.ScreenStarted)
    }

    QuestModeRouteEffects(
        completionId = completionId,
        events = viewModel.events,
        feedbackController = feedbackController,
        onNavigateBack = onNavigateBack,
        onNavigateToToday = onNavigateToToday,
        onNavigateToCompletion = onNavigateToCompletion,
        onTimerFinished = onTimerFinished,
    )

    KeepScreenOnEffect(enabled = shouldKeepScreenOn(state))

    QuestModeScreen(state = state, onAction = viewModel::onAction)
}

@Composable
internal fun QuestModeRouteEffects(
    completionId: CompletionId,
    events: Flow<QuestModeEvent>,
    feedbackController: QuestFeedbackController,
    onNavigateBack: () -> Unit,
    onNavigateToToday: () -> Unit,
    onNavigateToCompletion: (CompletionId) -> Unit,
    onTimerFinished: () -> Unit,
) {
    var timerFinishedHandled by rememberSaveable(completionId.value) { mutableStateOf(false) }
    var completionNavigationHandled by rememberSaveable(completionId.value) { mutableStateOf(false) }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                QuestModeEvent.NavigateBack -> onNavigateBack()
                QuestModeEvent.NavigateToToday -> onNavigateToToday()
                is QuestModeEvent.NavigateToCompletion -> if (!completionNavigationHandled) {
                    completionNavigationHandled = true
                    feedbackController.questCompleted()
                    onNavigateToCompletion(event.completionId)
                }
                QuestModeEvent.TimerFinished -> {
                    if (timerFinishedHandled) return@collect
                    timerFinishedHandled = true
                    feedbackController.timerFinished()
                    onTimerFinished()
                }
            }
        }
    }
}

/**
 * A plain, non-Composable function specifically so this decision is unit-testable on its own,
 * without needing a Compose UI test environment — see `QuestModeRouteKeepScreenOnTest` (`commonTest`).
 */
internal fun shouldKeepScreenOn(state: QuestModeUiState): Boolean {
    val content = state as? QuestModeUiState.Content ?: return false
    return !content.phoneDown &&
        content.quest.keepScreenOnRequested &&
        content.quest.timer is QuestTimerUi.Running
}
