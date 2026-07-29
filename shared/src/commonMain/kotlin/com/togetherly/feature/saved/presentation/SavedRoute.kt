package com.togetherly.feature.saved.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.quest.QuestId
import com.togetherly.feature.explore.presentation.ExploreQuestCard
import com.togetherly.feature.saved.model.SavedUiState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.explore_saved_empty_body
import togetherly.shared.generated.resources.explore_saved_empty_title
import togetherly.shared.generated.resources.explore_saved_title

@Composable
fun SavedRoute(
    onOpenQuestDetail: (QuestId) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SavedViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.onScreenStarted() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SavedEvent.OpenQuestDetail -> onOpenQuestDetail(event.questId)
                SavedEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    SavedScreen(state = state, onAction = viewModel::onAction, onNavigateBack = onNavigateBack)
}

@Composable
internal fun SavedScreen(
    state: SavedUiState,
    onAction: (SavedAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                title = stringResource(Res.string.explore_saved_title),
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = "Back",
                        onClick = onNavigateBack,
                    )
                },
            )
        },
    ) {
        when {
            state.error != null -> TogetherlyInlineError(
                message = state.error.asString(),
                onRetry = { onAction(SavedAction.RetryClicked) },
            )
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TogetherlyLoadingIndicator()
            }
            state.quests.isEmpty() -> SavedEmptyState()
            else -> Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.togetherlySpacing.m),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s),
            ) {
                state.quests.forEach { quest ->
                    ExploreQuestCard(
                        quest = quest,
                        onClick = { onAction(SavedAction.QuestClicked(quest.id)) },
                        onSaveClick = { onAction(SavedAction.SaveClicked(quest.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.togetherlySpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.explore_saved_empty_title),
            style = MaterialTheme.togetherlyTypography.titleM,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
        Text(
            text = stringResource(Res.string.explore_saved_empty_body),
            style = MaterialTheme.togetherlyTypography.bodyM,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
    }
}
