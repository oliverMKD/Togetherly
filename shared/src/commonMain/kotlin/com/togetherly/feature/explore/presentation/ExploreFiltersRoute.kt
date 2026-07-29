package com.togetherly.feature.explore.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.button.TogetherlySecondaryButton
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.component.selection.TogetherlyChipFlowRow
import com.togetherly.designsystem.component.selection.TogetherlyChoiceChip
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.explore.QuestAccessFilter
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestLocation
import com.togetherly.feature.explore.mapper.label as accessFilterLabel
import com.togetherly.feature.explore.model.ExploreFiltersUiState
import com.togetherly.feature.onboarding.presentation.label as ageBandLabel
import com.togetherly.feature.today.mapper.label
import com.togetherly.feature.today.mapper.toUi
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.explore_filter_group_access
import togetherly.shared.generated.resources.explore_filter_group_age
import togetherly.shared.generated.resources.explore_filter_group_category
import togetherly.shared.generated.resources.explore_filter_group_duration
import togetherly.shared.generated.resources.explore_filter_group_energy
import togetherly.shared.generated.resources.explore_filter_group_location
import togetherly.shared.generated.resources.explore_filters_apply
import togetherly.shared.generated.resources.explore_filters_cancel
import togetherly.shared.generated.resources.explore_filters_clear_all
import togetherly.shared.generated.resources.explore_filters_title

/**
 * A full-screen destination, not a [androidx.compose.material3.ModalBottomSheet] — unlike Today's
 * own filter sheet (which is in-VM-state, layered over Today's already-composed screen),
 * [com.togetherly.navigation.destination.RootDestination.ExploreFilters] was already established
 * (Step 12.1) as a real push destination on the *root* nav graph, not a piece of Explore's own
 * screen state; a full-screen destination fits that shape without fighting it (this feature's own
 * task spec permits either: "use a sheet only if... otherwise use a full-screen filter destination").
 */
@Composable
fun ExploreFiltersRoute(
    onNavigateBack: () -> Unit,
    viewModel: ExploreFiltersViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ExploreFiltersEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    ExploreFiltersScreen(state = state, onAction = viewModel::onAction, onNavigateBack = onNavigateBack)
}

@Composable
internal fun ExploreFiltersScreen(
    state: ExploreFiltersUiState,
    onAction: (ExploreFiltersAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                title = stringResource(Res.string.explore_filters_title),
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = stringResource(Res.string.explore_filters_cancel),
                        onClick = { onAction(ExploreFiltersAction.CancelClicked) },
                    )
                },
            )
        },
    ) {
        val draft = state.draft

        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.togetherlySpacing.m),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
        ) {
            NullableFilterChipGroup(
                title = stringResource(Res.string.explore_filter_group_duration),
                options = DurationBand.entries,
                selected = draft.duration,
                label = { it.label().asString() },
                onSelect = { onAction(ExploreFiltersAction.DurationChanged(it)) },
            )
            NullableFilterChipGroup(
                title = stringResource(Res.string.explore_filter_group_energy),
                options = EnergyLevel.entries,
                selected = draft.energy,
                label = { it.label().asString() },
                onSelect = { onAction(ExploreFiltersAction.EnergyChanged(it)) },
            )
            NullableFilterChipGroup(
                title = stringResource(Res.string.explore_filter_group_location),
                options = listOf(QuestLocation.INDOOR, QuestLocation.OUTDOOR),
                selected = draft.location,
                label = { it.label().asString() },
                onSelect = { onAction(ExploreFiltersAction.LocationChanged(it)) },
            )
            NullableFilterChipGroup(
                title = stringResource(Res.string.explore_filter_group_age),
                options = AgeBand.entries,
                selected = draft.ageBand,
                label = { it.ageBandLabel() },
                onSelect = { onAction(ExploreFiltersAction.AgeBandChanged(it)) },
            )
            NullableFilterChipGroup(
                title = stringResource(Res.string.explore_filter_group_category),
                options = QuestCategory.entries,
                selected = draft.category,
                label = { it.toUi().label().asString() },
                onSelect = { onAction(ExploreFiltersAction.CategoryChanged(it)) },
            )
            AccessFilterChipGroup(
                title = stringResource(Res.string.explore_filter_group_access),
                selected = draft.access,
                onSelect = { onAction(ExploreFiltersAction.AccessChanged(it)) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s),
            ) {
                TogetherlySecondaryButton(
                    label = stringResource(Res.string.explore_filters_clear_all),
                    onClick = { onAction(ExploreFiltersAction.ClearAllClicked) },
                    modifier = Modifier.weight(1f),
                )
                TogetherlyPrimaryButton(
                    label = stringResource(Res.string.explore_filters_apply),
                    onClick = { onAction(ExploreFiltersAction.ApplyClicked) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun <T> NullableFilterChipGroup(
    title: String,
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChipGroupShell(title = title, modifier = modifier) {
        options.forEach { option ->
            TogetherlyChoiceChip(
                label = label(option),
                selected = option == selected,
                onClick = { onSelect(if (option == selected) null else option) },
            )
        }
    }
}

@Composable
private fun AccessFilterChipGroup(
    title: String,
    selected: QuestAccessFilter,
    onSelect: (QuestAccessFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChipGroupShell(title = title, modifier = modifier) {
        QuestAccessFilter.entries.forEach { option ->
            TogetherlyChoiceChip(
                label = option.accessFilterLabel().asString(),
                selected = option == selected,
                onClick = { onSelect(option) },
            )
        }
    }
}

@Composable
private fun FilterChipGroupShell(title: String, modifier: Modifier = Modifier, chips: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.togetherlyTypography.labelL,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
        )
        TogetherlyChipFlowRow { chips() }
    }
}
