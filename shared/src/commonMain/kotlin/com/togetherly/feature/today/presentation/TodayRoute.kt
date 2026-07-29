package com.togetherly.feature.today.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.domain.quest.QuestId
import org.koin.compose.viewmodel.koinViewModel

/**
 * [TodayRoute] owns state/event collection; [TodayScreen] is stateless (see its own KDoc). Filters
 * and reroll UI are not implemented yet (Step 8.5) even though [TodayUiState] already carries their
 * fields — this screen only renders Loading/Mystery/Revealed/Error.
 */
@Composable
fun TodayRoute(
    viewModel: TodayViewModel = koinViewModel(),
    onOpenQuestDetail: (QuestId) -> Unit = {},
    onRerollLimitReached: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.onAction(TodayAction.ScreenStarted)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TodayEvent.OpenQuestDetail -> onOpenQuestDetail(event.questId)
                TodayEvent.RerollLimitReached -> onRerollLimitReached()
            }
        }
    }

    TodayScreen(state = state, onAction = viewModel::onAction)
}
