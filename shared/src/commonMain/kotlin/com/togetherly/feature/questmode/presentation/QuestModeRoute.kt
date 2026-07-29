package com.togetherly.feature.questmode.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.core.feedback.KeepScreenOnEffect
import com.togetherly.core.feedback.QuestFeedbackController
import com.togetherly.domain.completion.CompletionId
import com.togetherly.feature.questmode.model.QuestTimerUi
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * [QuestModeRoute] owns state/event collection; [QuestModeScreen] is stateless. Platform feedback
 * and keep-screen-on both resolve at this exact boundary — never inside [QuestModeViewModel]
 * (which stays platform-independent) and never inside [QuestModeScreen] (which stays a plain,
 * reusable Composable with no Koin lookups of its own).
 *
 * [QuestFeedbackController.timerFinished] fires once per [QuestModeEvent.TimerFinished] (the
 * ViewModel's own guarantee — see that event's KDoc); [QuestFeedbackController.questCompleted]
 * fires on [QuestModeEvent.NavigateToCompletion], before actually navigating, but without waiting
 * on it — a slow or failed haptic must never delay leaving Quest Mode.
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

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                QuestModeEvent.NavigateBack -> onNavigateBack()
                QuestModeEvent.NavigateToToday -> onNavigateToToday()
                is QuestModeEvent.NavigateToCompletion -> {
                    feedbackController.questCompleted()
                    onNavigateToCompletion(event.completionId)
                }
                QuestModeEvent.TimerFinished -> {
                    feedbackController.timerFinished()
                    onTimerFinished()
                }
            }
        }
    }

    KeepScreenOnEffect(enabled = shouldKeepScreenOn(state))

    QuestModeScreen(state = state, onAction = viewModel::onAction)
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
