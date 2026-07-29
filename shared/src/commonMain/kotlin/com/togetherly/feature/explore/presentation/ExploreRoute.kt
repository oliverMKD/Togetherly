package com.togetherly.feature.explore.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlySecondaryButton
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.feature.explore.model.ExploreEmptyState
import com.togetherly.feature.explore.model.ExplorePackUiModel
import com.togetherly.feature.explore.model.ExploreQuestUiModel
import com.togetherly.feature.explore.model.ExploreUiState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.explore_all_packs_title
import togetherly.shared.generated.resources.explore_empty_filter_body
import togetherly.shared.generated.resources.explore_empty_filter_title
import togetherly.shared.generated.resources.explore_empty_search_body
import togetherly.shared.generated.resources.explore_empty_search_title
import togetherly.shared.generated.resources.explore_featured_packs_title
import togetherly.shared.generated.resources.explore_filters_action
import togetherly.shared.generated.resources.explore_saved_action
import togetherly.shared.generated.resources.explore_search_results_title
import togetherly.shared.generated.resources.explore_subtitle
import togetherly.shared.generated.resources.explore_suggested_quests_title
import togetherly.shared.generated.resources.explore_title
import togetherly.shared.generated.resources.nav_destination_explore

@Composable
fun ExploreRoute(
    onOpenQuestDetail: (QuestId) -> Unit,
    onOpenPackDetails: (QuestPackId) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenSaved: () -> Unit,
    viewModel: ExploreViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.onScreenStarted() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ExploreEvent.OpenQuestDetail -> onOpenQuestDetail(event.questId)
                is ExploreEvent.OpenPackDetails -> onOpenPackDetails(event.packId)
                ExploreEvent.OpenFilters -> onOpenFilters()
                ExploreEvent.OpenSaved -> onOpenSaved()
            }
        }
    }

    ExploreScreen(state = state, onAction = viewModel::onAction)
}

@Composable
internal fun ExploreScreen(
    state: ExploreUiState,
    onAction: (ExploreAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                title = stringResource(Res.string.nav_destination_explore),
                actions = {
                    TogetherlyTextButton(
                        label = stringResource(Res.string.explore_saved_action),
                        onClick = { onAction(ExploreAction.SavedClicked) },
                    )
                },
            )
        },
    ) {
        ExploreContent(state = state, onAction = onAction)
    }
}

@Composable
private fun ExploreContent(
    state: ExploreUiState,
    onAction: (ExploreAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = MaterialTheme.togetherlySpacing.m),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xxs)) {
            Text(
                text = stringResource(Res.string.explore_title),
                style = MaterialTheme.togetherlyTypography.headlineM,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
            )
            Text(
                text = stringResource(Res.string.explore_subtitle),
                style = MaterialTheme.togetherlyTypography.bodyM,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExploreSearchField(
                query = state.searchQuery,
                onQueryChange = { onAction(ExploreAction.SearchChanged(it)) },
                onClear = { onAction(ExploreAction.SearchCleared) },
                modifier = Modifier.weight(1f),
            )
            FiltersButton(activeCount = state.activeFilterCount, onClick = { onAction(ExploreAction.FiltersClicked) })
        }

        ExploreCategorySelector(
            selected = state.selectedCategory,
            onCategorySelected = { onAction(ExploreAction.CategorySelected(it)) },
            onCategoryCleared = { onAction(ExploreAction.CategoryCleared) },
        )

        when {
            state.error != null -> TogetherlyInlineError(
                message = state.error.asString(),
                onRetry = { onAction(ExploreAction.RetryClicked) },
            )
            state.isLoading -> Box(Modifier.fillMaxWidth().padding(vertical = MaterialTheme.togetherlySpacing.xl), contentAlignment = Alignment.Center) {
                TogetherlyLoadingIndicator()
            }
            state.emptyState != null -> ExploreEmptyStateMessage(state.emptyState)
            state.isSearchActive -> SearchResultsSection(packs = state.packs, quests = state.quests, onAction = onAction)
            else -> ExploreCatalogueSections(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun ExploreCatalogueSections(state: ExploreUiState, onAction: (ExploreAction) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l)) {
        if (state.featuredPacks.isNotEmpty()) {
            ExploreSection(title = stringResource(Res.string.explore_featured_packs_title)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
                    items(state.featuredPacks, key = { it.id.value }) { pack ->
                        ExplorePackCard(pack = pack, onClick = { onAction(ExploreAction.PackClicked(pack.id)) }, featured = true)
                    }
                }
            }
        }

        if (state.packs.isNotEmpty()) {
            ExploreSection(title = stringResource(Res.string.explore_all_packs_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
                    state.packs.forEach { pack ->
                        ExplorePackCard(pack = pack, onClick = { onAction(ExploreAction.PackClicked(pack.id)) })
                    }
                }
            }
        }

        if (state.quests.isNotEmpty()) {
            ExploreSection(title = stringResource(Res.string.explore_suggested_quests_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
                    state.quests.forEach { quest ->
                        ExploreQuestCard(
                            quest = quest,
                            onClick = { onAction(ExploreAction.QuestClicked(quest.id)) },
                            onSaveClick = { onAction(ExploreAction.SaveClicked(quest.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultsSection(
    packs: List<ExplorePackUiModel>,
    quests: List<ExploreQuestUiModel>,
    onAction: (ExploreAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l)) {
        ExploreSection(title = stringResource(Res.string.explore_search_results_title)) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
                packs.forEach { pack ->
                    ExplorePackCard(pack = pack, onClick = { onAction(ExploreAction.PackClicked(pack.id)) })
                }
                quests.forEach { quest ->
                    ExploreQuestCard(
                        quest = quest,
                        onClick = { onAction(ExploreAction.QuestClicked(quest.id)) },
                        onSaveClick = { onAction(ExploreAction.SaveClicked(quest.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
        Text(
            text = title,
            style = MaterialTheme.togetherlyTypography.titleL,
            color = MaterialTheme.togetherlyColors.foregroundPrimary,
        )
        content()
    }
}

@Composable
private fun FiltersButton(activeCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val baseLabel = stringResource(Res.string.explore_filters_action)
    val label = if (activeCount > 0) "$baseLabel ($activeCount)" else baseLabel
    TogetherlySecondaryButton(label = label, onClick = onClick, modifier = modifier)
}

@Composable
private fun ExploreEmptyStateMessage(state: ExploreEmptyState, modifier: Modifier = Modifier) {
    val title = stringResource(if (state == ExploreEmptyState.SEARCH) Res.string.explore_empty_search_title else Res.string.explore_empty_filter_title)
    val body = stringResource(if (state == ExploreEmptyState.SEARCH) Res.string.explore_empty_search_body else Res.string.explore_empty_filter_body)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.togetherlySpacing.xl)
            .semantics(mergeDescendants = true) { contentDescription = "$title. $body" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
    ) {
        Text(text = title, style = MaterialTheme.togetherlyTypography.titleM, color = MaterialTheme.togetherlyColors.foregroundPrimary)
        Text(text = body, style = MaterialTheme.togetherlyTypography.bodyM, color = MaterialTheme.togetherlyColors.foregroundSecondary)
    }
}
