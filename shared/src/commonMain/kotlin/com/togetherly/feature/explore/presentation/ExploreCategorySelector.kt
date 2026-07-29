package com.togetherly.feature.explore.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.selection.TogetherlyChoiceChip
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.feature.today.mapper.label
import com.togetherly.feature.today.model.QuestCategoryUi
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.explore_category_all

/**
 * A horizontally scrolling row, not a wrapping [com.togetherly.designsystem.component.selection.TogetherlyChipFlowRow] —
 * eight chips (All + seven categories) at a comfortable label width don't need to wrap, and a
 * single scrollable row is what "Horizontal rows remain navigable" (this feature's own
 * accessibility requirement) is describing. No manual `contentDescription`/announcement wiring here:
 * [TogetherlyChoiceChip] already sets Material's own `selected` semantics (see that component's own
 * KDoc), which a screen reader announces on its own.
 */
@Composable
internal fun ExploreCategorySelector(
    selected: QuestCategoryUi?,
    onCategorySelected: (QuestCategoryUi) -> Unit,
    onCategoryCleared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
    ) {
        item {
            TogetherlyChoiceChip(
                label = stringResource(Res.string.explore_category_all),
                selected = selected == null,
                onClick = onCategoryCleared,
            )
        }
        items(QuestCategoryUi.entries) { category ->
            TogetherlyChoiceChip(
                label = category.label().asString(),
                selected = category == selected,
                onClick = { onCategorySelected(category) },
            )
        }
    }
}
