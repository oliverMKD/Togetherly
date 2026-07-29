package com.togetherly.designsystem.component.button

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyShapes
import com.togetherly.designsystem.theme.togetherlySize
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.ds_component_loading

/**
 * Togetherly's highest-emphasis call to action — at most one per screen. Uses
 * [togetherlyColors]' `actionPrimary`/`actionPrimaryContent` pair directly rather than Material's
 * own `primary` slot, so this button's color always tracks Togetherly's semantic role rather than
 * whatever [com.togetherly.designsystem.color.toMaterialColorScheme] happens to map `primary` to.
 *
 * While [loading], the button becomes non-interactive — Material's own `enabled` mechanics block
 * repeated clicks structurally, not an ad-hoc debounce — but keeps its normal (non-dimmed) colors
 * and its label, with only a small spinner joining the leading-icon slot, so it reads as "busy",
 * never as broken or permanently disabled. A screen reader hears "Loading" via [stateDescription]
 * in addition to Compose's own disabled announcement.
 */
@Composable
fun TogetherlyPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.togetherlyColors
    val loadingLabel = stringResource(Res.string.ds_component_loading)
    val baseColors = ButtonDefaults.buttonColors(
        containerColor = colors.actionPrimary,
        contentColor = colors.actionPrimaryContent,
    )

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .heightIn(min = MaterialTheme.togetherlySize.buttonHeight)
            .semantics { if (loading) stateDescription = loadingLabel },
        shape = MaterialTheme.togetherlyShapes.pill,
        colors = if (loading) {
            baseColors.copy(
                disabledContainerColor = colors.actionPrimary,
                disabledContentColor = colors.actionPrimaryContent,
            )
        } else {
            baseColors
        },
    ) {
        TogetherlyButtonContent(label = label, loading = loading, leadingIcon = leadingIcon)
    }
}
