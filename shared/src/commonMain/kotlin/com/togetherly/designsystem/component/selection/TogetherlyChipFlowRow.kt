package com.togetherly.designsystem.component.selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.designsystem.theme.togetherlySpacing

/**
 * Wraps [FlowRow] (stable in this project's Compose Foundation version — no experimental opt-in,
 * no third-party flow-layout dependency needed) with Togetherly's token-based gap between chips,
 * so every chip group in the app wraps with the same spacing instead of each screen picking its
 * own [Arrangement.spacedBy] value.
 */
@Composable
fun TogetherlyChipFlowRow(
    modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit,
) {
    val gap = MaterialTheme.togetherlySpacing.xs
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}
