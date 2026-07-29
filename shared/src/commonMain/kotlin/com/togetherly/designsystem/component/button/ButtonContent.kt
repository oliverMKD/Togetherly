package com.togetherly.designsystem.component.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.togetherly.designsystem.theme.togetherlySize
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography

/**
 * Shared label(+leading slot) row for every label-based Togetherly button (primary/secondary/
 * text) so loading behaviour is identical everywhere it appears instead of being reimplemented
 * per button: while [loading], a small spinner takes the leading-icon slot and the label stays put
 * — the label is never replaced or removed, only ever joined by the spinner.
 */
@Composable
internal fun TogetherlyButtonContent(
    label: String,
    loading: Boolean,
    leadingIcon: (@Composable () -> Unit)?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.size(MaterialTheme.togetherlySize.iconM),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
            leadingIcon != null -> leadingIcon()
        }
        Text(text = label, style = MaterialTheme.togetherlyTypography.labelL)
    }
}
