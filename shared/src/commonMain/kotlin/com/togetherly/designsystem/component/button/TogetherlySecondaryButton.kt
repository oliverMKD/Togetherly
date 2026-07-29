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
 * A lower-emphasis alternative to [TogetherlyPrimaryButton] for a screen's secondary action (e.g.
 * "Skip" next to a primary "Continue") — uses [togetherlyColors]' `actionSecondary`/
 * `actionSecondaryContent` pair rather than a lighter/outlined take on the primary color, since
 * that pair is what the token set defines for this exact role.
 *
 * Loading/disabled behavior is identical to [TogetherlyPrimaryButton] — see its KDoc.
 */
@Composable
fun TogetherlySecondaryButton(
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
        containerColor = colors.actionSecondary,
        contentColor = colors.actionSecondaryContent,
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
                disabledContainerColor = colors.actionSecondary,
                disabledContentColor = colors.actionSecondaryContent,
            )
        } else {
            baseColors
        },
    ) {
        TogetherlyButtonContent(label = label, loading = loading, leadingIcon = leadingIcon)
    }
}
