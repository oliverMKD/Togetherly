package com.togetherly.designsystem.component.selection

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyTypography

/**
 * A single pick/unpick choice among many (e.g. a topic filter). Built on Material's own
 * [FilterChip] rather than a from-scratch component, since [FilterChip] already provides correct
 * selectable semantics (`selected`, [androidx.compose.ui.semantics.Role.Checkbox]) and the
 * touch/hit-testing behavior a hand-rolled chip would have to reimplement — this wrapper only adds
 * Togetherly's tokens on top, which is the added value that justifies wrapping it at all.
 *
 * The visual chip stays Material's compact height (so a row of chips doesn't look oversized);
 * [minimumInteractiveComponentSize] pads its *touch* target to the 48dp floor without inflating
 * what's drawn.
 */
@Composable
fun TogetherlyChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.togetherlyColors

    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.minimumInteractiveComponentSize(),
        label = { Text(text = label, style = MaterialTheme.togetherlyTypography.labelM) },
        leadingIcon = icon,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.backgroundSurface,
            labelColor = colors.foregroundPrimary,
            iconColor = colors.foregroundSecondary,
            selectedContainerColor = colors.actionPrimary,
            selectedLabelColor = colors.actionPrimaryContent,
            selectedLeadingIconColor = colors.actionPrimaryContent,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = colors.borderSubtle,
            selectedBorderColor = colors.actionPrimary,
            borderWidth = 1.dp,
            selectedBorderWidth = 2.dp,
        ),
    )
}
