package com.togetherly.feature.explore.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import com.togetherly.feature.explore.model.ExploreUiState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.explore_all_packs_title
import togetherly.shared.generated.resources.explore_empty_filter_body
import togetherly.shared.generated.resources.explore_empty_filter_title
import togetherly.shared.generated.resources.explore_empty_search_body
import togetherly.shared.generated.resources.explore_empty_search_title
import togetherly.shared.generated.resources.explore_empty_state_content_description
import togetherly.shared.generated.resources.explore_featured_packs_title
import togetherly.shared.generated.resources.explore_filters_action
import togetherly.shared.generated.resources.explore_filters_action_with_count
import togetherly.shared.generated.resources.explore_saved_action
import togetherly.shared.generated.resources.explore_search_results_title
import togetherly.shared.generated.resources.explore_subtitle
import togetherly.shared.generated.resources.explore_suggested_quests_title
import togetherly.shared.generated.resources.explore_title
import togetherly.shared.generated.resources.nav_destination_explore

/** Lets tests scroll the catalogue's own list unambiguously — the screen also has a search field's internal scroll and the featured-packs [LazyRow], both of which also expose a scroll action. */
internal const val ExploreContentListTestTag = "explore-content-list"

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
        scrollable = false,
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
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(ExploreContentListTestTag),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = MaterialTheme.togetherlySpacing.m),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
    ) {
        item(key = "intro", contentType = "header") {
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
        }

        item(key = "search", contentType = "controls") {
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
        }

        item(key = "categories", contentType = "controls") {
            ExploreCategorySelector(
                selected = state.selectedCategory,
                onCategorySelected = { onAction(ExploreAction.CategorySelected(it)) },
                onCategoryCleared = { onAction(ExploreAction.CategoryCleared) },
            )
        }

        when {
            state.error != null -> item(key = "error", contentType = "status") {
                TogetherlyInlineError(
                    message = state.error.asString(),
                    onRetry = { onAction(ExploreAction.RetryClicked) },
                )
            }
            state.isLoading -> item(key = "loading", contentType = "status") {
                Box(Modifier.fillMaxWidth().padding(vertical = MaterialTheme.togetherlySpacing.xl), contentAlignment = Alignment.Center) {
                    TogetherlyLoadingIndicator()
                }
            }
            state.emptyState != null -> item(key = "empty", contentType = "status") {
                ExploreEmptyStateMessage(state.emptyState)
            }
            state.isSearchActive -> {
                item(key = "search-results-title", contentType = "section-title") {
                    ExploreSectionTitle(stringResource(Res.string.explore_search_results_title))
                }
                items(state.packs, key = { "search-pack-${it.id.value}" }, contentType = { "pack" }) { pack ->
                    ExplorePackCard(pack = pack, onClick = { onAction(ExploreAction.PackClicked(pack.id)) })
                }
                items(state.quests, key = { "search-quest-${it.id.value}" }, contentType = { "quest" }) { quest ->
                    ExploreQuestCard(
                        quest = quest,
                        onClick = { onAction(ExploreAction.QuestClicked(quest.id)) },
                        onSaveClick = { onAction(ExploreAction.SaveClicked(quest.id)) },
                    )
                }
            }
            else -> {
                if (state.featuredPacks.isNotEmpty()) {
                    item(key = "featured-packs", contentType = "featured-section") {
                        ExploreSection(title = stringResource(Res.string.explore_featured_packs_title)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
                                items(state.featuredPacks, key = { "featured-${it.id.value}" }) { pack ->
                                    ExplorePackCard(
                                        pack = pack,
                                        onClick = { onAction(ExploreAction.PackClicked(pack.id)) },
                                        featured = true,
                                    )
                                }
                            }
                        }
                    }
                }
                if (state.packs.isNotEmpty()) {
                    item(key = "all-packs-title", contentType = "section-title") {
                        ExploreSectionTitle(stringResource(Res.string.explore_all_packs_title))
                    }
                    items(state.packs, key = { "pack-${it.id.value}" }, contentType = { "pack" }) { pack ->
                        ExplorePackCard(pack = pack, onClick = { onAction(ExploreAction.PackClicked(pack.id)) })
                    }
                }
                if (state.quests.isNotEmpty()) {
                    item(key = "suggested-quests-title", contentType = "section-title") {
                        ExploreSectionTitle(stringResource(Res.string.explore_suggested_quests_title))
                    }
                    items(state.quests, key = { "quest-${it.id.value}" }, contentType = { "quest" }) { quest ->
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
private fun ExploreSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.togetherlyTypography.titleL,
        color = MaterialTheme.togetherlyColors.foregroundPrimary,
    )
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
    val label = if (activeCount > 0) {
        stringResource(Res.string.explore_filters_action_with_count, activeCount)
    } else {
        stringResource(Res.string.explore_filters_action)
    }
    TogetherlySecondaryButton(label = label, onClick = onClick, modifier = modifier)
}

@Composable
private fun ExploreEmptyStateMessage(state: ExploreEmptyState, modifier: Modifier = Modifier) {
    val title = stringResource(if (state == ExploreEmptyState.SEARCH) Res.string.explore_empty_search_title else Res.string.explore_empty_filter_title)
    val body = stringResource(if (state == ExploreEmptyState.SEARCH) Res.string.explore_empty_search_body else Res.string.explore_empty_filter_body)
    val contentDescription = stringResource(Res.string.explore_empty_state_content_description, title, body)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.togetherlySpacing.xl)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
    ) {
        Text(text = title, style = MaterialTheme.togetherlyTypography.titleM, color = MaterialTheme.togetherlyColors.foregroundPrimary)
        Text(text = body, style = MaterialTheme.togetherlyTypography.bodyM, color = MaterialTheme.togetherlyColors.foregroundSecondary)
    }
}
