package com.togetherly.designsystem.component.button

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySize

/**
 * A compact, icon-only action (e.g. a top-bar action, a dismiss control). [contentDescription] is
 * required rather than defaulted, since an icon-only control has no visible label a screen reader
 * could otherwise announce — the caller always knows what the icon does; this component never
 * would.
 *
 * No [loading] parameter: icon buttons in this system are simple toggles/navigation triggers, not
 * submit actions with a pending network result — see button-family KDoc in
 * [com.togetherly.designsystem.component.button.TogetherlyPrimaryButton] for where loading
 * actually belongs.
 */
@Composable
fun TogetherlyIconButton(
    icon: @Composable () -> Unit,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .sizeIn(
                minWidth = MaterialTheme.togetherlySize.minimumTouchTarget,
                minHeight = MaterialTheme.togetherlySize.minimumTouchTarget,
            )
            .semantics { this.contentDescription = contentDescription },
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.togetherlyColors.foregroundPrimary,
        ),
    ) {
        icon()
    }
}
