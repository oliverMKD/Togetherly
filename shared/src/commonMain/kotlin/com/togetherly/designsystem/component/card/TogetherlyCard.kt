package com.togetherly.designsystem.component.card

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyShapes
import com.togetherly.designsystem.theme.togetherlySize

/**
 * A plain content container with Togetherly's surface color and corner radius — the base building
 * block for any card-shaped grouping that isn't a selectable choice (see
 * [TogetherlySelectableCard] for that case). Owns no feature state: [content] is fully supplied by
 * the caller.
 *
 * When [onClick] is non-null, the whole card becomes a single tappable target with a
 * [MaterialTheme.togetherlySize.minimumTouchTarget]-tall floor; when null, it's a static grouping
 * with no click semantics at all.
 */
@Composable
fun TogetherlyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.togetherlyColors.backgroundSurface,
        contentColor = MaterialTheme.togetherlyColors.foregroundPrimary,
    )
    val shape = MaterialTheme.togetherlyShapes.card

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.heightIn(min = MaterialTheme.togetherlySize.minimumTouchTarget),
            shape = shape,
            colors = colors,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            content = content,
        )
    }
}
