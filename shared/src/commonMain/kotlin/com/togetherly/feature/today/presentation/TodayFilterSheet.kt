package com.togetherly.feature.today.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.button.TogetherlySecondaryButton
import com.togetherly.designsystem.component.selection.TogetherlyChipFlowRow
import com.togetherly.designsystem.component.selection.TogetherlyChoiceChip
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.PreparationLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestLocation
import com.togetherly.feature.today.mapper.label
import com.togetherly.feature.today.mapper.toUi
import com.togetherly.feature.today.model.TodayFilterState
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.today_filter_group_category
import togetherly.shared.generated.resources.today_filter_group_energy
import togetherly.shared.generated.resources.today_filter_group_location
import togetherly.shared.generated.resources.today_filter_group_preparation
import togetherly.shared.generated.resources.today_filter_group_time
import togetherly.shared.generated.resources.today_filters_apply
import togetherly.shared.generated.resources.today_filters_clear_all
import togetherly.shared.generated.resources.today_filters_title

/**
 * A shared Material3 [ModalBottomSheet] — this Compose Multiplatform version (1.11.1) ships it as
 * a stable cross-platform component (Android and iOS both render the real sheet, not a fallback),
 * so a separate full-screen filter destination isn't needed; see the task's own instruction to
 * document this choice.
 *
 * Each group is a single-select [TogetherlyChoiceChip] row: tapping the already-selected chip
 * clears that group back to "no explicit preference" (matches [TodayFilterState]'s own `null`
 * meaning), tapping a different chip replaces the selection. No group is required.
 *
 * Dismissing this sheet (swipe-down, scrim tap, or system back) calls [onDismiss] — wired by
 * [TodayScreen] to [TodayAction.FiltersDismissed], which restores the pre-open draft (see that
 * action's own KDoc on [TodayViewModel.onFiltersDismissed]) — so closing without an explicit Clear
 * all/Find a quest tap always discards whatever was changed since the sheet opened.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayFilterSheet(
    filters: TodayFilterState,
    isApplying: Boolean,
    onAction: (TodayAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.togetherlySpacing.m, vertical = MaterialTheme.togetherlySpacing.s),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
        ) {
            Text(
                text = stringResource(Res.string.today_filters_title),
                style = MaterialTheme.togetherlyTypography.titleL,
                color = MaterialTheme.togetherlyColors.foregroundPrimary,
                modifier = Modifier.semantics { heading() },
            )

            FilterChipGroup(
                title = stringResource(Res.string.today_filter_group_time),
                options = DurationBand.entries,
                selected = filters.duration,
                label = { it.label().asString() },
                onSelect = { onAction(TodayAction.DurationFilterChanged(it)) },
            )
            FilterChipGroup(
                title = stringResource(Res.string.today_filter_group_location),
                options = listOf(QuestLocation.INDOOR, QuestLocation.OUTDOOR),
                selected = filters.location,
                label = { it.label().asString() },
                onSelect = { onAction(TodayAction.LocationFilterChanged(it)) },
            )
            FilterChipGroup(
                title = stringResource(Res.string.today_filter_group_energy),
                options = EnergyLevel.entries,
                selected = filters.energy,
                label = { it.label().asString() },
                onSelect = { onAction(TodayAction.EnergyFilterChanged(it)) },
            )
            FilterChipGroup(
                title = stringResource(Res.string.today_filter_group_preparation),
                options = PreparationLevel.entries,
                selected = filters.preparation,
                label = { it.label().asString() },
                onSelect = { onAction(TodayAction.PreparationFilterChanged(it)) },
            )
            FilterChipGroup(
                title = stringResource(Res.string.today_filter_group_category),
                options = QuestCategory.entries,
                selected = filters.category,
                label = { it.toUi().label().asString() },
                onSelect = { onAction(TodayAction.CategoryFilterChanged(it)) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s),
            ) {
                TogetherlySecondaryButton(
                    label = stringResource(Res.string.today_filters_clear_all),
                    onClick = { onAction(TodayAction.ClearFiltersClicked) },
                    modifier = Modifier.weight(1f),
                )
                TogetherlyPrimaryButton(
                    label = stringResource(Res.string.today_filters_apply),
                    loading = isApplying,
                    onClick = { onAction(TodayAction.ApplyFiltersClicked) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun <T> FilterChipGroup(
    title: String,
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.togetherlyTypography.labelL,
            color = MaterialTheme.togetherlyColors.foregroundSecondary,
            modifier = Modifier.semantics { heading() },
        )
        TogetherlyChipFlowRow {
            options.forEach { option ->
                TogetherlyChoiceChip(
                    label = label(option),
                    selected = option == selected,
                    onClick = { onSelect(if (option == selected) null else option) },
                )
            }
        }
    }
}
