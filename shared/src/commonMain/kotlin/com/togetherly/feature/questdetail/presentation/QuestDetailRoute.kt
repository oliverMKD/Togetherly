package com.togetherly.feature.questdetail.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.quest.QuestId
import com.togetherly.feature.questdetail.model.QuestOpenSource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * [QuestDetailViewModel] is parameterized by [questId] and [source] (Koin's `parametersOf`, not a
 * `SavedStateHandle` read) — a distinct instance per distinct
 * [com.togetherly.navigation.destination.RootDestination.QuestDetail]. [onNavigateBack]/
 * [onStartedQuestMode] are the only ways this reaches outside itself — never a direct
 * `NavController` reference inside the ViewModel or [QuestDetailScreen].
 */
@Composable
fun QuestDetailRoute(
    questId: QuestId,
    source: QuestOpenSource,
    onNavigateBack: () -> Unit,
    onStartedQuestMode: (CompletionId) -> Unit,
    onOpenPaywall: (QuestId) -> Unit = {},
    viewModel: QuestDetailViewModel = koinViewModel(key = questId.value) { parametersOf(questId, source) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.onScreenStarted()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                QuestDetailEvent.NavigateBack -> onNavigateBack()
                is QuestDetailEvent.NavigatedToQuestMode -> onStartedQuestMode(event.completionId)
                is QuestDetailEvent.OpenPaywall -> onOpenPaywall(event.questId)
            }
        }
    }

    QuestDetailScreen(state = state, onAction = viewModel::onAction)
}
