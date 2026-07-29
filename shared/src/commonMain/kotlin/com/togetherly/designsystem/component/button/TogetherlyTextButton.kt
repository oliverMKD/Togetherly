package com.togetherly.designsystem.component.button

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySize
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.ds_component_loading

/**
 * Togetherly's lowest-emphasis action — a dismissal, a "Learn more", a tertiary link — rendered as
 * accent-colored text with no container. Loading/disabled behavior matches
 * [com.togetherly.designsystem.component.button.TogetherlyPrimaryButton]; see its KDoc.
 */
@Composable
fun TogetherlyTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.togetherlyColors
    val loadingLabel = stringResource(Res.string.ds_component_loading)
    val baseColors = ButtonDefaults.textButtonColors(contentColor = colors.actionPrimary)

    TextButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .heightIn(min = MaterialTheme.togetherlySize.minimumTouchTarget)
            .semantics { if (loading) stateDescription = loadingLabel },
        colors = if (loading) {
            baseColors.copy(disabledContentColor = colors.actionPrimary)
        } else {
            baseColors
        },
    ) {
        TogetherlyButtonContent(label = label, loading = loading, leadingIcon = leadingIcon)
    }
}
